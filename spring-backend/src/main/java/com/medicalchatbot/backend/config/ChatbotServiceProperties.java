package com.medicalchatbot.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "chatbot.service")
public record ChatbotServiceProperties(
        @NotBlank String baseUrl
) {
}
