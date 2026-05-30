package com.medicalchatbot.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalchatbot.backend.dto.ChatRequest;
import com.medicalchatbot.backend.dto.ChatResponse;
import com.medicalchatbot.backend.service.ChatApplicationService;
import com.medicalchatbot.backend.service.ChatbotServiceClient;
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
    void observationsRejectInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/patients/demo-patient-001/observations?limit=100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Dữ liệu yêu cầu không hợp lệ."));
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
                        "demo-patient-001",
                        null,
                        null,
                        objectMapper.readTree("[]"),
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
                .andExpect(jsonPath("$.patient_id").value("demo-patient-001"));
    }
}
