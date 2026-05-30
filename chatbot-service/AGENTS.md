# Chatbot Service Setup Notes

## What Was Built

This folder contains the first FastAPI chatbot/FHIR service slice for the Medical Chatbot project.

Implemented:

- FastAPI service entrypoint.
- Health endpoint.
- FHIR status endpoint.
- Patient, Observation, Condition, and MedicationRequest read endpoints.
- `POST /chat` endpoint for patient, observation, condition, and medication questions.
- OpenAI tool/function calling intent extraction when `OPENAI_API_KEY` is configured.
- Rule-based fallback intent extraction for local demos without an LLM API key.
- Central FHIR HTTP client using HAPI FHIR REST APIs.
- Normalizers that convert raw FHIR resources/Bundles into compact JSON for app and future LLM usage.
- Unit tests for normalizers, FHIR client behavior with mocked HTTP transport, and intent extraction behavior.

Not implemented yet:

- Final LLM answer generation after FHIR tool execution.
- Real cost estimation from token usage.
- Authentication/access control.
- Frontend integration.

## Main Rule

Chatbot service code must use FHIR REST endpoints for structured medical data. Do not query HAPI PostgreSQL tables directly.

## Local Run

```powershell
cd chatbot-service
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## HAPI FHIR Dependency

Expected local FHIR base URL:

```text
http://localhost:8080/fhir
```

The HAPI stack is managed in:

```text
infra/hapi-fhir
```

## Verified Demo Endpoints

Use these after the server starts:

```text
GET http://localhost:8000/health
GET http://localhost:8000/fhir/status
GET http://localhost:8000/patients/demo-patient-001
GET http://localhost:8000/patients/demo-patient-001/observations?limit=5
GET http://localhost:8000/patients/demo-patient-001/conditions
GET http://localhost:8000/patients/demo-patient-001/medications
POST http://localhost:8000/chat
```

## Intent Extraction

The chat route first extracts a tool plan:

```text
message + optional patient_id
  -> IntentExtractor
  -> tool_name + patient_id + optional limit/observation_type
  -> FHIR retrieval function
```

When configured, the OpenAI extractor asks the model to select one of these tools:

```text
get_patient_by_id
search_patients
get_observations
get_conditions
get_medication_requests
unsupported_question
```

Without `OPENAI_API_KEY`, `RuleBasedIntentExtractor` keeps local demo behavior working.

## Evidence Shape

Chat responses now keep both a compact summary and detailed normalized FHIR data:

```json
{
  "resource_type": "Observation",
  "id": "demo-hba1c-detailed-003",
  "summary": "HbA1c",
  "data": {
    "status": "final",
    "effective_time": "2026-05-24T14:18:00+07:00",
    "encounter": "Encounter/demo-encounter-004",
    "value": {"value": 7.2, "unit": "%"},
    "interpretation": [{"text": "High"}],
    "reference_range": [{"text": "Non-diabetes reference threshold."}],
    "note": ["Demo value for testing interpretation and reference range formatting."]
  }
}
```

The normalizer preserves detailed fields for Patient, Encounter, Observation, Condition, and MedicationRequest while keeping the older summary fields compatible.

## Verification Result

Last checked on 2026-05-30:

```text
python -m unittest discover tests: 34 tests passed
python -m compileall app api agents fhir tests: passed
app import: passed
GET /health: passed
GET /fhir/status: passed
GET /patients/demo-patient-001: passed
GET /patients/demo-patient-001/observations?limit=5: passed
GET /patients/demo-patient-001/conditions: passed
GET /patients/demo-patient-001/medications: passed
POST /chat medication demo: passed
LLM/tool-call intent extraction fallback: passed
Chatbot service dev server: http://localhost:8000
Detailed evidence passthrough via POST /chat: passed
```
