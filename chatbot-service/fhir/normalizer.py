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
        "active": resource.get("active"),
        "identifier": _identifiers(resource.get("identifier", [])),
        "names": [_name(item) for item in resource.get("name", []) if isinstance(item, dict)],
        "name": _human_name(name),
        "gender": resource.get("gender"),
        "birth_date": resource.get("birthDate"),
        "telecom": _telecom(resource.get("telecom", [])),
        "phone": _telecom_value(resource.get("telecom", []), "phone"),
        "email": _telecom_value(resource.get("telecom", []), "email"),
        "address": _addresses(resource.get("address", [])),
        "marital_status": _codeable_concept(resource.get("maritalStatus", {})),
        "contact": _contacts(resource.get("contact", [])),
        "general_practitioner": _references(resource.get("generalPractitioner", [])),
        "managing_organization": _reference(resource.get("managingOrganization", {})),
    }


def normalize_patient_bundle(bundle: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        normalize_patient(entry["resource"])
        for entry in bundle.get("entry", [])
        if entry.get("resource", {}).get("resourceType") == "Patient"
    ]


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
        "category": [_codeable_concept(item) for item in resource.get("category", []) if isinstance(item, dict)],
        "code": _code_text(resource.get("code", {})),
        "code_detail": _codeable_concept(resource.get("code", {})),
        "effective_time": resource.get("effectiveDateTime") or resource.get("effectivePeriod"),
        "issued": resource.get("issued"),
        "subject": resource.get("subject", {}).get("reference"),
        "subject_detail": _reference(resource.get("subject", {})),
        "encounter": resource.get("encounter", {}).get("reference"),
        "encounter_detail": _reference(resource.get("encounter", {})),
        "performer": _references(resource.get("performer", [])),
        "value": _quantity(resource.get("valueQuantity")),
        "value_codeable_concept": _codeable_concept(resource.get("valueCodeableConcept", {})),
        "value_string": resource.get("valueString"),
        "value_boolean": resource.get("valueBoolean"),
        "value_integer": resource.get("valueInteger"),
        "interpretation": [_codeable_concept(item) for item in resource.get("interpretation", []) if isinstance(item, dict)],
        "body_site": _codeable_concept(resource.get("bodySite", {})),
        "method": _codeable_concept(resource.get("method", {})),
        "reference_range": [_reference_range(item) for item in resource.get("referenceRange", []) if isinstance(item, dict)],
        "note": _notes(resource.get("note", [])),
        "components": [],
    }
    components = []
    for component in resource.get("component", []):
        components.append(
            {
                "code": _code_text(component.get("code", {})),
                "code_detail": _codeable_concept(component.get("code", {})),
                "value": _quantity(component.get("valueQuantity")),
                "value_codeable_concept": _codeable_concept(component.get("valueCodeableConcept", {})),
                "value_string": component.get("valueString"),
                "interpretation": [
                    _codeable_concept(item)
                    for item in component.get("interpretation", [])
                    if isinstance(item, dict)
                ],
                "reference_range": [
                    _reference_range(item)
                    for item in component.get("referenceRange", [])
                    if isinstance(item, dict)
                ],
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
        "clinical_status_detail": _codeable_concept(resource.get("clinicalStatus", {})),
        "verification_status": _code_text(resource.get("verificationStatus", {})),
        "verification_status_detail": _codeable_concept(resource.get("verificationStatus", {})),
        "category": [_codeable_concept(item) for item in resource.get("category", []) if isinstance(item, dict)],
        "severity": _codeable_concept(resource.get("severity", {})),
        "code": _code_text(resource.get("code", {})),
        "code_detail": _codeable_concept(resource.get("code", {})),
        "subject": resource.get("subject", {}).get("reference"),
        "subject_detail": _reference(resource.get("subject", {})),
        "encounter": resource.get("encounter", {}).get("reference"),
        "encounter_detail": _reference(resource.get("encounter", {})),
        "onset": resource.get("onsetDateTime") or resource.get("onsetAge") or resource.get("onsetPeriod"),
        "abatement": resource.get("abatementDateTime") or resource.get("abatementAge") or resource.get("abatementPeriod"),
        "recorded_date": resource.get("recordedDate"),
        "recorder": _reference(resource.get("recorder", {})),
        "asserter": _reference(resource.get("asserter", {})),
        "note": _notes(resource.get("note", [])),
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
        "category": [_codeable_concept(item) for item in resource.get("category", []) if isinstance(item, dict)],
        "priority": resource.get("priority"),
        "medication": _code_text(medication),
        "medication_detail": _codeable_concept(medication),
        "medication_reference": _reference(resource.get("medicationReference", {})),
        "subject": resource.get("subject", {}).get("reference"),
        "subject_detail": _reference(resource.get("subject", {})),
        "encounter": resource.get("encounter", {}).get("reference"),
        "encounter_detail": _reference(resource.get("encounter", {})),
        "authored_on": resource.get("authoredOn"),
        "requester": _reference(resource.get("requester", {})),
        "reason_code": [_codeable_concept(item) for item in resource.get("reasonCode", []) if isinstance(item, dict)],
        "reason_reference": _references(resource.get("reasonReference", [])),
        "note": _notes(resource.get("note", [])),
        "dispense_request": _dispense_request(resource.get("dispenseRequest", {})),
        "dosage_instruction": [
            _dosage_instruction(item)
            for item in resource.get("dosageInstruction", [])
            if isinstance(item, dict)
        ],
        "dosage": [
            item.get("text")
            for item in resource.get("dosageInstruction", [])
            if item.get("text")
        ],
    }


def normalize_encounter_bundle(bundle: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        normalize_encounter(entry["resource"])
        for entry in bundle.get("entry", [])
        if entry.get("resource", {}).get("resourceType") == "Encounter"
    ]


def normalize_encounter(resource: dict[str, Any]) -> dict[str, Any]:
    return {
        "resource_type": resource.get("resourceType"),
        "id": resource.get("id"),
        "status": resource.get("status"),
        "class": _coding(resource.get("class", {})),
        "type": [_codeable_concept(item) for item in resource.get("type", []) if isinstance(item, dict)],
        "service_type": _codeable_concept(resource.get("serviceType", {})),
        "subject": resource.get("subject", {}).get("reference"),
        "subject_detail": _reference(resource.get("subject", {})),
        "participant": [_participant(item) for item in resource.get("participant", []) if isinstance(item, dict)],
        "period": resource.get("period"),
        "reason_code": [_codeable_concept(item) for item in resource.get("reasonCode", []) if isinstance(item, dict)],
        "diagnosis": [_encounter_diagnosis(item) for item in resource.get("diagnosis", []) if isinstance(item, dict)],
        "location": [_encounter_location(item) for item in resource.get("location", []) if isinstance(item, dict)],
        "service_provider": _reference(resource.get("serviceProvider", {})),
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


def _name(name: dict[str, Any]) -> dict[str, Any]:
    return {
        "use": name.get("use"),
        "text": _human_name(name),
        "family": name.get("family"),
        "given": name.get("given", []),
        "prefix": name.get("prefix", []),
        "suffix": name.get("suffix", []),
    }


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


def _telecom(telecom: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {
            "system": item.get("system"),
            "value": item.get("value"),
            "use": item.get("use"),
            "rank": item.get("rank"),
        }
        for item in telecom
        if isinstance(item, dict)
    ]


def _addresses(addresses: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {
            "use": item.get("use"),
            "type": item.get("type"),
            "text": item.get("text"),
            "line": item.get("line", []),
            "city": item.get("city"),
            "district": item.get("district"),
            "state": item.get("state"),
            "postal_code": item.get("postalCode"),
            "country": item.get("country"),
        }
        for item in addresses
        if isinstance(item, dict)
    ]


def _contacts(contacts: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {
            "relationship": [
                _codeable_concept(item)
                for item in contact.get("relationship", [])
                if isinstance(item, dict)
            ],
            "name": _name(contact.get("name", {})),
            "telecom": _telecom(contact.get("telecom", [])),
            "address": _addresses([contact.get("address", {})]) if contact.get("address") else [],
            "gender": contact.get("gender"),
        }
        for contact in contacts
        if isinstance(contact, dict)
    ]


def _code_text(codeable: dict[str, Any]) -> str | None:
    if codeable.get("text"):
        return codeable["text"]
    coding = _first(codeable.get("coding"))
    return coding.get("display") or coding.get("code")


def _codeable_concept(codeable: dict[str, Any] | None) -> dict[str, Any] | None:
    if not codeable:
        return None
    codings = [_coding(item) for item in codeable.get("coding", []) if isinstance(item, dict)]
    return {
        "text": codeable.get("text") or _first_display(codings),
        "coding": codings,
    }


def _coding(coding: dict[str, Any] | None) -> dict[str, Any] | None:
    if not coding:
        return None
    return {
        "system": coding.get("system"),
        "code": coding.get("code"),
        "display": coding.get("display"),
    }


def _first_display(codings: list[dict[str, Any] | None]) -> str | None:
    for coding in codings:
        if not coding:
            continue
        if coding.get("display"):
            return coding["display"]
        if coding.get("code"):
            return coding["code"]
    return None


def _quantity(quantity: dict[str, Any] | None) -> dict[str, Any] | None:
    if not quantity:
        return None
    return {
        "value": quantity.get("value"),
        "unit": quantity.get("unit") or quantity.get("code"),
        "system": quantity.get("system"),
        "code": quantity.get("code"),
        "comparator": quantity.get("comparator"),
    }


def _reference(reference: dict[str, Any] | None) -> dict[str, Any] | None:
    if not reference:
        return None
    return {
        "reference": reference.get("reference"),
        "type": reference.get("type"),
        "identifier": _identifier(reference.get("identifier", {})),
        "display": reference.get("display"),
    }


def _references(references: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        item
        for item in (_reference(reference) for reference in references if isinstance(reference, dict))
        if item
    ]


def _identifier(identifier: dict[str, Any] | None) -> dict[str, Any] | None:
    if not identifier:
        return None
    return {
        "system": identifier.get("system"),
        "value": identifier.get("value"),
    }


def _reference_range(item: dict[str, Any]) -> dict[str, Any]:
    return {
        "low": _quantity(item.get("low")),
        "high": _quantity(item.get("high")),
        "type": _codeable_concept(item.get("type", {})),
        "applies_to": [
            _codeable_concept(applies_to)
            for applies_to in item.get("appliesTo", [])
            if isinstance(applies_to, dict)
        ],
        "age": item.get("age"),
        "text": item.get("text"),
    }


def _notes(notes: list[dict[str, Any]]) -> list[str]:
    return [
        item.get("text")
        for item in notes
        if isinstance(item, dict) and item.get("text")
    ]


def _dosage_instruction(item: dict[str, Any]) -> dict[str, Any]:
    return {
        "sequence": item.get("sequence"),
        "text": item.get("text"),
        "patient_instruction": item.get("patientInstruction"),
        "timing": item.get("timing"),
        "route": _codeable_concept(item.get("route", {})),
        "method": _codeable_concept(item.get("method", {})),
        "dose_and_rate": [
            {
                "type": _codeable_concept(dose.get("type", {})),
                "dose_quantity": _quantity(dose.get("doseQuantity")),
                "rate_quantity": _quantity(dose.get("rateQuantity")),
            }
            for dose in item.get("doseAndRate", [])
            if isinstance(dose, dict)
        ],
        "max_dose_per_period": item.get("maxDosePerPeriod"),
    }


def _dispense_request(item: dict[str, Any] | None) -> dict[str, Any] | None:
    if not item:
        return None
    return {
        "validity_period": item.get("validityPeriod"),
        "number_of_repeats_allowed": item.get("numberOfRepeatsAllowed"),
        "quantity": _quantity(item.get("quantity")),
        "expected_supply_duration": _quantity(item.get("expectedSupplyDuration")),
    }


def _participant(item: dict[str, Any]) -> dict[str, Any]:
    return {
        "type": [_codeable_concept(value) for value in item.get("type", []) if isinstance(value, dict)],
        "period": item.get("period"),
        "individual": _reference(item.get("individual", {})),
    }


def _encounter_diagnosis(item: dict[str, Any]) -> dict[str, Any]:
    return {
        "condition": _reference(item.get("condition", {})),
        "use": _codeable_concept(item.get("use", {})),
        "rank": item.get("rank"),
    }


def _encounter_location(item: dict[str, Any]) -> dict[str, Any]:
    return {
        "location": _reference(item.get("location", {})),
        "status": item.get("status"),
        "physical_type": _codeable_concept(item.get("physicalType", {})),
        "period": item.get("period"),
    }
