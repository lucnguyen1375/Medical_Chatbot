from dataclasses import dataclass
from typing import Any

import httpx

from app.config import get_settings


class FhirClientError(Exception):
    def __init__(self, user_message: str, technical_detail: str | None = None) -> None:
        super().__init__(technical_detail or user_message)
        self.user_message = user_message
        self.technical_detail = technical_detail or user_message


class FhirNotFoundError(FhirClientError):
    pass


@dataclass(frozen=True)
class FhirClient:
    base_url: str
    timeout_seconds: float = 20
    transport: httpx.AsyncBaseTransport | None = None

    async def get_metadata(self) -> dict[str, Any]:
        return await self._get("metadata")

    async def get_patient(self, patient_id: str) -> dict[str, Any]:
        return await self._get(f"Patient/{patient_id}")

    async def search_patient_resources(
        self,
        resource_type: str,
        patient_id: str,
        *,
        count: int = 10,
        sort: str | None = None,
    ) -> dict[str, Any]:
        params: dict[str, str | int] = {
            "patient": f"Patient/{patient_id}",
            "_count": count,
        }
        if sort:
            params["_sort"] = sort
        return await self._get(resource_type, params=params)

    async def _get(
        self,
        path: str,
        *,
        params: dict[str, str | int] | None = None,
    ) -> dict[str, Any]:
        url = f"{self.base_url.rstrip('/')}/{path.lstrip('/')}"
        try:
            async with httpx.AsyncClient(
                timeout=self.timeout_seconds,
                transport=self.transport,
                headers={"Accept": "application/fhir+json"},
            ) as client:
                response = await client.get(url, params=params)
                if response.status_code == 404:
                    raise FhirNotFoundError("Requested FHIR resource was not found.")
                response.raise_for_status()
                payload = response.json()
        except FhirClientError:
            raise
        except httpx.HTTPStatusError as exc:
            raise FhirClientError(
                "The FHIR server returned an error.",
                f"HTTP {exc.response.status_code}: {exc.response.text}",
            ) from exc
        except (httpx.HTTPError, ValueError) as exc:
            raise FhirClientError(
                "The FHIR server is unavailable or returned invalid data.",
                str(exc),
            ) from exc

        if not isinstance(payload, dict):
            raise FhirClientError("The FHIR server returned invalid JSON.")
        return payload


def get_fhir_client() -> FhirClient:
    settings = get_settings()
    return FhirClient(
        base_url=settings.normalized_fhir_base_url,
        timeout_seconds=settings.fhir_request_timeout_seconds,
    )
