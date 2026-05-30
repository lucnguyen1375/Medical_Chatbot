# App PostgreSQL

PostgreSQL database for the web application layer.

This database is separate from the HAPI FHIR PostgreSQL database. Use it for Spring Boot application data such as users, chat sessions, chat messages, usage logs, and quota policies.

No application tables or migrations are created yet.

## Connection

```text
Host: localhost
Port: 5433
Database: medical_chatbot_app
User: app_user
Password: app_password
```

## Run

From the repository root:

```powershell
docker compose -f infra/app-postgres/docker-compose.yml up -d
```

## Check

```powershell
docker compose -f infra/app-postgres/docker-compose.yml ps
docker exec medical-chatbot-app-postgres psql -U app_user -d medical_chatbot_app -c "select current_user, current_database();"
```

## Stop

```powershell
docker compose -f infra/app-postgres/docker-compose.yml down
```

To remove persisted data:

```powershell
docker compose -f infra/app-postgres/docker-compose.yml down -v
```
