package com.medicalchatbot.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalchatbot.backend.dto.ChatRequest;
import com.medicalchatbot.backend.dto.ChatResponse;
import com.medicalchatbot.backend.service.ChatApplicationService;
import com.medicalchatbot.backend.service.ChatbotServiceClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class ChatbotController {

    private final ChatbotServiceClient chatbotServiceClient;
    private final ChatApplicationService chatApplicationService;

    public ChatbotController(
            ChatbotServiceClient chatbotServiceClient,
            ChatApplicationService chatApplicationService
    ) {
        this.chatbotServiceClient = chatbotServiceClient;
        this.chatApplicationService = chatApplicationService;
    }

    @GetMapping("/chatbot/status")
    JsonNode chatbotStatus() {
        return chatbotServiceClient.getStatus();
    }

    @GetMapping("/patients")
    JsonNode searchPatients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(name = "birth_date", required = false) String birthDate,
            @RequestParam(required = false) String identifier,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return chatbotServiceClient.searchPatients(name, phone, birthDate, identifier, limit);
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

    @GetMapping("/patients/{patientId}/encounters")
    JsonNode encounters(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return chatbotServiceClient.getPatientEncounters(patientId, limit);
    }

    @GetMapping("/patients/{patientId}/medications")
    JsonNode medications(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return chatbotServiceClient.getPatientMedications(patientId, limit);
    }

    @PostMapping("/chat")
    ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatApplicationService.chat(request);
    }
}
