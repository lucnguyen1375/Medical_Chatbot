# Frontend Notes

This folder is reserved for the web frontend.

Planned responsibilities:

- Chat UI.
- Patient data views exposed through the Spring Boot backend.
- Usage/quota warnings returned by the backend.

The frontend should call the Spring Boot backend only. It should not call the FastAPI chatbot service or HAPI FHIR directly.
