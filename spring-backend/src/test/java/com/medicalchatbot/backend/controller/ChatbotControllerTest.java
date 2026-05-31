package com.medicalchatbot.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalchatbot.backend.dto.ChatMessageItem;
import com.medicalchatbot.backend.dto.ChatMessagesResponse;
import com.medicalchatbot.backend.dto.ChatRequest;
import com.medicalchatbot.backend.dto.ChatResponse;
import com.medicalchatbot.backend.dto.ChatSessionListResponse;
import com.medicalchatbot.backend.dto.ChatSessionSummary;
import com.medicalchatbot.backend.service.ChatApplicationService;
import com.medicalchatbot.backend.service.ChatbotServiceClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatbotController.class)
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatbotServiceClient chatbotServiceClient;

    @MockitoBean
    private ChatApplicationService chatApplicationService;

    @Test
    void patientReturnsChatbotServicePayload() throws Exception {
        when(chatbotServiceClient.getPatient("demo-patient-001"))
                .thenReturn(objectMapper.readTree("""
                        {
                          "id": "demo-patient-001",
                          "name": "Van A Nguyen"
                        }
                        """));

        mockMvc.perform(get("/api/patients/demo-patient-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("demo-patient-001"))
                .andExpect(jsonPath("$.name").value("Van A Nguyen"));
    }

    @Test
    void searchPatientsReturnsChatbotServicePayload() throws Exception {
        when(chatbotServiceClient.searchPatients("Nguyen Van A", null, "2003-01-01", null, 20))
                .thenReturn(objectMapper.readTree("""
                        {
                          "criteria": {
                            "name": "Nguyen Van A",
                            "birth_date": "2003-01-01"
                          },
                          "patients": [
                            {
                              "id": "demo-patient-001",
                              "name": "Van A Nguyen"
                            }
                          ]
                        }
                        """));

        mockMvc.perform(get("/api/patients?name=Nguyen Van A&birth_date=2003-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patients[0].id").value("demo-patient-001"))
                .andExpect(jsonPath("$.criteria.name").value("Nguyen Van A"));
    }

    @Test
    void observationsRejectInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/patients/demo-patient-001/observations?limit=100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Dữ liệu yêu cầu không hợp lệ."));
    }

    @Test
    void encountersReturnChatbotServicePayload() throws Exception {
        when(chatbotServiceClient.getPatientEncounters("demo-patient-005", 5))
                .thenReturn(objectMapper.readTree("""
                        {
                          "patient_id": "demo-patient-005",
                          "encounters": [
                            {
                              "id": "demo-encounter-006",
                              "status": "finished"
                            }
                          ]
                        }
                        """));

        mockMvc.perform(get("/api/patients/demo-patient-005/encounters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient_id").value("demo-patient-005"))
                .andExpect(jsonPath("$.encounters[0].id").value("demo-encounter-006"));
    }

    @Test
    void chatReturnsPersistedSessionResponse() throws Exception {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        when(chatApplicationService.chat(new ChatRequest(null, "demo-patient-001", "Bệnh nhân 001 đang dùng thuốc gì?")))
                .thenReturn(new ChatResponse(
                        sessionId,
                        "Theo dữ liệu FHIR hiện có...",
                        "medications",
                        "get_medication_requests",
                        "llm",
                        "llm",
                        null,
                        "demo-patient-001",
                        null,
                        null,
                        objectMapper.readTree("null"),
                        null,
                        objectMapper.readTree("[]"),
                        null,
                        objectMapper.readTree("[]"),
                        objectMapper.readTree("""
                                {
                                  "input_tokens": 0,
                                  "output_tokens": 0,
                                  "estimated_cost_usd": 0
                                }
                                """),
                        objectMapper.readTree("""
                                {
                                  "input_tokens": 0,
                                  "output_tokens": 0,
                                  "estimated_cost_usd": 0
                                }
                                """)
                ));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patient_id": "demo-patient-001",
                                  "message": "Bệnh nhân 001 đang dùng thuốc gì?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value(sessionId.toString()))
                .andExpect(jsonPath("$.intent").value("medications"))
                .andExpect(jsonPath("$.tool_name").value("get_medication_requests"))
                .andExpect(jsonPath("$.intent_source").value("llm"))
                .andExpect(jsonPath("$.answer_source").value("llm"))
                .andExpect(jsonPath("$.patient_id").value("demo-patient-001"));
    }

    @Test
    void chatSessionsReturnRecentSessions() throws Exception {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        when(chatApplicationService.recentSessions(20))
                .thenReturn(new ChatSessionListResponse(List.of(new ChatSessionSummary(
                        sessionId,
                        "Thuốc của bệnh nhân 001",
                        OffsetDateTime.parse("2026-05-31T10:00:00Z"),
                        OffsetDateTime.parse("2026-05-31T10:05:00Z"),
                        "demo-patient-001",
                        2,
                        "Theo dữ liệu FHIR..."
                ))));

        mockMvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$.sessions[0].title").value("Thuốc của bệnh nhân 001"))
                .andExpect(jsonPath("$.sessions[0].active_patient_id").value("demo-patient-001"))
                .andExpect(jsonPath("$.sessions[0].message_count").value(2))
                .andExpect(jsonPath("$.sessions[0].last_message_preview").value("Theo dữ liệu FHIR..."));
    }

    @Test
    void chatSessionsRejectInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/chat/sessions?limit=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatSessionMessagesReturnMessages() throws Exception {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000402");
        UUID userMessageId = UUID.fromString("00000000-0000-0000-0000-000000000501");
        UUID assistantMessageId = UUID.fromString("00000000-0000-0000-0000-000000000502");
        when(chatApplicationService.sessionMessages(sessionId))
                .thenReturn(new ChatMessagesResponse(
                        sessionId,
                        List.of(
                                new ChatMessageItem(
                                        userMessageId,
                                        "user",
                                        "Bệnh nhân này dùng thuốc gì?",
                                        OffsetDateTime.parse("2026-05-31T10:00:00Z")
                                ),
                                new ChatMessageItem(
                                        assistantMessageId,
                                        "assistant",
                                        "Theo dữ liệu FHIR...",
                                        OffsetDateTime.parse("2026-05-31T10:00:05Z")
                                )
                        )
                ));

        mockMvc.perform(get("/api/chat/sessions/{sessionId}/messages", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value(sessionId.toString()))
                .andExpect(jsonPath("$.messages[0].id").value(userMessageId.toString()))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("Bệnh nhân này dùng thuốc gì?"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"));
    }
}
