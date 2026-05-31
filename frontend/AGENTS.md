# Frontend Notes

This folder contains the static Staff Demo Dashboard for testing the webapp flow.

Implemented:

- Three-column staff dashboard:
  - ChatGPT-like chat history sidebar on the left
  - fixed-height chat area with an internal message scroll panel in the center
  - compact patient search plus patient detail/evidence panel on the right
- Patient search through Spring, shown only after the user searches:
  - name
  - phone
  - birth date
  - identifier
  - direct `demo-patient-*` lookup
- Selected-patient context for chat requests.
- Patient detail panels for:
  - demographics
  - encounters
  - observations
  - conditions
  - medications
- Chat history panel using Spring history APIs.
- Session loading from `GET /api/chat/sessions/{sessionId}/messages`.
- Chat history rows use fixed minimum height and text clamping to avoid overlapping long titles/previews.
- Ambiguous patient candidate selection remains supported.
- Evidence detail keeps JSON in collapsible `<details>` blocks.
- Vietnamese UI text for touched frontend files.

Current endpoint dependency:

```text
Spring backend: http://localhost:8081
```

Verified:

```text
node --check frontend/app.js: passed
Frontend server: http://localhost:5173
Patient search through Spring: passed
Chat history read through Spring: passed
Selected patient chat through Spring: passed
```

Rule:

The frontend should call the Spring Boot backend only. It should not call the FastAPI chatbot service or HAPI FHIR directly.
