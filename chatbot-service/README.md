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

## LLM Intent And Answer Generation

The chat endpoint extracts a FHIR tool plan before calling HAPI FHIR.

Set these variables in `chatbot-service/.env` to enable OpenAI tool calling and final answer generation:

```env
LLM_PROVIDER=openai
LLM_MODEL=gpt-4.1-mini
OPENAI_API_KEY=replace_me
LLM_REQUEST_TIMEOUT_SECONDS=20
ENABLE_LLM_ANSWER=true
```

If `OPENAI_API_KEY` is missing, the service automatically uses a local rule-based extractor and template answer fallback so demos still run.

Supported tool plans:

```text
get_patient_by_id
get_observations
get_conditions
get_medication_requests
unsupported_question
```

After FHIR retrieval, `agents/answer_generator.py` can call the LLM again with only the normalized `evidence.data` payload. The LLM does not query FHIR or PostgreSQL directly.

## Endpoints

- `GET /health`
- `GET /fhir/status`
- `GET /patients/{patient_id}`
- `GET /patients/{patient_id}/observations?limit=5`
- `GET /patients/{patient_id}/conditions`
- `GET /patients/{patient_id}/medications`
- `POST /chat`

Demo patient:

```text
demo-patient-001
```

Demo chat request:

```powershell
$body = @{
  message = "What medications is Patient/demo-patient-001 taking?"
  patient_id = "demo-patient-001"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8000/chat" -Method Post -ContentType "application/json" -Body $body
```

The response includes `tool_name`, `intent_source`, `answer_source`, `answer_usage`, and combined `usage`. `answer_source` is `llm` when OpenAI writes the final answer, otherwise `template` or `template_fallback`.

## Tests

```powershell
cd chatbot-service
python -m unittest discover tests
```
