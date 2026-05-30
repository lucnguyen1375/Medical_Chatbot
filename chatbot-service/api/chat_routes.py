from typing import Any

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from agents.intent_extractor import (
    CONDITION_KEYWORDS,
    MEDICATION_KEYWORDS,
    OBSERVATION_KEYWORDS,
    PATIENT_CONTACT_KEYWORDS,
    PATIENT_INFO_KEYWORDS,
    PATIENT_LIST_KEYWORDS,
    TOOL_GET_CONDITIONS,
    TOOL_GET_MEDICATIONS,
    TOOL_GET_OBSERVATIONS,
    TOOL_GET_PATIENT,
    TOOL_SEARCH_PATIENTS,
    IntentExtractor,
    IntentPlan,
    contains_any,
    get_intent_extractor,
    normalize_text,
    resolve_patient_id_for_request,
)
from fhir.client import FhirClient, FhirClientError, get_fhir_client
from fhir.normalizer import (
    normalize_condition_bundle,
    normalize_medication_request_bundle,
    normalize_observation_bundle,
    normalize_patient,
    normalize_patient_bundle,
)


router = APIRouter(tags=["chat"])


class ChatRequest(BaseModel):
    user_id: str = Field(default="demo_user")
    session_id: str | None = None
    message: str = Field(min_length=1)
    patient_id: str | None = None


@router.post("/chat")
async def chat(
    request: ChatRequest,
    client: FhirClient = Depends(get_fhir_client),
    intent_extractor: IntentExtractor = Depends(get_intent_extractor),
) -> dict[str, Any]:
    plan = await intent_extractor.extract(
        request.message,
        provided_patient_id=request.patient_id,
    )

    try:
        if plan.tool_name == TOOL_SEARCH_PATIENTS:
            return _with_plan_metadata(await _answer_patients(client, plan.limit), plan)
        if plan.tool_name == TOOL_GET_MEDICATIONS:
            if plan.all_patients:
                return _with_plan_metadata(await _answer_all_patient_medications(client, plan.limit), plan)
            return _with_plan_metadata(await _answer_medications(client, plan.patient_id, plan.limit), plan)
        if plan.tool_name == TOOL_GET_OBSERVATIONS:
            if plan.all_patients:
                return _with_plan_metadata(
                    await _answer_all_patient_observations(client, plan.limit, plan.observation_type),
                    plan,
                )
            return _with_plan_metadata(
                await _answer_observations(client, plan.patient_id, plan.limit, plan.observation_type),
                plan,
            )
        if plan.tool_name == TOOL_GET_CONDITIONS:
            if plan.all_patients:
                return _with_plan_metadata(await _answer_all_patient_conditions(client, plan.limit), plan)
            return _with_plan_metadata(await _answer_conditions(client, plan.patient_id, plan.limit), plan)
        if plan.tool_name == TOOL_GET_PATIENT:
            return _with_plan_metadata(await _answer_patient(client, plan.patient_id), plan)
    except FhirClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=exc.user_message,
        ) from exc

    return {
        "answer": (
            "Tôi chưa xác định được cần lấy loại dữ liệu FHIR nào. "
            "Bạn có thể hỏi về thông tin bệnh nhân, chỉ số/xét nghiệm, chẩn đoán hoặc thuốc, "
            "và nên kèm mã bệnh nhân nếu có."
        ),
        "intent": "unknown",
        "patient_id": plan.patient_id,
        "evidence": [],
        "usage": plan.usage,
        "tool_name": plan.tool_name,
        "intent_source": plan.source,
        "intent_reason": plan.reason,
    }


async def _answer_patients(client: FhirClient, limit: int) -> dict[str, Any]:
    bundle = await client.search_patients(count=limit)
    patients = normalize_patient_bundle(bundle)
    if not patients:
        answer = "Không tìm thấy bệnh nhân nào trong FHIR Server."
    else:
        summary = "; ".join(_format_patient_summary(patient) for patient in patients)
        answer = (
            f"Theo dữ liệu FHIR hiện có, hệ thống tìm thấy {len(patients)} bệnh nhân: "
            f"{summary}."
        )
    return {
        "answer": answer,
        "intent": "patients",
        "patient_id": None,
        "evidence": [
            _evidence("Patient", patient.get("id"), _format_patient_summary(patient), patient)
            for patient in patients
        ],
        "usage": _zero_usage(),
    }


async def _answer_patient(client: FhirClient, patient_id: str) -> dict[str, Any]:
    patient = normalize_patient(await client.get_patient(patient_id))
    phone = _value_or_unknown(patient.get("phone"))
    name = _value_or_unknown(patient.get("name"))
    gender = _gender_vi(patient.get("gender"))
    birth_date = _value_or_unknown(patient.get("birth_date"))
    answer = (
        f"Theo dữ liệu FHIR hiện có, Bệnh nhân Patient/{patient_id} có họ tên {name}, "
        f"giới tính {gender}, ngày sinh {birth_date}, số điện thoại {phone}."
    )
    summary = (
        f"Họ tên: {name}; "
        f"giới tính: {gender}; "
        f"ngày sinh: {birth_date}; "
        f"số điện thoại: {phone}"
    )
    return {
        "answer": answer,
        "intent": "patient",
        "patient_id": patient_id,
        "evidence": [_evidence("Patient", patient.get("id"), summary, patient)],
        "usage": _zero_usage(),
    }


async def _answer_all_patient_observations(
    client: FhirClient,
    limit: int,
    observation_type: str | None = None,
) -> dict[str, Any]:
    patients = await _get_patients(client, limit=20)
    summaries = []
    evidence = []
    for patient in patients:
        patient_id = patient.get("id")
        if not patient_id:
            continue
        search_count = max(limit, 20) if observation_type else limit
        bundle = await client.search_patient_resources(
            "Observation",
            patient_id,
            count=search_count,
            sort="-date",
        )
        observations = normalize_observation_bundle(bundle)
        if observation_type:
            observations = [
                item for item in observations
                if _observation_matches_type(item, observation_type)
            ][:limit]
        patient_label = _format_patient_identity(patient)
        if observations:
            observation_summary = "; ".join(_format_observation(item) for item in observations)
            summaries.append(f"{patient_label}: {observation_summary}")
            evidence.extend(
                _evidence(
                    "Observation",
                    item.get("id"),
                    f"{patient_label}: {_display_vi(item.get('code'))}",
                    _with_patient_context(item, patient),
                )
                for item in observations
            )
        else:
            summaries.append(f"{patient_label}: không có bản ghi phù hợp")

    answer = _format_all_patient_answer(
        summaries,
        empty_message="Không tìm thấy bệnh nhân nào để kiểm tra chỉ số/xét nghiệm.",
        prefix="Theo dữ liệu FHIR hiện có, chỉ số/xét nghiệm của các bệnh nhân là",
    )
    return {
        "answer": answer,
        "intent": "observations",
        "patient_id": None,
        "evidence": evidence,
        "usage": _zero_usage(),
    }


async def _answer_observations(
    client: FhirClient,
    patient_id: str,
    limit: int,
    observation_type: str | None = None,
) -> dict[str, Any]:
    search_count = max(limit, 20) if observation_type else limit
    bundle = await client.search_patient_resources("Observation", patient_id, count=search_count, sort="-date")
    observations = normalize_observation_bundle(bundle)
    if observation_type:
        observations = [
            item for item in observations
            if _observation_matches_type(item, observation_type)
        ][:limit]

    if not observations:
        if observation_type:
            answer = (
                f"Không tìm thấy bản ghi {_display_vi(observation_type)} phù hợp "
                f"cho Bệnh nhân Patient/{patient_id}."
            )
        else:
            answer = f"Không tìm thấy bản ghi chỉ số/xét nghiệm nào cho Bệnh nhân Patient/{patient_id}."
    else:
        summary = "; ".join(_format_observation(item) for item in observations)
        answer = (
            f"Theo dữ liệu FHIR hiện có, Bệnh nhân Patient/{patient_id} có "
            f"{len(observations)} bản ghi chỉ số/xét nghiệm gần đây: {summary}."
        )
    return {
        "answer": answer,
        "intent": "observations",
        "patient_id": patient_id,
        "evidence": [
            _evidence("Observation", item.get("id"), _display_vi(item.get("code")), item)
            for item in observations
        ],
        "usage": _zero_usage(),
    }


async def _answer_all_patient_conditions(client: FhirClient, limit: int) -> dict[str, Any]:
    patients = await _get_patients(client, limit=20)
    summaries = []
    evidence = []
    for patient in patients:
        patient_id = patient.get("id")
        if not patient_id:
            continue
        bundle = await client.search_patient_resources("Condition", patient_id, count=limit)
        conditions = normalize_condition_bundle(bundle)
        patient_label = _format_patient_identity(patient)
        if conditions:
            condition_names = ", ".join(_display_vi(item.get("code") or item.get("id")) for item in conditions)
            summaries.append(f"{patient_label}: {condition_names}")
            evidence.extend(
                _evidence(
                    "Condition",
                    item.get("id"),
                    f"{patient_label}: {_display_vi(item.get('code'))}",
                    _with_patient_context(item, patient),
                )
                for item in conditions
            )
        else:
            summaries.append(f"{patient_label}: không có bản ghi chẩn đoán")

    answer = _format_all_patient_answer(
        summaries,
        empty_message="Không tìm thấy bệnh nhân nào để kiểm tra chẩn đoán.",
        prefix="Theo dữ liệu FHIR hiện có, chẩn đoán/tình trạng bệnh của các bệnh nhân là",
    )
    return {
        "answer": answer,
        "intent": "conditions",
        "patient_id": None,
        "evidence": evidence,
        "usage": _zero_usage(),
    }


async def _answer_conditions(client: FhirClient, patient_id: str, limit: int) -> dict[str, Any]:
    bundle = await client.search_patient_resources("Condition", patient_id, count=limit)
    conditions = normalize_condition_bundle(bundle)
    if not conditions:
        answer = f"Không tìm thấy bản ghi chẩn đoán/tình trạng bệnh nào cho Bệnh nhân Patient/{patient_id}."
    else:
        condition_names = ", ".join(_display_vi(item.get("code") or item.get("id")) for item in conditions)
        answer = (
            f"Theo dữ liệu FHIR hiện có, Bệnh nhân Patient/{patient_id} có "
            f"{len(conditions)} bản ghi chẩn đoán/tình trạng bệnh: {condition_names}."
        )
    return {
        "answer": answer,
        "intent": "conditions",
        "patient_id": patient_id,
        "evidence": [
            _evidence("Condition", item.get("id"), _display_vi(item.get("code")), item)
            for item in conditions
        ],
        "usage": _zero_usage(),
    }


async def _answer_all_patient_medications(client: FhirClient, limit: int) -> dict[str, Any]:
    patients = await _get_patients(client, limit=20)
    summaries = []
    evidence = []
    for patient in patients:
        patient_id = patient.get("id")
        if not patient_id:
            continue
        bundle = await client.search_patient_resources("MedicationRequest", patient_id, count=limit)
        medications = normalize_medication_request_bundle(bundle)
        patient_label = _format_patient_identity(patient)
        if medications:
            medication_names = ", ".join(_display_vi(item.get("medication") or item.get("id")) for item in medications)
            summaries.append(f"{patient_label}: {medication_names}")
            evidence.extend(
                _evidence(
                    "MedicationRequest",
                    item.get("id"),
                    f"{patient_label}: {_display_vi(item.get('medication'))}",
                    _with_patient_context(item, patient),
                )
                for item in medications
            )
        else:
            summaries.append(f"{patient_label}: không có bản ghi thuốc")

    answer = _format_all_patient_answer(
        summaries,
        empty_message="Không tìm thấy bệnh nhân nào để kiểm tra thuốc.",
        prefix="Theo dữ liệu FHIR hiện có, thuốc của các bệnh nhân là",
    )
    return {
        "answer": answer,
        "intent": "medications",
        "patient_id": None,
        "evidence": evidence,
        "usage": _zero_usage(),
    }


async def _answer_medications(client: FhirClient, patient_id: str, limit: int) -> dict[str, Any]:
    bundle = await client.search_patient_resources("MedicationRequest", patient_id, count=limit)
    medications = normalize_medication_request_bundle(bundle)
    if not medications:
        answer = f"Không tìm thấy bản ghi thuốc nào cho Bệnh nhân Patient/{patient_id}."
    else:
        medication_names = ", ".join(_display_vi(item.get("medication") or item.get("id")) for item in medications)
        answer = (
            f"Theo dữ liệu FHIR hiện có, Bệnh nhân Patient/{patient_id} có "
            f"{len(medications)} y lệnh thuốc: {medication_names}."
        )
    return {
        "answer": answer,
        "intent": "medications",
        "patient_id": patient_id,
        "evidence": [
            _evidence("MedicationRequest", item.get("id"), _display_vi(item.get("medication")), item)
            for item in medications
        ],
        "usage": _zero_usage(),
    }


def _detect_intent(message: str) -> str:
    text = normalize_text(message)
    if _contains_any(text, PATIENT_LIST_KEYWORDS):
        return "patients"
    if _contains_any(text, MEDICATION_KEYWORDS):
        return "medications"
    if _contains_any(text, OBSERVATION_KEYWORDS):
        return "observations"
    if _contains_any(text, PATIENT_CONTACT_KEYWORDS):
        return "patient"
    if _contains_any(text, CONDITION_KEYWORDS):
        return "conditions"
    if _contains_any(text, PATIENT_INFO_KEYWORDS):
        return "patient"
    return "unknown"


def _resolve_patient_id(request: ChatRequest) -> str:
    return resolve_patient_id_for_request(request.message, request.patient_id)


def _format_observation(observation: dict[str, Any]) -> str:
    code = _display_vi(observation.get("code") or observation.get("id"))
    value = observation.get("value")
    if value:
        return f"{code} {value.get('value')} {value.get('unit') or ''}".strip()
    components = observation.get("components") or []
    component_text = ", ".join(
        (
            f"{_display_vi(item.get('code'))} "
            f"{item.get('value', {}).get('value')} "
            f"{item.get('value', {}).get('unit') or ''}"
        ).strip()
        for item in components
    )
    return f"{code}: {component_text}" if component_text else str(code)


def _format_patient_summary(patient: dict[str, Any]) -> str:
    patient_id = patient.get("id") or "unknown"
    name = _value_or_unknown(patient.get("name"))
    gender = _gender_vi(patient.get("gender"))
    birth_date = _value_or_unknown(patient.get("birth_date"))
    phone = _value_or_unknown(patient.get("phone"))
    return f"Patient/{patient_id} - {name}, giới tính {gender}, ngày sinh {birth_date}, SĐT {phone}"


def _format_patient_identity(patient: dict[str, Any]) -> str:
    patient_id = patient.get("id") or "unknown"
    name = _value_or_unknown(patient.get("name"))
    return f"Patient/{patient_id} ({name})"


def _format_all_patient_answer(summaries: list[str], *, empty_message: str, prefix: str) -> str:
    if not summaries:
        return empty_message
    return f"{prefix}: " + "; ".join(summaries) + "."


async def _get_patients(client: FhirClient, limit: int) -> list[dict[str, Any]]:
    bundle = await client.search_patients(count=limit)
    return normalize_patient_bundle(bundle)


def _evidence(resource_type: str, resource_id: Any, summary: Any, data: Any | None = None) -> dict[str, Any]:
    return {
        "resource_type": resource_type,
        "id": resource_id,
        "summary": summary,
        "data": data,
    }


def _with_patient_context(resource: dict[str, Any], patient: dict[str, Any]) -> dict[str, Any]:
    return {
        **resource,
        "patient": {
            "id": patient.get("id"),
            "name": patient.get("name"),
            "gender": patient.get("gender"),
            "birth_date": patient.get("birth_date"),
            "phone": patient.get("phone"),
        },
    }


def _zero_usage() -> dict[str, int | float]:
    return {
        "input_tokens": 0,
        "output_tokens": 0,
        "estimated_cost_usd": 0,
    }


def _with_plan_metadata(payload: dict[str, Any], plan: IntentPlan) -> dict[str, Any]:
    payload["usage"] = plan.usage
    payload["tool_name"] = plan.tool_name
    payload["intent_source"] = plan.source
    if plan.all_patients:
        payload["all_patients"] = True
    if plan.observation_type:
        payload["observation_type"] = plan.observation_type
    return payload


def _contains_any(text: str, keywords: list[str]) -> bool:
    return contains_any(text, keywords)


def _value_or_unknown(value: Any) -> Any:
    return value if value else "không rõ"


def _gender_vi(gender: Any) -> str:
    if gender == "male":
        return "nam"
    if gender == "female":
        return "nữ"
    if gender == "other":
        return "khác"
    return "không rõ"


def _display_vi(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    translations = {
        "blood pressure": "Huyết áp",
        "blood_pressure": "Huyết áp",
        "huyet ap": "Huyết áp",
        "blood pressure panel with all children optional": "Huyết áp",
        "systolic blood pressure": "Huyết áp tâm thu",
        "diastolic blood pressure": "Huyết áp tâm trương",
        "blood glucose": "Đường huyết",
        "glucose [mass/volume] in blood": "Đường huyết",
        "heart rate": "Nhịp tim",
        "heart_rate": "Nhịp tim",
        "nhip tim": "Nhịp tim",
        "hemoglobin a1c/hemoglobin.total in blood": "HbA1c",
        "hba1c": "HbA1c",
        "cholesterol [mass/volume] in serum or plasma": "Cholesterol toàn phần",
        "total cholesterol": "Cholesterol toàn phần",
        "prediabetes": "Tiền đái tháo đường",
        "hypertension": "Tăng huyết áp",
        "essential (primary) hypertension": "Tăng huyết áp nguyên phát",
        "acute upper respiratory infection": "Nhiễm trùng đường hô hấp trên cấp",
        "acute upper respiratory infection, unspecified": "Nhiễm trùng đường hô hấp trên cấp",
        "type 2 diabetes mellitus": "Đái tháo đường type 2",
        "type 2 diabetes mellitus without complications": "Đái tháo đường type 2 không biến chứng",
        "hyperlipidemia": "Rối loạn lipid máu",
        "hyperlipidemia, unspecified": "Rối loạn lipid máu",
        "amlodipine 5 mg tablet": "Amlodipine 5 mg",
        "amlodipine 5 mg oral tablet": "Amlodipine 5 mg",
        "metformin 500 mg tablet": "Metformin 500 mg",
        "metformin 500 mg oral tablet": "Metformin 500 mg",
        "paracetamol 500 mg tablet": "Paracetamol 500 mg",
        "acetaminophen 500 mg oral tablet": "Paracetamol 500 mg",
        "atorvastatin 20 mg tablet": "Atorvastatin 20 mg",
        "atorvastatin 20 mg oral tablet": "Atorvastatin 20 mg",
        "losartan 50 mg tablet": "Losartan 50 mg",
        "losartan 50 mg oral tablet": "Losartan 50 mg",
    }
    return translations.get(value.lower(), value)


def _observation_matches_type(observation: dict[str, Any], observation_type: str) -> bool:
    wanted = normalize_text(observation_type)
    searchable_parts = [
        normalize_text(str(observation.get("code") or "")),
        *[
            normalize_text(str(component.get("code") or ""))
            for component in observation.get("components", [])
        ],
    ]
    searchable = " ".join(searchable_parts)

    if _contains_any(wanted, ["blood pressure", "blood_pressure", "huyet ap"]):
        return _contains_any(searchable, ["blood pressure", "systolic", "diastolic", "huyet ap"])
    if _contains_any(wanted, ["glucose", "duong huyet"]):
        return _contains_any(searchable, ["glucose", "duong huyet"])
    if _contains_any(wanted, ["heart rate", "heart_rate", "nhip tim"]):
        return _contains_any(searchable, ["heart rate", "nhip tim"])
    if _contains_any(wanted, ["cholesterol"]):
        return _contains_any(searchable, ["cholesterol"])
    if _contains_any(wanted, ["hba1c", "a1c"]):
        return _contains_any(searchable, ["hba1c", "a1c", "hemoglobin"])
    return wanted in searchable
