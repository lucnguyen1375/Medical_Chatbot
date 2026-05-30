import unittest

from api.chat_routes import (
    _detect_intent,
    _finalize_chat_response,
    _observation_matches_type,
    _resolve_patient_id,
    ChatRequest,
)
from agents.answer_generator import AnswerResult
from agents.intent_extractor import (
    IntentPlan,
    TOOL_GET_CONDITIONS,
    TOOL_GET_MEDICATIONS,
    TOOL_GET_OBSERVATIONS,
    TOOL_GET_PATIENT,
    TOOL_SEARCH_PATIENTS,
    RuleBasedIntentExtractor,
    apply_all_patient_scope,
    add_observation_type_hint,
    apply_patient_id_hint,
    enforce_patient_list_routing,
    enforce_contact_detail_routing,
    plan_from_tool_call,
)


class FakeAnswerGenerator:
    async def generate(self, **kwargs):
        return AnswerResult(
            answer=f"LLM: {kwargs['fallback_answer']}",
            source="llm",
            usage={"input_tokens": 20, "output_tokens": 5, "estimated_cost_usd": 0.01},
        )


class ChatRoutesTests(unittest.TestCase):
    def test_detects_medication_intent(self) -> None:
        self.assertEqual(_detect_intent("Patient/demo-patient-001 has what medications?"), "medications")

    def test_detects_vietnamese_observation_intent_without_accents(self) -> None:
        self.assertEqual(_detect_intent("huyet ap cua demo-patient-001"), "observations")

    def test_detects_vietnamese_phone_as_patient_intent(self) -> None:
        self.assertEqual(_detect_intent("so dien thoai cua benh nhan 001"), "patient")

    def test_detects_patient_list_intent(self) -> None:
        self.assertEqual(_detect_intent("danh sach benh nhan hien co"), "patients")

    def test_resolves_patient_reference(self) -> None:
        request = ChatRequest(message="Show medications for Patient/demo-patient-001")

        self.assertEqual(_resolve_patient_id(request), "demo-patient-001")

    def test_resolves_numbered_demo_patient(self) -> None:
        request = ChatRequest(message="so dien thoai cua benh nhan 001")

        self.assertEqual(_resolve_patient_id(request), "demo-patient-001")

    def test_resolves_message_patient_before_request_patient(self) -> None:
        request = ChatRequest(
            message="so dien thoai cua benh nhan 004",
            patient_id="demo-patient-001",
        )

        self.assertEqual(_resolve_patient_id(request), "demo-patient-004")

    def test_matches_vietnamese_blood_pressure_observation_type(self) -> None:
        observation = {
            "code": "Blood pressure",
            "components": [
                {"code": "Systolic blood pressure"},
                {"code": "Diastolic blood pressure"},
            ],
        }

        self.assertTrue(_observation_matches_type(observation, "huyết áp"))

    def test_matches_vietnamese_heart_rate_observation_type(self) -> None:
        observation = {
            "code": "Heart rate",
            "components": [],
        }

        self.assertTrue(_observation_matches_type(observation, "nhịp tim"))

    def test_defaults_demo_patient(self) -> None:
        request = ChatRequest(message="Show medications")

        self.assertEqual(_resolve_patient_id(request), "demo-patient-001")


class IntentExtractorTests(unittest.IsolatedAsyncioTestCase):
    async def test_finalize_chat_response_adds_llm_answer_metadata_and_combined_usage(self) -> None:
        payload = {
            "answer": "Template answer",
            "intent": "observations",
            "patient_id": "demo-patient-003",
            "evidence": [{"resource_type": "Observation", "id": "obs-1", "summary": "HbA1c"}],
            "usage": {"input_tokens": 0, "output_tokens": 0, "estimated_cost_usd": 0},
        }
        plan = IntentPlan(
            tool_name=TOOL_GET_OBSERVATIONS,
            patient_id="demo-patient-003",
            source="llm",
            usage={"input_tokens": 10, "output_tokens": 3, "estimated_cost_usd": 0},
        )

        result = await _finalize_chat_response(
            payload,
            "hba1c cua benh nhan 003",
            plan,
            FakeAnswerGenerator(),
        )

        self.assertEqual(result["answer"], "LLM: Template answer")
        self.assertEqual(result["answer_source"], "llm")
        self.assertEqual(result["answer_usage"]["input_tokens"], 20)
        self.assertEqual(result["usage"]["input_tokens"], 30)
        self.assertEqual(result["tool_name"], TOOL_GET_OBSERVATIONS)

    async def test_rule_based_extractor_returns_fhir_tool_plan(self) -> None:
        plan = await RuleBasedIntentExtractor().extract(
            "What medications is Patient/demo-patient-001 taking?"
        )

        self.assertEqual(plan.tool_name, TOOL_GET_MEDICATIONS)
        self.assertEqual(plan.intent, "medications")
        self.assertEqual(plan.patient_id, "demo-patient-001")
        self.assertEqual(plan.source, "rules")

    async def test_rule_based_extractor_routes_phone_to_patient_tool(self) -> None:
        plan = await RuleBasedIntentExtractor().extract("so dien thoai cua benh nhan 001")

        self.assertEqual(plan.tool_name, TOOL_GET_PATIENT)
        self.assertEqual(plan.intent, "patient")
        self.assertEqual(plan.patient_id, "demo-patient-001")

    async def test_rule_based_extractor_routes_patient_list(self) -> None:
        plan = await RuleBasedIntentExtractor().extract("danh sach benh nhan hien co")

        self.assertEqual(plan.tool_name, TOOL_SEARCH_PATIENTS)
        self.assertEqual(plan.intent, "patients")
        self.assertEqual(plan.limit, 20)

    async def test_rule_based_extractor_routes_all_patient_medications(self) -> None:
        plan = await RuleBasedIntentExtractor().extract("tat ca benh nhan dang dung thuoc gi")

        self.assertEqual(plan.tool_name, TOOL_GET_MEDICATIONS)
        self.assertTrue(plan.all_patients)

    async def test_rule_based_extractor_adds_vietnamese_observation_type(self) -> None:
        plan = await RuleBasedIntentExtractor().extract("huyet ap cua benh nhan 001")

        self.assertEqual(plan.tool_name, TOOL_GET_OBSERVATIONS)
        self.assertEqual(plan.observation_type, "blood_pressure")

    async def test_rule_based_extractor_adds_vietnamese_heart_rate_type(self) -> None:
        plan = await RuleBasedIntentExtractor().extract("nhip tim cua benh nhan 001")

        self.assertEqual(plan.tool_name, TOOL_GET_OBSERVATIONS)
        self.assertEqual(plan.observation_type, "heart_rate")

    def test_guardrail_routes_phone_to_patient_tool(self) -> None:
        plan = IntentPlan(
            tool_name=TOOL_GET_CONDITIONS,
            patient_id="demo-patient-001",
            source="llm",
            usage={"input_tokens": 10, "output_tokens": 4, "estimated_cost_usd": 0},
        )

        routed = enforce_contact_detail_routing("so dien thoai cua benh nhan 001", plan)

        self.assertEqual(routed.tool_name, TOOL_GET_PATIENT)
        self.assertEqual(routed.intent, "patient")
        self.assertEqual(routed.source, "llm_guardrail")

    def test_guardrail_routes_patient_list_tool(self) -> None:
        plan = IntentPlan(tool_name=TOOL_GET_PATIENT, patient_id="demo-patient-001", source="llm")

        routed = enforce_patient_list_routing("liet ke tat ca benh nhan", plan)

        self.assertEqual(routed.tool_name, TOOL_SEARCH_PATIENTS)
        self.assertEqual(routed.intent, "patients")
        self.assertEqual(routed.source, "llm_guardrail")

    def test_all_patient_scope_changes_patient_list_to_medications(self) -> None:
        plan = IntentPlan(tool_name=TOOL_SEARCH_PATIENTS, patient_id="demo-patient-001", source="llm")

        routed = apply_all_patient_scope("tat ca benh nhan dang dung thuoc gi", plan)

        self.assertEqual(routed.tool_name, TOOL_GET_MEDICATIONS)
        self.assertTrue(routed.all_patients)

    def test_observation_type_hint_handles_vietnamese_blood_pressure(self) -> None:
        plan = IntentPlan(tool_name=TOOL_GET_OBSERVATIONS, patient_id="demo-patient-001", source="llm")

        routed = add_observation_type_hint("huyet ap cua benh nhan 001", plan)

        self.assertEqual(routed.observation_type, "blood_pressure")

    def test_patient_id_hint_overrides_llm_default_when_message_has_number(self) -> None:
        plan = IntentPlan(tool_name=TOOL_GET_PATIENT, patient_id="demo-patient-001", source="llm")

        routed = apply_patient_id_hint("so dien thoai cua benh nhan 004", None, plan)

        self.assertEqual(routed.patient_id, "demo-patient-004")

    def test_patient_id_hint_prefers_message_over_provided_field(self) -> None:
        plan = IntentPlan(tool_name=TOOL_GET_PATIENT, patient_id="demo-patient-001", source="llm")

        routed = apply_patient_id_hint("so dien thoai cua benh nhan 004", "demo-patient-001", plan)

        self.assertEqual(routed.patient_id, "demo-patient-004")

    def test_plan_from_tool_call_prefers_provided_patient_id(self) -> None:
        plan = plan_from_tool_call(
            tool_name=TOOL_GET_OBSERVATIONS,
            arguments={
                "patient_id": "demo-patient-from-llm",
                "observation_type": "glucose",
                "limit": 100,
            },
            provided_patient_id="demo-patient-001",
            usage={"input_tokens": 10, "output_tokens": 5, "estimated_cost_usd": 0},
            source="llm",
        )

        self.assertEqual(plan.tool_name, TOOL_GET_OBSERVATIONS)
        self.assertEqual(plan.patient_id, "demo-patient-001")
        self.assertEqual(plan.observation_type, "glucose")
        self.assertEqual(plan.limit, 20)
        self.assertEqual(plan.source, "llm")


if __name__ == "__main__":
    unittest.main()
