package com.medicalchatbot.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatMessageItem(
        UUID id,

        String role,

        String content,

        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
