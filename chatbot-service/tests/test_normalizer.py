import unittest

from fhir.normalizer import (
    normalize_condition,
    normalize_encounter,
    normalize_medication_request,
    normalize_observation_bundle,
    normalize_patient,
    normalize_patient_bundle,
)


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
        self.assertEqual(result["name"], "Nguyen Van A")
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
                        "category": [{"text": "Laboratory"}],
                        "code": {"text": "Blood glucose"},
                        "subject": {"reference": "Patient/demo-patient-001"},
                        "encounter": {"reference": "Encounter/demo-encounter-001"},
                        "effectiveDateTime": "2026-05-20T09:10:00+07:00",
                        "valueQuantity": {
                            "value": 145,
                            "unit": "mg/dL",
                            "system": "http://unitsofmeasure.org",
                            "code": "mg/dL",
                        },
                        "interpretation": [{"text": "High"}],
                        "referenceRange": [
                            {
                                "low": {"value": 70, "unit": "mg/dL"},
                                "high": {"value": 99, "unit": "mg/dL"},
                                "text": "Fasting reference range",
                            }
                        ],
                    }
                }
            ],
        }

        result = normalize_observation_bundle(bundle)

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["code"], "Blood glucose")
        self.assertEqual(result[0]["value"]["value"], 145)
        self.assertEqual(result[0]["subject"], "Patient/demo-patient-001")
        self.assertEqual(result[0]["encounter"], "Encounter/demo-encounter-001")
        self.assertEqual(result[0]["interpretation"][0]["text"], "High")
        self.assertEqual(result[0]["reference_range"][0]["high"]["value"], 99)

    def test_normalize_patient_bundle(self) -> None:
        bundle = {
            "resourceType": "Bundle",
            "entry": [
                {
                    "resource": {
                        "resourceType": "Patient",
                        "id": "demo-patient-001",
                        "name": [{"family": "Nguyen", "given": ["Van", "A"]}],
                    }
                },
                {
                    "resource": {
                        "resourceType": "Observation",
                        "id": "ignored",
                    }
                },
            ],
        }

        result = normalize_patient_bundle(bundle)

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["id"], "demo-patient-001")

    def test_normalize_patient_keeps_contact_address_and_email(self) -> None:
        patient = {
            "resourceType": "Patient",
            "id": "demo-patient-005",
            "active": True,
            "name": [{"family": "Hoang", "given": ["Anh", "E"]}],
            "telecom": [
                {"system": "phone", "value": "0900000005", "use": "mobile"},
                {"system": "email", "value": "demo.patient.005@example.vn"},
            ],
            "address": [{"line": ["12 Nguyen Trai"], "city": "Ha Noi", "country": "VN"}],
            "contact": [
                {
                    "relationship": [{"text": "Next-of-Kin"}],
                    "name": {"family": "Hoang", "given": ["Minh"]},
                    "telecom": [{"system": "phone", "value": "0911000005"}],
                }
            ],
        }

        result = normalize_patient(patient)

        self.assertTrue(result["active"])
        self.assertEqual(result["email"], "demo.patient.005@example.vn")
        self.assertEqual(result["address"][0]["city"], "Ha Noi")
        self.assertEqual(result["contact"][0]["name"]["text"], "Hoang Minh")

    def test_normalize_condition_keeps_detailed_fields(self) -> None:
        condition = {
            "resourceType": "Condition",
            "id": "demo-condition-006",
            "clinicalStatus": {"text": "Active"},
            "verificationStatus": {"text": "Confirmed"},
            "category": [{"text": "Problem List Item"}],
            "severity": {"text": "Mild"},
            "code": {"text": "Asthma"},
            "subject": {"reference": "Patient/demo-patient-005"},
            "encounter": {"reference": "Encounter/demo-encounter-006"},
            "onsetDateTime": "2024-03-10",
            "recordedDate": "2026-05-29",
            "asserter": {"display": "Dr. Demo"},
            "note": [{"text": "Demo chronic condition."}],
        }

        result = normalize_condition(condition)

        self.assertEqual(result["code"], "Asthma")
        self.assertEqual(result["severity"]["text"], "Mild")
        self.assertEqual(result["encounter"], "Encounter/demo-encounter-006")
        self.assertEqual(result["note"], ["Demo chronic condition."])

    def test_normalize_medication_request_keeps_dosage_and_dispense_request(self) -> None:
        medication = {
            "resourceType": "MedicationRequest",
            "id": "demo-medication-006",
            "status": "active",
            "intent": "order",
            "priority": "routine",
            "medicationCodeableConcept": {"text": "Salbutamol inhaler"},
            "subject": {"reference": "Patient/demo-patient-005"},
            "encounter": {"reference": "Encounter/demo-encounter-006"},
            "authoredOn": "2026-05-29",
            "requester": {"display": "Dr. Demo"},
            "reasonCode": [{"text": "Asthma symptom relief"}],
            "dosageInstruction": [
                {
                    "sequence": 1,
                    "text": "Inhale 1-2 puffs when needed.",
                    "route": {"text": "Inhalation"},
                    "doseAndRate": [{"doseQuantity": {"value": 2, "unit": "puff"}}],
                }
            ],
            "dispenseRequest": {
                "quantity": {"value": 1, "unit": "inhaler"},
                "expectedSupplyDuration": {"value": 30, "unit": "days"},
            },
        }

        result = normalize_medication_request(medication)

        self.assertEqual(result["medication"], "Salbutamol inhaler")
        self.assertEqual(result["dosage"][0], "Inhale 1-2 puffs when needed.")
        self.assertEqual(result["dosage_instruction"][0]["dose_and_rate"][0]["dose_quantity"]["value"], 2)
        self.assertEqual(result["dispense_request"]["expected_supply_duration"]["value"], 30)

    def test_normalize_encounter_keeps_period_participant_reason_and_location(self) -> None:
        encounter = {
            "resourceType": "Encounter",
            "id": "demo-encounter-006",
            "status": "finished",
            "class": {"code": "AMB", "display": "ambulatory"},
            "type": [{"text": "Asthma follow-up visit"}],
            "subject": {"reference": "Patient/demo-patient-005"},
            "participant": [{"individual": {"display": "Dr. Demo"}}],
            "period": {"start": "2026-05-29T08:00:00+07:00"},
            "reasonCode": [{"text": "Shortness of breath follow-up"}],
            "location": [{"location": {"display": "Outpatient room 2"}, "status": "completed"}],
        }

        result = normalize_encounter(encounter)

        self.assertEqual(result["class"]["code"], "AMB")
        self.assertEqual(result["type"][0]["text"], "Asthma follow-up visit")
        self.assertEqual(result["participant"][0]["individual"]["display"], "Dr. Demo")
        self.assertEqual(result["location"][0]["location"]["display"], "Outpatient room 2")


if __name__ == "__main__":
    unittest.main()
