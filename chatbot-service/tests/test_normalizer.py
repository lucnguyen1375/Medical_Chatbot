import unittest

from fhir.normalizer import normalize_observation_bundle, normalize_patient


class NormalizerTests(unittest.TestCase):
    def test_normalize_patient(self) -> None:
        patient = {
            "resourceType": "Patient",
            "id": "demo-patient-001",
            "identifier": [{"system": "system", "value": "DEMO-001"}],
            "name": [{"family": "Nguyen", "given": ["Van", "A"]}],
            "gender": "male",
            "birthDate": "2003-01-01",
            "telecom": [{"system": "phone", "value": "0900000001"}],
        }

        result = normalize_patient(patient)

        self.assertEqual(result["id"], "demo-patient-001")
        self.assertEqual(result["name"], "Van A Nguyen")
        self.assertEqual(result["gender"], "male")
        self.assertEqual(result["phone"], "0900000001")

    def test_normalize_observation_bundle(self) -> None:
        bundle = {
            "resourceType": "Bundle",
            "entry": [
                {
                    "resource": {
                        "resourceType": "Observation",
                        "id": "glucose",
                        "status": "final",
                        "code": {"text": "Blood glucose"},
                        "valueQuantity": {
                            "value": 145,
                            "unit": "mg/dL",
                            "system": "http://unitsofmeasure.org",
                            "code": "mg/dL",
                        },
                    }
                }
            ],
        }

        result = normalize_observation_bundle(bundle)

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["code"], "Blood glucose")
        self.assertEqual(result[0]["value"]["value"], 145)


if __name__ == "__main__":
    unittest.main()
