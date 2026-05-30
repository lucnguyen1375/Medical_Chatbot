import re
import unicodedata
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from fhir.client import FhirClient, FhirClientError, get_fhir_client
from fhir.normalizer import (
    normalize_condition_bundle,
    normalize_medication_request_bundle,
    normalize_observation_bundle,
    normalize_patient,
)


router = APIRouter(tags=["chat"])

DEFAULT_PATIENT_ID = "demo-patient-001"


class ChatRequest(BaseModel):
    user_id: str = Field(default="demo_user")
    session_id: str | None = None
    message: str = Field(min_length=1)
    patient_id: str | None = None


@router.post("/chat")
async def chat(request: ChatRequest, client: FhirClient = Depends(get_fhir_client)) -> dict[str, Any]:
    patient_id = _resolve_patient_id(request)
    intent = _detect_intent(request.message)

    try:
        if intent == "medications":
            return await _answer_medications(client, patient_id)
        if intent == "observations":
            return await _answer_observations(client, patient_id)
        if intent == "conditions":
            return await _answer_conditions(client, patient_id)
        if intent == "patient":
            return await _answer_patient(client, patient_id)
    except FhirClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=exc.user_message,
        ) from exc

    return {
        "answer": (
            "I could not determine which FHIR data to retrieve. Ask about patient information, "
            "observations, conditions, or medications, and include a patient id when available."
        ),
        "intent": "unknown",
        "patient_id": patient_id,
        "evidence": [],
        "usage": _zero_usage(),
    }


async def _answer_patient(client: FhirClient, patient_id: str) -> dict[str, Any]:
    patient = normalize_patient(await client.get_patient(patient_id))
    answer = (
        f"According to the available FHIR data, Patient/{patient_id} is "
        f"{patient.get('name') or 'unnamed'}, gender {patient.get('gender') or 'unknown'}, "
        f"birth date {patient.get('birth_date') or 'unknown'}."
    )
    return {
        "answer": answer,
        "intent": "patient",
        "patient_id": patient_id,
        "evidence": [_evidence("Patient", patient.get("id"), patient.get("name"))],
        "usage": _zero_usage(),
    }


async def _answer_observations(client: FhirClient, patient_id: str) -> dict[str, Any]:
    bundle = await client.search_patient_resources("Observation", patient_id, count=5, sort="-date")
    observations = normalize_observation_bundle(bundle)
    if not observations:
        answer = f"No Observation records were found for Patient/{patient_id}."
    else:
        summary = "; ".join(_format_observation(item) for item in observations)
        answer = (
            f"According to the available FHIR data, Patient/{patient_id} has "
            f"{len(observations)} recent observation record(s): {summary}."
        )
    return {
        "answer": answer,
        "intent": "observations",
        "patient_id": patient_id,
        "evidence": [_evidence("Observation", item.get("id"), item.get("code")) for item in observations],
        "usage": _zero_usage(),
    }


async def _answer_conditions(client: FhirClient, patient_id: str) -> dict[str, Any]:
    bundle = await client.search_patient_resources("Condition", patient_id, count=20)
    conditions = normalize_condition_bundle(bundle)
    if not conditions:
        answer = f"No Condition records were found for Patient/{patient_id}."
    else:
        condition_names = ", ".join(item.get("code") or item.get("id") for item in conditions)
        answer = (
            f"According to the available FHIR data, Patient/{patient_id} has "
            f"{len(conditions)} condition record(s): {condition_names}."
        )
    return {
        "answer": answer,
        "intent": "conditions",
        "patient_id": patient_id,
        "evidence": [_evidence("Condition", item.get("id"), item.get("code")) for item in conditions],
        "usage": _zero_usage(),
    }


async def _answer_medications(client: FhirClient, patient_id: str) -> dict[str, Any]:
    bundle = await client.search_patient_resources("MedicationRequest", patient_id, count=20)
    medications = normalize_medication_request_bundle(bundle)
    if not medications:
        answer = f"No MedicationRequest records were found for Patient/{patient_id}."
    else:
        medication_names = ", ".join(item.get("medication") or item.get("id") for item in medications)
        answer = (
            f"According to the available FHIR data, Patient/{patient_id} has "
            f"{len(medications)} medication request record(s): {medication_names}."
        )
    return {
        "answer": answer,
        "intent": "medications",
        "patient_id": patient_id,
        "evidence": [_evidence("MedicationRequest", item.get("id"), item.get("medication")) for item in medications],
        "usage": _zero_usage(),
    }


def _detect_intent(message: str) -> str:
    text = _normalize_text(message)
    if _contains_any(text, ["medication", "medicine", "drug", "thuoc"]):
        return "medications"
    if _contains_any(text, ["observation", "glucose", "blood pressure", "lab", "huyet ap", "duong huyet"]):
        return "observations"
    if _contains_any(text, ["condition", "diagnosis", "diagnose", "chan doan", "benh"]):
        return "conditions"
    if _contains_any(text, ["patient", "information", "info", "thong tin"]):
        return "patient"
    return "unknown"


def _resolve_patient_id(request: ChatRequest) -> str:
    if request.patient_id:
        return request.patient_id.removeprefix("Patient/").strip()

    patient_ref = re.search(r"Patient/([A-Za-z0-9.-]+)", request.message, flags=re.IGNORECASE)
    if patient_ref:
        return patient_ref.group(1)

    demo_id = re.search(r"\bdemo-patient-[A-Za-z0-9.-]+\b", request.message, flags=re.IGNORECASE)
    if demo_id:
        return demo_id.group(0)

    return DEFAULT_PATIENT_ID


def _format_observation(observation: dict[str, Any]) -> str:
    code = observation.get("code") or observation.get("id")
    value = observation.get("value")
    if value:
        return f"{code} {value.get('value')} {value.get('unit') or ''}".strip()
    components = observation.get("components") or []
    component_text = ", ".join(
        f"{item.get('code')} {item.get('value', {}).get('value')} {item.get('value', {}).get('unit') or ''}".strip()
        for item in components
    )
    return f"{code}: {component_text}" if component_text else str(code)


def _evidence(resource_type: str, resource_id: Any, summary: Any) -> dict[str, Any]:
    return {
        "resource_type": resource_type,
        "id": resource_id,
        "summary": summary,
    }


def _zero_usage() -> dict[str, int | float]:
    return {
        "input_tokens": 0,
        "output_tokens": 0,
        "estimated_cost_usd": 0,
    }


def _normalize_text(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text)
    without_accents = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    return without_accents.lower()


def _contains_any(text: str, keywords: list[str]) -> bool:
    return any(keyword in text for keyword in keywords)
