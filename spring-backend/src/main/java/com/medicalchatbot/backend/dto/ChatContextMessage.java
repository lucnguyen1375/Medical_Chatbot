package com.medicalchatbot.backend.dto;

public record ChatContextMessage(
        String role,

        String content
) {
}
