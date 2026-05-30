# Frontend Notes

This folder contains the first minimal static frontend for testing the backend flow.

Implemented:

- Chat UI.
- Spring backend URL and patient ID inputs.
- Full chat flow test through `POST /api/chat`.
- Response metadata panel for session, intent, tool, patient, evidence, and usage.
- Selectable patient candidates when the backend returns `needs_patient_selection`.

Planned responsibilities:

- Patient data views exposed through the Spring Boot backend.
- Usage/quota warnings returned by the backend.
- Chat history loading once Spring exposes history APIs.

The frontend should call the Spring Boot backend only. It should not call the FastAPI chatbot service or HAPI FHIR directly.
