package com.medicalchatbot.backend.service;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalchatbot.backend.dto.ChatRequest;
import com.medicalchatbot.backend.dto.ChatResponse;
import com.medicalchatbot.backend.dto.ChatbotChatRequest;
import com.medicalchatbot.backend.enums.ChatMessageRole;
import com.medicalchatbot.backend.repository.AppUserRepository;
import com.medicalchatbot.backend.repository.ChatMessageRepository;
import com.medicalchatbot.backend.repository.ChatSessionRepository;
import com.medicalchatbot.backend.repository.UsageLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatApplicationService {

    private static final String DEMO_USERNAME = "demo_user";

    private final AppUserRepository appUserRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UsageLogRepository usageLogRepository;
    private final ChatbotServiceClient chatbotServiceClient;

    public ChatApplicationService(
            AppUserRepository appUserRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            UsageLogRepository usageLogRepository,
            ChatbotServiceClient chatbotServiceClient
    ) {
        this.appUserRepository = appUserRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.usageLogRepository = usageLogRepository;
        this.chatbotServiceClient = chatbotServiceClient;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        UUID userId = getDemoUserId();
        UUID sessionId = request.sessionId() == null
                ? chatSessionRepository.create(userId, titleFromMessage(request.message()))
                : requireSessionForUser(request.sessionId(), userId);

        chatMessageRepository.save(sessionId, ChatMessageRole.USER, request.message());

        JsonNode chatbotResponse = chatbotServiceClient.chat(new ChatbotChatRequest(
                userId.toString(),
                sessionId.toString(),
                request.message(),
                request.patientId()
        ));

        String answer = chatbotResponse.path("answer").asText("");
        chatMessageRepository.save(sessionId, ChatMessageRole.ASSISTANT, answer);
        saveUsage(userId, sessionId, chatbotResponse.path("usage"));

        return new ChatResponse(
                sessionId,
                answer,
                chatbotResponse.path("intent").asText(null),
                chatbotResponse.path("tool_name").asText(null),
                chatbotResponse.path("intent_source").asText(null),
                chatbotResponse.path("answer_source").asText(null),
                chatbotResponse.path("answer_reason").asText(null),
                chatbotResponse.path("patient_id").asText(null),
                chatbotResponse.path("observation_type").asText(null),
                chatbotResponse.has("all_patients") ? chatbotResponse.path("all_patients").asBoolean(false) : null,
                chatbotResponse.path("patient_search"),
                chatbotResponse.has("needs_patient_selection")
                        ? chatbotResponse.path("needs_patient_selection").asBoolean(false)
                        : null,
                chatbotResponse.path("patient_candidates"),
                chatbotResponse.path("pending_question").asText(null),
                chatbotResponse.path("evidence"),
                chatbotResponse.path("answer_usage"),
                chatbotResponse.path("usage")
        );
    }

    private UUID getDemoUserId() {
        return appUserRepository.findIdByUsername(DEMO_USERNAME)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không tìm thấy người dùng demo."
                ));
    }

    private UUID requireSessionForUser(UUID sessionId, UUID userId) {
        if (!chatSessionRepository.existsForUser(sessionId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên trò chuyện.");
        }
        return sessionId;
    }

    private void saveUsage(UUID userId, UUID sessionId, JsonNode usage) {
        usageLogRepository.save(
                userId,
                sessionId,
                usage.path("input_tokens").asInt(0),
                usage.path("output_tokens").asInt(0),
                BigDecimal.valueOf(usage.path("estimated_cost_usd").asDouble(0))
        );
    }

    private String titleFromMessage(String message) {
        String normalized = message == null ? "Cuộc trò chuyện mới" : message.strip();
        if (normalized.isEmpty()) {
            return "Cuộc trò chuyện mới";
        }
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }
}
