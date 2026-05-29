# Spring Backend

Main backend for the Medical Chatbot web application.

This Spring Boot service is the frontend-facing backend. It currently proxies selected patient/FHIR read APIs to the FastAPI chatbot service at `http://localhost:8000`.

## Runtime

- Java target: 21
- Spring Boot: 3.5.9
- Local port: `8081`
- Chatbot service base URL: `http://localhost:8000`

## Run

From this folder:

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd test
```

## Endpoints

- `GET /api/health`
- `GET /api/chatbot/status`
- `GET /api/patients/{patientId}`
- `GET /api/patients/{patientId}/observations?limit=5`
- `GET /api/patients/{patientId}/conditions?limit=20`
- `GET /api/patients/{patientId}/medications?limit=20`

Demo:

```text
http://localhost:8081/api/patients/demo-patient-001
```

## Architecture Rule

The frontend should call this Spring Boot backend. This backend calls the FastAPI chatbot service. It should not query HAPI PostgreSQL internal tables directly.
