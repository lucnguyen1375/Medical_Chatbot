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

The frontend should call the Spring Boot backend only. It must not call the FastAPI chatbot service or HAPI FHIR directly.
