# Medical Chatbot Service

FastAPI chatbot/FHIR service for the Medical Chatbot project.

This service sits behind the future Spring Boot backend. It calls HAPI FHIR through REST APIs only and must not query HAPI PostgreSQL tables directly.

## Run

From the repository root:

```powershell
cd chatbot-service
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Default HAPI FHIR URL:

```text
http://localhost:8080/fhir
```

Override it with:

```powershell
$env:FHIR_BASE_URL="http://localhost:8080/fhir"
```

## Endpoints

- `GET /health`
- `GET /fhir/status`
- `GET /patients/{patient_id}`
- `GET /patients/{patient_id}/observations?limit=5`
- `GET /patients/{patient_id}/conditions`
- `GET /patients/{patient_id}/medications`

Demo patient:

```text
demo-patient-001
```

## Tests

```powershell
cd chatbot-service
python -m unittest discover tests
```
