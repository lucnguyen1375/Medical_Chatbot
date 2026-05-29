# Chatbot Service Setup Notes

## What Was Built

This folder contains the first FastAPI chatbot/FHIR service slice for the Medical Chatbot project.

Implemented:

- FastAPI service entrypoint.
- Health endpoint.
- FHIR status endpoint.
- Patient, Observation, Condition, and MedicationRequest read endpoints.
- Central FHIR HTTP client using HAPI FHIR REST APIs.
- Normalizers that convert raw FHIR resources/Bundles into compact JSON for app and future LLM usage.
- Unit tests for normalizers and FHIR client behavior with mocked HTTP transport.

Not implemented yet:

- Chat session APIs.
- LLM orchestration/tool calling.
- Usage, token, and cost tracking.
- Authentication/access control.
- Spring Boot backend integration.
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
```

## Verification Result

Last checked on 2026-05-29:

```text
python -m py_compile chatbot-service modules: passed
python -m unittest discover tests: 5 tests passed
app import: passed
GET /health: passed
GET /fhir/status: passed
GET /patients/demo-patient-001: passed
GET /patients/demo-patient-001/observations?limit=5: passed
GET /patients/demo-patient-001/conditions: passed
GET /patients/demo-patient-001/medications: passed
Chatbot service dev server: http://localhost:8000
```
