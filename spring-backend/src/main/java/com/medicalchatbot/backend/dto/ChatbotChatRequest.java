package com.medicalchatbot.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatbotChatRequest(
        @JsonProperty("user_id")
        String userId,

        @JsonProperty("session_id")
        String sessionId,

        String message,

        @JsonProperty("patient_id")
        String patientId,

        @JsonProperty("conversation_context")
        ConversationContext conversationContext
) {
}
