# Spring Backend Notes

This folder contains the main Spring Boot backend for the web application.

Implemented:

- Java 21 Spring Boot project generated with Maven Wrapper.
- Spring Boot runs on port `8081`.
- `ChatbotServiceClient` calls the FastAPI chatbot service at `http://localhost:8000`.
- Frontend-facing REST endpoints under `/api`.
- Basic exception handling for chatbot-service errors.
- Controller tests for health, patient proxy, and validation.

Not implemented yet:

- Authentication and authorization.
- App database for users, sessions, messages, usage logs, and quotas.
- Chat API.
- Frontend integration.

## Main Rule

The Spring Boot backend should not query HAPI PostgreSQL internal tables. It should call the chatbot service or HAPI FHIR REST APIs through controlled service layers.

## Local Run

```powershell
cd spring-backend
.\mvnw.cmd spring-boot:run
```

## Verified Endpoints

Last checked on 2026-05-29:

```text
GET http://localhost:8081/api/health
GET http://localhost:8081/api/chatbot/status
GET http://localhost:8081/api/patients/demo-patient-001
GET http://localhost:8081/api/patients/demo-patient-001/observations?limit=5
GET http://localhost:8081/api/patients/demo-patient-001/conditions
GET http://localhost:8081/api/patients/demo-patient-001/medications
```

## Verification Result

```text
.\mvnw.cmd test: passed, 4 tests
Spring Boot app start: passed
Spring Boot dev server: http://localhost:8081
Spring -> chatbot-service -> HAPI FHIR integration: passed
```
