# HAPI FHIR Setup Notes

## What Was Built

This folder contains the first infrastructure slice for the Medical Chatbot project:

- HAPI FHIR JPA Server.
- PostgreSQL used only as HAPI FHIR internal persistence.
- PostgreSQL host port configured as `localhost:5434` for GUI inspection tools.
- Demo FHIR transaction Bundle with fixed IDs.
- Python scripts to wait for HAPI, seed demo data, and check FHIR connectivity.

The backend and frontend have not been created yet. Future backend code should connect to HAPI through the FHIR REST API, not through HAPI PostgreSQL tables.

## Folder Contents

```text
infra/hapi-fhir/
  docker-compose.yml
  config/
    application.yaml
  seed/
    ambiguous-patient-demo-data-transaction-bundle.json
    demo-data-transaction-bundle.json
    detailed-demo-data-transaction-bundle.json
    extended-demo-data-transaction-bundle.json
  scripts/
    wait_for_hapi.py
    seed_fhir_data.py
    check_connection.py
  README.md
  AGENTS.md
```

## How To Run

From the repository root:

```powershell
docker compose -f infra/hapi-fhir/docker-compose.yml up -d
python infra/hapi-fhir/scripts/wait_for_hapi.py
python infra/hapi-fhir/scripts/seed_fhir_data.py
python infra/hapi-fhir/scripts/check_connection.py
```

The local FHIR base URL is:

```text
http://localhost:8080/fhir
```

Future backend services running inside the same Docker Compose network should use:

```text
http://hapi-fhir:8080/fhir
```

For database inspection tools such as DBeaver or pgAdmin:

```text
Host: localhost
Port: 5434
Database: hapi
User: admin
Password: admin
```

Compatibility dev login for existing pgAdmin profiles:

```text
User: postgres
Password: postgres
```

Only use this for inspection/debugging. Chatbot application code must still use FHIR REST APIs.

## Demo Data Seeded

The seed script posts every JSON transaction Bundle in `seed/`. The Bundles use fixed resource IDs and transaction `PUT`, so running the seed script repeatedly updates the same resources instead of creating duplicates.

Seeded resources:

- `Patient/demo-patient-001`
- `Patient/demo-patient-002`
- `Patient/demo-patient-003`
- `Patient/demo-patient-004`
- `Patient/demo-patient-005`
- `Patient/demo-patient-006`
- `Encounter/demo-encounter-001`
- `Encounter/demo-encounter-002`
- `Encounter/demo-encounter-003`
- `Encounter/demo-encounter-004`
- `Encounter/demo-encounter-005`
- `Observation/demo-blood-pressure-001`
- `Observation/demo-blood-pressure-002`
- `Observation/demo-blood-pressure-003`
- `Observation/demo-blood-pressure-004`
- `Observation/demo-glucose-001`
- `Observation/demo-glucose-002`
- `Observation/demo-glucose-003`
- `Observation/demo-heart-rate-001`
- `Observation/demo-heart-rate-002`
- `Observation/demo-hba1c-001`
- `Observation/demo-total-cholesterol-001`
- `Condition/demo-condition-001`
- `Condition/demo-condition-002`
- `Condition/demo-condition-003`
- `Condition/demo-condition-004`
- `Condition/demo-condition-005`
- `Condition/demo-condition-006`
- `MedicationRequest/demo-medication-request-001`
- `MedicationRequest/demo-medication-request-002`
- `MedicationRequest/demo-medication-request-003`
- `MedicationRequest/demo-medication-request-004`
- `MedicationRequest/demo-medication-request-005`
- `MedicationRequest/demo-medication-request-006`

Demo patient map:

| Patient | Name | Phone | Test data |
|---|---|---|---|
| `demo-patient-001` | Nguyen Van A | `0900000001` | hypertension, prediabetes, blood pressure, glucose, heart rate, amlodipine, metformin |
| `demo-patient-002` | Tran Thi B | `0900000002` | resolved upper respiratory infection, blood pressure, heart rate, paracetamol |
| `demo-patient-003` | Le Minh C | `0900000003` | type 2 diabetes, glucose, detailed HbA1c, metformin |
| `demo-patient-004` | Pham Thu D | `0900000004` | hypertension, hyperlipidemia, blood pressure, cholesterol, atorvastatin, losartan |
| `demo-patient-005` | Hoang Anh E | `0900000005` | asthma, oxygen saturation, temperature, salbutamol inhaler, address/contact/email demo fields |
| `demo-patient-006` | Nguyen Van B | `0900000006` | ambiguous-name test candidate for patient selection flow |

## Verified Endpoints

These endpoints were checked through FHIR REST calls:

- `GET http://localhost:8080/fhir/metadata`
- `GET http://localhost:8080/fhir/Patient/demo-patient-001`
- `GET http://localhost:8080/fhir/Patient/demo-patient-003`
- `GET http://localhost:8080/fhir/Observation?patient=Patient/demo-patient-001&_sort=-date&_count=5`
- `GET http://localhost:8080/fhir/Observation?patient=Patient/demo-patient-004&_sort=-date&_count=10`
- `GET http://localhost:8080/fhir/Condition?patient=Patient/demo-patient-001`
- `GET http://localhost:8080/fhir/Condition?patient=Patient/demo-patient-004`
- `GET http://localhost:8080/fhir/MedicationRequest?patient=Patient/demo-patient-001`
- `GET http://localhost:8080/fhir/MedicationRequest?patient=Patient/demo-patient-004`

## Verification Result

Last checked on 2026-05-29:

```text
docker compose config: passed
python py_compile scripts: passed
wait_for_hapi.py: passed
seed_fhir_data.py first run: 201 Created for all 7 resources
seed_fhir_data.py second run: 200 OK for all 7 resources
check_connection.py: passed
FHIR version from metadata: 4.0.1
Demo patient: Nguyen Van A
Observation count: 2
Condition count: 1
MedicationRequest count: 1
PostgreSQL host port mapping: 0.0.0.0:5434->5432/tcp verified
Local Windows PostgreSQL service conflict avoided by using host port 5434
Created dev compatibility role: postgres/postgres
```

Last extended seed verification on 2026-05-30:

```text
seed_fhir_data.py first extended run: 7 existing resources updated, 26 extended resources created
seed_fhir_data.py second extended run: 33 resources updated with 200 OK
Total demo resources in seed files: 33
Patient/demo-patient-003: verified, phone 0900000003
Patient/demo-patient-004 observations: 2 entries
Patient/demo-patient-004 conditions: 2 entries
Patient/demo-patient-004 MedicationRequest: 2 entries
```

Last detailed seed verification on 2026-05-30:

```text
Added detailed-demo-data-transaction-bundle.json
seed_fhir_data.py: passed
Total demo resources in seed files: 41
Added Practitioner/demo-doctor-001 for valid performer/requester references
Added Patient/demo-patient-005 with telecom, email, address, and contact data
Added Encounter/demo-encounter-006 with participant, reason, period, and location
Added detailed Observation resources with interpretation, referenceRange, issued, performer, and note
Added Condition/demo-condition-006 with severity, onset, asserter, and note
Added MedicationRequest/demo-medication-006 with dosageInstruction, reasonCode, reasonReference, dispenseRequest, and note
check_connection.py: passed
```

Last ambiguous patient seed verification on 2026-05-30:

```text
Added ambiguous-patient-demo-data-transaction-bundle.json
Added Patient/demo-patient-006 with family name Nguyen
seed_fhir_data.py: passed
Total demo resources in seed files: 42
Spring chat smoke test for "so dien thoai cua Nguyen": returned candidates demo-patient-001 and demo-patient-006
```

## Important Rules

- Do not read or write HAPI PostgreSQL internal tables from application code.
- Use FHIR REST endpoints for exact structured medical data.
- Use patient-specific queries with `_count` limits when possible.
- Keep real patient data out of this demo seed folder.
