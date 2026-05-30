import unittest

from api.chat_routes import _detect_intent, _resolve_patient_id, ChatRequest


class ChatRoutesTests(unittest.TestCase):
    def test_detects_medication_intent(self) -> None:
        self.assertEqual(_detect_intent("Patient/demo-patient-001 has what medications?"), "medications")

    def test_detects_vietnamese_observation_intent_without_accents(self) -> None:
        self.assertEqual(_detect_intent("huyet ap cua demo-patient-001"), "observations")

    def test_resolves_patient_reference(self) -> None:
        request = ChatRequest(message="Show medications for Patient/demo-patient-001")

        self.assertEqual(_resolve_patient_id(request), "demo-patient-001")

    def test_defaults_demo_patient(self) -> None:
        request = ChatRequest(message="Show medications")

        self.assertEqual(_resolve_patient_id(request), "demo-patient-001")


if __name__ == "__main__":
    unittest.main()
