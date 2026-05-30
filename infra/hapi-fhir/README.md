# HAPI FHIR Local Stack

This folder owns the local HAPI FHIR stack for the Medical Chatbot project.

It runs:

- HAPI FHIR JPA Server at `http://localhost:8080/fhir`
- PostgreSQL as HAPI's internal persistence database, published at `localhost:5434`
- Demo FHIR resources seeded through the FHIR REST API

The chatbot backend must connect through FHIR REST endpoints. Do not query or modify HAPI PostgreSQL tables directly.

## Run

From the repository root:

```powershell
docker compose -f infra/hapi-fhir/docker-compose.yml up -d
python infra/hapi-fhir/scripts/wait_for_hapi.py
python infra/hapi-fhir/scripts/seed_fhir_data.py
python infra/hapi-fhir/scripts/check_connection.py
```

Run the seed script again any time you want to restore the demo resources. It uses fixed FHIR IDs and `PUT` inside a transaction Bundle, so it updates existing resources instead of creating duplicates.

## Useful Endpoints

- `GET http://localhost:8080/fhir/metadata`
- `GET http://localhost:8080/fhir/Patient/demo-patient-001`
- `GET http://localhost:8080/fhir/Observation?patient=Patient/demo-patient-001&_sort=-date&_count=5`
- `GET http://localhost:8080/fhir/Condition?patient=Patient/demo-patient-001`
- `GET http://localhost:8080/fhir/MedicationRequest?patient=Patient/demo-patient-001`

## Backend Connection

When the future backend runs on the host machine:

```env
FHIR_BASE_URL=http://localhost:8080/fhir
```

When it runs inside the same Docker Compose network:

```env
FHIR_BASE_URL=http://hapi-fhir:8080/fhir
```

## Inspect PostgreSQL From Host

The Compose file publishes PostgreSQL to the host:

```text
Host: localhost
Port: 5434
Database: hapi
User: admin
Password: admin
```

For tools that already have a local `postgres` user profile, this dev stack also supports:

```text
Host: localhost
Port: 5434
Database: hapi
User: postgres
Password: postgres
```

Use this only for debugging and learning HAPI's internal schema. Application code should not query HAPI tables directly.

## Stop

```powershell
docker compose -f infra/hapi-fhir/docker-compose.yml down
```

To remove persisted HAPI data too:

```powershell
docker compose -f infra/hapi-fhir/docker-compose.yml down -v
```
