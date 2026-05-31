package com.medicalchatbot.backend.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatMessagesResponse(
        @JsonProperty("session_id")
        UUID sessionId,

        List<ChatMessageItem> messages
) {
}
