from fastapi import FastAPI

from api.fhir_routes import router as fhir_router
from api.health_routes import router as health_router
from app.config import get_settings


settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
)

app.include_router(health_router)
app.include_router(fhir_router)
