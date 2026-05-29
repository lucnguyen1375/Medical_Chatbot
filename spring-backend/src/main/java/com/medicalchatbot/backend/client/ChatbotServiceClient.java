package com.medicalchatbot.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ChatbotServiceClient {

    private final RestClient chatbotRestClient;

    public ChatbotServiceClient(RestClient chatbotRestClient) {
        this.chatbotRestClient = chatbotRestClient;
    }

    public JsonNode getStatus() {
        return chatbotRestClient.get()
                .uri("/fhir/status")
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getPatient(String patientId) {
        return chatbotRestClient.get()
                .uri("/patients/{patientId}", patientId)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getPatientObservations(String patientId, int limit) {
        return chatbotRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/patients/{patientId}/observations")
                        .queryParam("limit", limit)
                        .build(patientId))
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getPatientConditions(String patientId, int limit) {
        return chatbotRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/patients/{patientId}/conditions")
                        .queryParam("limit", limit)
                        .build(patientId))
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getPatientMedications(String patientId, int limit) {
        return chatbotRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/patients/{patientId}/medications")
                        .queryParam("limit", limit)
                        .build(patientId))
                .retrieve()
                .body(JsonNode.class);
    }
}
