package com.medicalchatbot.backend.dto.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @JsonProperty("session_id")
        UUID sessionId,

        @JsonProperty("patient_id")
        String patientId,

        @NotBlank
        String message
) {
}
