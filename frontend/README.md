# Frontend

Web frontend for the Medical Chatbot project.

This folder contains a minimal static chat UI for testing the local Spring backend.

## Run

From this folder:

```powershell
python -m http.server 5173 --bind 127.0.0.1
```

Open:

```text
http://localhost:5173
```

The UI calls:

```text
POST http://localhost:8081/api/chat
```

The response metadata panel shows `intent`, `tool_name`, `answer_source`, patient scope, evidence, and usage. Evidence items can be expanded to inspect normalized FHIR data.

The frontend should call the Spring Boot backend only. It must not call the FastAPI chatbot service or HAPI FHIR directly.
