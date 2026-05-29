# AGENTS.md

## 1. Project Overview

This project builds a medical chatbot that helps users query healthcare data through natural language.

The system is designed around **FHIR-based structured data retrieval**, not direct SQL generation by the LLM.

Main goals:

- Allow users to ask natural-language questions about patients, encounters, observations, conditions, and medication requests.
- Retrieve structured healthcare data from a **HAPI FHIR Server**.
- Store FHIR data internally through **PostgreSQL**, managed by HAPI FHIR JPA.
- Use the LLM for intent understanding, query planning, answer generation, and optional document-based explanation.
- Track usage, quota, token consumption, and estimated AI cost.
- Apply basic optimization techniques such as caching, context reduction, and safe tool calling.

The chatbot must not directly access or modify HAPI FHIR internal database tables.

---

## 2. Core Architecture

Recommended architecture:

```text
User
  ↓
Chat UI / Frontend
  ↓
Chatbot Backend
  ↓
LLM Orchestrator / Agent Layer
  ↓
Tool Calling Layer
  ↓
FHIR Retrieval Service
  ↓
HAPI FHIR JPA Server
  ↓
PostgreSQL
```

Optional document retrieval flow:

```text
User Question
  ↓
LLM Orchestrator
  ↓
RAG Service
  ↓
Vector Database / Document Store
  ↓
Medical guideline / explanation documents
```

The system should use a **hybrid approach**:

- FHIR API for structured patient data.
- RAG for unstructured documents, medical explanations, guidelines, and long notes.
- Usage management for request count, token count, and cost estimation.

---

## 3. Important Rule: Do Not Query HAPI PostgreSQL Directly

HAPI FHIR uses PostgreSQL as an internal persistence layer through JPA/Hibernate.

The database schema is not a simple business schema like:

```sql
patients(id, name, gender)
observations(id, patient_id, code, value)
conditions(id, patient_id, diagnosis)
```

Instead, HAPI creates internal tables such as:

```text
hfj_resource
hfj_res_ver
hfj_spidx_token
hfj_spidx_string
hfj_spidx_date
hfj_res_link
...
```

These tables are implementation details.

Therefore:

```text
Correct:
LLM → intent/parameters → Backend → FHIR REST API → HAPI FHIR → PostgreSQL

Incorrect:
LLM → generated SQL → PostgreSQL internal HAPI tables
```

The chatbot backend must communicate with HAPI FHIR through REST endpoints such as:

```http
GET /fhir/Patient
GET /fhir/Patient/{id}
GET /fhir/Observation?patient=Patient/{id}
GET /fhir/Encounter?patient=Patient/{id}
GET /fhir/Condition?patient=Patient/{id}
GET /fhir/MedicationRequest?patient=Patient/{id}
```

---

## 4. Main Components

### 4.1 Chat UI

Responsibilities:

- Display chat messages.
- Send user questions to the backend.
- Show chatbot answers.
- Optionally show retrieved evidence or source data.
- Show usage/quota warnings if needed.

The frontend should not call the FHIR Server directly.

---

### 4.2 Chatbot Backend

Responsibilities:

- Receive user messages.
- Manage chat sessions.
- Store conversation history.
- Call the LLM orchestrator.
- Call FHIR retrieval tools.
- Call RAG tools when needed.
- Track usage and quota.
- Return final answers to the frontend.

The backend is the central coordination layer.

---

### 4.3 LLM Orchestrator / Agent Layer

Responsibilities:

- Understand the user's intent.
- Extract useful entities such as patient name, patient ID, date range, resource type, lab type, or medication name.
- Decide which tool should be called.
- Avoid hallucinating medical data.
- Generate a final answer only from retrieved data.

The LLM should produce structured tool calls, not raw SQL.

Example internal output:

```json
{
  "intent": "get_patient_observations",
  "parameters": {
    "patient_id": "Patient/123",
    "observation_type": "blood_pressure",
    "limit": 5
  }
}
```

---

### 4.4 FHIR Retrieval Service

Responsibilities:

- Convert structured intent and parameters into FHIR REST API calls.
- Call HAPI FHIR Server.
- Normalize FHIR JSON responses into a simpler format for the LLM.
- Handle errors from the FHIR Server.
- Prevent unsafe or overly broad queries.

Example:

```text
Input:
{
  "patient_id": "Patient/123",
  "resource": "Observation",
  "code": "blood-pressure",
  "limit": 5
}

FHIR request:
GET /fhir/Observation?patient=Patient/123&code=...&_sort=-date&_count=5
```

---

### 4.5 HAPI FHIR Server

Responsibilities:

- Provide standard FHIR REST API.
- Validate FHIR resources.
- Store resources through JPA/Hibernate.
- Maintain search indexes.
- Manage FHIR resource history and references.

Recommended setup:

```text
HAPI FHIR JPA Server + PostgreSQL
```

The HAPI FHIR Server should be treated as the source of truth for structured healthcare data.

---

### 4.6 PostgreSQL

Responsibilities:

- Persist FHIR resources and indexes for HAPI FHIR.
- Store HAPI FHIR internal data.
- Optionally store application-specific data in separate schemas or databases.

Do not manually edit HAPI internal tables unless there is a clear maintenance reason.

If the project needs application tables, use separate tables/schemas such as:

```text
app_users
chat_sessions
chat_messages
usage_logs
quota_policies
cache_entries
```

Do not mix application logic with HAPI internal tables.

---

### 4.7 RAG Service

Use RAG for unstructured or semi-structured knowledge, such as:

- Medical explanation documents.
- Hospital rules.
- Drug usage guidelines.
- Clinical guideline PDFs.
- Long doctor notes.
- General explanations of lab results.

Do not use RAG as the primary method for exact structured patient data.

Good use of RAG:

```text
"What does a high glucose value mean?"
"Explain this diagnosis in simple language."
"What are common causes of hypertension?"
```

Bad use of RAG:

```text
"What is the latest glucose result of Patient/123?"
"What medications is Patient/123 taking?"
"What was the most recent diagnosis?"
```

Those should use FHIR queries.

---

## 5. Data Retrieval Strategy

### 5.1 Structured Medical Data

Use FHIR API.

Examples:

| User Question | FHIR Resource |
|---|---|
| Patient information | Patient |
| Latest visit | Encounter |
| Blood pressure / heart rate / glucose | Observation |
| Diagnosis | Condition |
| Medication | MedicationRequest |
| Lab report | DiagnosticReport |

Example flow:

```text
User asks:
"What medications is Patient/123 taking?"

Backend:
GET /fhir/MedicationRequest?patient=Patient/123

LLM:
Summarize only the returned MedicationRequest data.
```

---

### 5.2 Unstructured Knowledge

Use RAG.

Example flow:

```text
User asks:
"What does this glucose value mean?"

Backend:
1. Retrieve the glucose Observation from FHIR.
2. Retrieve explanation documents through RAG.
3. Ask LLM to explain based on both sources.
```

---

### 5.3 Mixed Questions

Some questions require both FHIR and RAG.

Example:

```text
User asks:
"Patient/123 has high blood pressure. Explain what that means."
```

Recommended process:

```text
1. Use FHIR to retrieve blood pressure observations.
2. Use RAG to retrieve explanation about blood pressure ranges.
3. Generate a careful answer.
4. Avoid giving diagnosis unless the data explicitly supports it.
```

---

## 6. Tool Calling Rules

The LLM should call predefined backend tools instead of producing database queries directly.

Recommended tools:

### search_patient

Find patient by name, identifier, birth date, or phone number.

```json
{
  "name": "search_patient",
  "parameters": {
    "name": "Nguyen Van A",
    "birth_date": "2003-01-01"
  }
}
```

### get_patient_by_id

Retrieve a patient by FHIR ID.

```json
{
  "name": "get_patient_by_id",
  "parameters": {
    "patient_id": "Patient/123"
  }
}
```

### get_observations

Retrieve observations for a patient.

```json
{
  "name": "get_observations",
  "parameters": {
    "patient_id": "Patient/123",
    "type": "blood_pressure",
    "limit": 5
  }
}
```

### get_encounters

Retrieve encounters for a patient.

```json
{
  "name": "get_encounters",
  "parameters": {
    "patient_id": "Patient/123",
    "limit": 5
  }
}
```

### get_conditions

Retrieve conditions or diagnoses for a patient.

```json
{
  "name": "get_conditions",
  "parameters": {
    "patient_id": "Patient/123"
  }
}
```

### get_medication_requests

Retrieve medication requests for a patient.

```json
{
  "name": "get_medication_requests",
  "parameters": {
    "patient_id": "Patient/123"
  }
}
```

### retrieve_documents

Retrieve relevant text chunks from medical documents.

```json
{
  "name": "retrieve_documents",
  "parameters": {
    "query": "meaning of high glucose level"
  }
}
```

---

## 7. Usage, Cost, and Quota Management

The system should track usage at multiple levels.

### 7.1 Request Quota

Track how many AI calls a user makes.

Example:

```text
Free user: 50 AI requests/day
Admin user: 500 AI requests/day
```

Useful for limiting spam and abuse.

---

### 7.2 Token Quota

Track input and output tokens.

Example:

```text
User A used 25,000 tokens today.
Limit: 100,000 tokens/day.
```

Useful because one request can be cheap or expensive depending on context size.

---

### 7.3 Cost Quota

Track estimated cost.

Example:

```text
User A used approximately $0.42 today.
Limit: $1.00/day.
```

Useful because different models have different prices.

---

### 7.4 Why Track All Three?

Request count, token count, and cost measure different things.

| Metric | Controls |
|---|---|
| Request count | Number of calls |
| Token count | Context and response size |
| Cost | Actual money spent |

A user may send few requests but each request may include large context. Another user may send many tiny requests. Tracking only one metric is not enough.

---

## 8. Caching Rules

The system may cache safe and repeatable results.

### 8.1 Cache Hit

A cache hit means the system found a previous result for the same or equivalent request and reused it instead of calling the expensive service again.

Example:

```text
Question:
"What does high glucose mean?"

Cached result exists:
Yes → cache hit
No → cache miss
```

### 8.2 Good Cache Candidates

Good candidates:

- General medical explanations.
- RAG retrieval results for common concepts.
- FHIR metadata.
- Code mappings such as LOINC/SNOMED mappings.
- Non-sensitive aggregate data.

Be careful with patient-specific data.

### 8.3 Patient Data Cache

Patient-specific data may change and is sensitive.

If cached, it should have:

- Short TTL.
- User/session scoping.
- Permission checks.
- No sharing across users.
- Clear invalidation logic.

---

## 9. Safety and Privacy Rules

This project deals with healthcare data, so safety matters.

The system must:

- Never invent patient data.
- Never claim a diagnosis unless the retrieved data explicitly says so.
- Avoid giving definitive medical advice.
- Prefer phrases such as "the record shows" or "according to the available data".
- Show uncertainty when data is missing.
- Respect access control.
- Avoid exposing data from one patient to another user.
- Log sensitive data carefully.
- Avoid storing raw medical records in LLM prompts unless necessary.

Recommended answer style:

```text
According to the available FHIR data, Patient/123 has the following recorded medications...
```

Not recommended:

```text
This patient definitely has disease X.
```

unless the Condition resource explicitly supports it.

---

## 10. Error Handling

The backend should handle these cases clearly:

### Patient Not Found

```text
No patient matched the provided information.
```

### Multiple Patients Found

```text
Multiple patients matched this name. Ask for birth date, identifier, or another disambiguation field.
```

### Resource Not Found

```text
No Observation records were found for this patient.
```

### FHIR Server Error

```text
The FHIR Server returned an error. Log the technical detail internally, but return a simple message to the user.
```

### LLM Tool Error

```text
The system could not complete the data retrieval step. Do not hallucinate an answer.
```

---

## 11. Suggested Backend Folder Structure

Example:

```text
src/
  app/
    main.py
    config.py

  api/
    chat_routes.py
    health_routes.py

  agents/
    orchestrator.py
    prompts.py
    tool_registry.py

  fhir/
    client.py
    patient_service.py
    observation_service.py
    encounter_service.py
    condition_service.py
    medication_service.py
    normalizer.py

  rag/
    retriever.py
    vector_store.py
    document_loader.py

  usage/
    usage_tracker.py
    quota_service.py
    cost_estimator.py

  cache/
    cache_service.py
    cache_keys.py

  db/
    models.py
    session.py
    migrations/

  security/
    auth.py
    access_control.py

  tests/
    test_fhir_client.py
    test_patient_service.py
    test_usage_tracker.py
    test_quota_service.py
```

Adjust this structure based on the actual framework.

---

## 12. Suggested Docker Compose Services

Recommended services:

```text
chatbot-backend
frontend
hapi-fhir
postgres
redis
vector-db
```

Minimum services for the FHIR demo:

```text
chatbot-backend
hapi-fhir
postgres
```

Optional:

```text
redis       → cache and rate limit
qdrant      → vector database for RAG
pgvector    → vector search inside PostgreSQL
```

---

## 13. Environment Variables

Recommended variables:

```env
# HAPI FHIR
FHIR_BASE_URL=http://localhost:8080/fhir

# PostgreSQL for app data
APP_DATABASE_URL=postgresql://app_user:app_password@localhost:5432/app_db

# PostgreSQL used by HAPI FHIR
HAPI_POSTGRES_DB=hapi
HAPI_POSTGRES_USER=admin
HAPI_POSTGRES_PASSWORD=admin

# LLM
LLM_PROVIDER=openai
LLM_MODEL=gpt-4.1-mini
LLM_API_KEY=replace_me

# Usage limits
DAILY_REQUEST_LIMIT=100
DAILY_TOKEN_LIMIT=100000
DAILY_COST_LIMIT_USD=1.00

# Cache
REDIS_URL=redis://localhost:6379
CACHE_TTL_SECONDS=300
```

Do not commit real API keys or passwords.

---

## 14. Development Guidelines

### 14.1 General

- Keep the LLM layer separate from the FHIR client layer.
- Keep FHIR API calls centralized in the `fhir/` module.
- Keep prompt templates centralized.
- Keep usage tracking centralized.
- Do not duplicate quota logic across controllers.
- Normalize FHIR responses before sending them to the LLM.

### 14.2 FHIR

- Prefer FHIR REST search parameters over custom database queries.
- Always limit broad searches with `_count`.
- Use `_sort=-date` when retrieving latest records.
- Use patient ID instead of patient name once the patient is identified.
- Handle Bundle responses properly.
- Preserve important fields such as `resourceType`, `id`, `code`, `display`, `effectiveDateTime`, `valueQuantity`, and `subject`.

### 14.3 LLM

- The LLM must not create facts that are not present in retrieved data.
- The LLM should ask for clarification if patient identity is ambiguous.
- The LLM should call tools rather than directly answering data questions from memory.
- The LLM should be given compact, normalized data instead of huge raw FHIR bundles whenever possible.

### 14.4 RAG

- Chunk documents by semantic sections, not arbitrary tiny fragments.
- Store source metadata.
- Return citations or source names when possible.
- Do not mix old/deprecated documents with current documents without marking them.

### 14.5 Testing

Test at least:

- Patient search.
- Observation retrieval.
- Latest encounter retrieval.
- Condition retrieval.
- MedicationRequest retrieval.
- FHIR Server unavailable.
- Empty FHIR result.
- Multiple patients matched.
- Quota exceeded.
- Cache hit and cache miss.
- LLM tool call parsing.

---

## 15. Example FHIR Requests

### Get server metadata

```http
GET /fhir/metadata
```

### Create a Patient

```http
POST /fhir/Patient
Content-Type: application/fhir+json

{
  "resourceType": "Patient",
  "name": [
    {
      "family": "Nguyen",
      "given": ["Van A"]
    }
  ],
  "gender": "male",
  "birthDate": "2003-01-01"
}
```

### Search Patient by Name

```http
GET /fhir/Patient?name=Nguyen
```

### Get Observations of a Patient

```http
GET /fhir/Observation?patient=Patient/123&_count=10
```

### Get Latest Observations

```http
GET /fhir/Observation?patient=Patient/123&_sort=-date&_count=5
```

### Get Conditions

```http
GET /fhir/Condition?patient=Patient/123
```

### Get Medication Requests

```http
GET /fhir/MedicationRequest?patient=Patient/123
```

---

## 16. Recommended Answer Generation Pattern

When answering a user, the chatbot should follow this pattern:

```text
1. State what data was found.
2. Mention the relevant time/date if available.
3. Summarize the result clearly.
4. Avoid overclaiming.
5. Mention if data is missing or incomplete.
```

Example:

```text
According to the available FHIR data, Patient/123 has three recorded blood pressure observations. The latest record is from 2026-05-20 with systolic 130 mmHg and diastolic 85 mmHg. This answer is based only on the available Observation resources.
```

---

## 17. Minimum Demo Scope

For a 5-week student project, the minimum reasonable scope is:

```text
1. HAPI FHIR + PostgreSQL running with Docker.
2. Mock data for Patient, Encounter, Observation, Condition, MedicationRequest.
3. Backend API for chat.
4. LLM intent extraction and tool calling.
5. FHIR retrieval tools.
6. Simple usage tracking: request count, token count, estimated cost.
7. Basic cache for repeated general explanation queries.
8. Optional RAG for medical explanation documents.
9. Simple frontend chat UI.
```

Do not overbuild microservices unless the team has enough time.

---

## 18. Out of Scope for Initial Version

Avoid these in the first version unless required:

- Real hospital integration.
- Real patient data.
- Full clinical decision support.
- Complex authorization model.
- Full SNOMED/LOINC terminology server integration.
- Fine-tuning a medical LLM.
- Writing SQL directly against HAPI FHIR internal tables.
- Building a complete EHR system.

---

## 19. Final Design Principle

The most important rule of this project:

```text
Use FHIR API for exact structured medical data.
Use RAG for explanations and documents.
Use the LLM for reasoning, orchestration, and answer generation.
Do not let the LLM directly query the HAPI PostgreSQL database.
```
