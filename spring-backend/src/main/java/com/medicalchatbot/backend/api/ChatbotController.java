package com.medicalchatbot.backend.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalchatbot.backend.client.ChatbotServiceClient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class ChatbotController {

    private final ChatbotServiceClient chatbotServiceClient;

    public ChatbotController(ChatbotServiceClient chatbotServiceClient) {
        this.chatbotServiceClient = chatbotServiceClient;
    }

    @GetMapping("/chatbot/status")
    JsonNode chatbotStatus() {
        return chatbotServiceClient.getStatus();
    }

    @GetMapping("/patients/{patientId}")
    JsonNode patient(@PathVariable String patientId) {
        return chatbotServiceClient.getPatient(patientId);
    }

    @GetMapping("/patients/{patientId}/observations")
    JsonNode observations(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return chatbotServiceClient.getPatientObservations(patientId, limit);
    }

    @GetMapping("/patients/{patientId}/conditions")
    JsonNode conditions(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return chatbotServiceClient.getPatientConditions(patientId, limit);
    }

    @GetMapping("/patients/{patientId}/medications")
    JsonNode medications(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return chatbotServiceClient.getPatientMedications(patientId, limit);
    }
}
