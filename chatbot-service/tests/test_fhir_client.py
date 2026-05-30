import unittest

import httpx

from fhir.client import FhirClient, FhirNotFoundError


class FhirClientTests(unittest.IsolatedAsyncioTestCase):
    async def test_get_metadata_success(self) -> None:
        def handler(request: httpx.Request) -> httpx.Response:
            self.assertEqual(str(request.url), "http://fhir.test/metadata")
            return httpx.Response(200, json={"resourceType": "CapabilityStatement"})

        client = FhirClient(
            base_url="http://fhir.test",
            transport=httpx.MockTransport(handler),
        )

        result = await client.get_metadata()

        self.assertEqual(result["resourceType"], "CapabilityStatement")

    async def test_patient_not_found(self) -> None:
        def handler(request: httpx.Request) -> httpx.Response:
            return httpx.Response(404, json={"resourceType": "OperationOutcome"})

        client = FhirClient(
            base_url="http://fhir.test",
            transport=httpx.MockTransport(handler),
        )

        with self.assertRaises(FhirNotFoundError):
            await client.get_patient("missing")

    async def test_search_patient_resources_uses_safe_count_and_patient_ref(self) -> None:
        def handler(request: httpx.Request) -> httpx.Response:
            self.assertEqual(request.url.params["patient"], "Patient/demo-patient-001")
            self.assertEqual(request.url.params["_count"], "5")
            self.assertEqual(request.url.params["_sort"], "-date")
            return httpx.Response(200, json={"resourceType": "Bundle", "entry": []})

        client = FhirClient(
            base_url="http://fhir.test",
            transport=httpx.MockTransport(handler),
        )

        result = await client.search_patient_resources(
            "Observation",
            "demo-patient-001",
            count=5,
            sort="-date",
        )

        self.assertEqual(result["resourceType"], "Bundle")

    async def test_search_patients_uses_count(self) -> None:
        def handler(request: httpx.Request) -> httpx.Response:
            self.assertEqual(str(request.url.copy_with(query=None)), "http://fhir.test/Patient")
            self.assertEqual(request.url.params["_count"], "20")
            return httpx.Response(200, json={"resourceType": "Bundle", "entry": []})

        client = FhirClient(
            base_url="http://fhir.test",
            transport=httpx.MockTransport(handler),
        )

        result = await client.search_patients(count=20)

        self.assertEqual(result["resourceType"], "Bundle")


if __name__ == "__main__":
    unittest.main()
