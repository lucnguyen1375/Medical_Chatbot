from typing import Any


def normalize_capability_statement(resource: dict[str, Any]) -> dict[str, Any]:
    return {
        "resource_type": resource.get("resourceType"),
        "fhir_version": resource.get("fhirVersion"),
        "software": resource.get("software", {}).get("name"),
        "status": resource.get("status"),
        "date": resource.get("date"),
    }


def normalize_patient(resource: dict[str, Any]) -> dict[str, Any]:
    name = _first(resource.get("name"))
    return {
        "resource_type": resource.get("resourceType"),
        "id": resource.get("id"),
        "identifier": _identifiers(resource.get("identifier", [])),
        "name": _human_name(name),
        "gender": resource.get("gender"),
        "birth_date": resource.get("birthDate"),
        "phone": _telecom_value(resource.get("telecom", []), "phone"),
    }


def normalize_observation_bundle(bundle: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        normalize_observation(entry["resource"])
        for entry in bundle.get("entry", [])
        if entry.get("resource", {}).get("resourceType") == "Observation"
    ]


def normalize_observation(resource: dict[str, Any]) -> dict[str, Any]:
    normalized = {
        "resource_type": resource.get("resourceType"),
        "id": resource.get("id"),
        "status": resource.get("status"),
        "code": _code_text(resource.get("code", {})),
        "effective_time": resource.get("effectiveDateTime") or resource.get("effectivePeriod"),
        "subject": resource.get("subject", {}).get("reference"),
        "value": _quantity(resource.get("valueQuantity")),
        "components": [],
    }
    components = []
    for component in resource.get("component", []):
        components.append(
            {
                "code": _code_text(component.get("code", {})),
                "value": _quantity(component.get("valueQuantity")),
            }
        )
    normalized["components"] = components
    return normalized


def normalize_condition_bundle(bundle: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        normalize_condition(entry["resource"])
        for entry in bundle.get("entry", [])
        if entry.get("resource", {}).get("resourceType") == "Condition"
    ]


def normalize_condition(resource: dict[str, Any]) -> dict[str, Any]:
    return {
        "resource_type": resource.get("resourceType"),
        "id": resource.get("id"),
        "clinical_status": _code_text(resource.get("clinicalStatus", {})),
        "verification_status": _code_text(resource.get("verificationStatus", {})),
        "code": _code_text(resource.get("code", {})),
        "subject": resource.get("subject", {}).get("reference"),
        "recorded_date": resource.get("recordedDate"),
    }


def normalize_medication_request_bundle(bundle: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        normalize_medication_request(entry["resource"])
        for entry in bundle.get("entry", [])
        if entry.get("resource", {}).get("resourceType") == "MedicationRequest"
    ]


def normalize_medication_request(resource: dict[str, Any]) -> dict[str, Any]:
    medication = resource.get("medicationCodeableConcept") or {}
    return {
        "resource_type": resource.get("resourceType"),
        "id": resource.get("id"),
        "status": resource.get("status"),
        "intent": resource.get("intent"),
        "medication": _code_text(medication),
        "subject": resource.get("subject", {}).get("reference"),
        "authored_on": resource.get("authoredOn"),
        "dosage": [
            item.get("text")
            for item in resource.get("dosageInstruction", [])
            if item.get("text")
        ],
    }


def _first(items: Any) -> dict[str, Any]:
    if isinstance(items, list) and items:
        item = items[0]
        if isinstance(item, dict):
            return item
    return {}


def _human_name(name: dict[str, Any]) -> str | None:
    given = name.get("given") or []
    family = name.get("family")
    parts = [*given, family]
    text = " ".join(part for part in parts if part)
    return text or name.get("text")


def _identifiers(identifiers: list[dict[str, Any]]) -> list[dict[str, str | None]]:
    return [
        {
            "system": item.get("system"),
            "value": item.get("value"),
        }
        for item in identifiers
        if isinstance(item, dict)
    ]


def _telecom_value(telecom: list[dict[str, Any]], system: str) -> str | None:
    for item in telecom:
        if item.get("system") == system:
            return item.get("value")
    return None


def _code_text(codeable: dict[str, Any]) -> str | None:
    if codeable.get("text"):
        return codeable["text"]
    coding = _first(codeable.get("coding"))
    return coding.get("display") or coding.get("code")


def _quantity(quantity: dict[str, Any] | None) -> dict[str, Any] | None:
    if not quantity:
        return None
    return {
        "value": quantity.get("value"),
        "unit": quantity.get("unit") or quantity.get("code"),
        "system": quantity.get("system"),
        "code": quantity.get("code"),
    }
