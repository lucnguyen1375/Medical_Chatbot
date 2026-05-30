package com.medicalchatbot.backend.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record ChatResponse(
        @JsonProperty("session_id")
        UUID sessionId,

        String answer,

        String intent,

        @JsonProperty("patient_id")
        String patientId,

        JsonNode evidence,

        JsonNode usage
) {
}
