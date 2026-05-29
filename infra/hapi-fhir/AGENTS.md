# HAPI FHIR Setup Notes

## What Was Built

This folder contains the first infrastructure slice for the Medical Chatbot project:

- HAPI FHIR JPA Server.
- PostgreSQL used only as HAPI FHIR internal persistence.
- PostgreSQL host port configured as `localhost:5432` for GUI inspection tools.
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
    demo-data-transaction-bundle.json
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
Port: 5432
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

The seed Bundle uses fixed resource IDs and transaction `PUT`, so running the seed script repeatedly updates the same resources instead of creating duplicates.

Seeded resources:

- `Patient/demo-patient-001`
- `Patient/demo-patient-002`
- `Encounter/demo-encounter-001`
- `Observation/demo-blood-pressure-001`
- `Observation/demo-glucose-001`
- `Condition/demo-condition-001`
- `MedicationRequest/demo-medication-request-001`

## Verified Endpoints

These endpoints were checked through FHIR REST calls:

- `GET http://localhost:8080/fhir/metadata`
- `GET http://localhost:8080/fhir/Patient/demo-patient-001`
- `GET http://localhost:8080/fhir/Observation?patient=Patient/demo-patient-001&_sort=-date&_count=5`
- `GET http://localhost:8080/fhir/Condition?patient=Patient/demo-patient-001`
- `GET http://localhost:8080/fhir/MedicationRequest?patient=Patient/demo-patient-001`

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
PostgreSQL host port mapping: 0.0.0.0:5432->5432/tcp verified
Local Windows PostgreSQL service stopped: postgresql-x64-18
Created dev compatibility role: postgres/postgres
```

## Important Rules

- Do not read or write HAPI PostgreSQL internal tables from application code.
- Use FHIR REST endpoints for exact structured medical data.
- Use patient-specific queries with `_count` limits when possible.
- Keep real patient data out of this demo seed folder.
