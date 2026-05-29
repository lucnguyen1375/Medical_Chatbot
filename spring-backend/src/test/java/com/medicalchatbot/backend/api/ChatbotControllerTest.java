package com.medicalchatbot.backend.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalchatbot.backend.client.ChatbotServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
                .andExpect(jsonPath("$.detail").value("Request validation failed."));
    }
}
