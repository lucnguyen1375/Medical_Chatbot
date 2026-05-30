# Chatbot Service Setup Notes

## What Was Built

This folder contains the first FastAPI chatbot/FHIR service slice for the Medical Chatbot project.

Implemented:

- FastAPI service entrypoint.
- Health endpoint.
- FHIR status endpoint.
- Patient, Observation, Condition, and MedicationRequest read endpoints.
- Rule-based demo `POST /chat` endpoint for patient, observation, condition, and medication questions.
- Central FHIR HTTP client using HAPI FHIR REST APIs.
- Normalizers that convert raw FHIR resources/Bundles into compact JSON for app and future LLM usage.
- Unit tests for normalizers, FHIR client behavior with mocked HTTP transport, and chat route intent/patient parsing.

Not implemented yet:

- LLM orchestration/tool calling.
- Real usage, token, and cost tracking.
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

## Verification Result

Last checked on 2026-05-29:

```text
python -m py_compile chatbot-service modules: passed
python -m unittest discover tests: 9 tests passed
app import: passed
GET /health: passed
GET /fhir/status: passed
GET /patients/demo-patient-001: passed
GET /patients/demo-patient-001/observations?limit=5: passed
GET /patients/demo-patient-001/conditions: passed
GET /patients/demo-patient-001/medications: passed
POST /chat medication demo: passed
Chatbot service dev server: http://localhost:8000
```
