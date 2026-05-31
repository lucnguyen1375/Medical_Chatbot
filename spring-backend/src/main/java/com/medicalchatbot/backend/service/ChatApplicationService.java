package com.medicalchatbot.backend.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalchatbot.backend.dto.ChatRequest;
import com.medicalchatbot.backend.dto.ChatResponse;
import com.medicalchatbot.backend.dto.ChatbotChatRequest;
import com.medicalchatbot.backend.enums.ChatMessageRole;
import com.medicalchatbot.backend.repository.AppUserRepository;
import com.medicalchatbot.backend.repository.AuditLogRepository;
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
    private final AuditLogRepository auditLogRepository;
    private final ChatbotServiceClient chatbotServiceClient;
    private final ObjectMapper objectMapper;

    public ChatApplicationService(
            AppUserRepository appUserRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            UsageLogRepository usageLogRepository,
            AuditLogRepository auditLogRepository,
            ChatbotServiceClient chatbotServiceClient,
            ObjectMapper objectMapper
    ) {
        this.appUserRepository = appUserRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.usageLogRepository = usageLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.chatbotServiceClient = chatbotServiceClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        UUID userId = getDemoUserId();
        UUID sessionId = request.sessionId() == null
                ? chatSessionRepository.create(userId, titleFromMessage(request.message()))
                : requireSessionForUser(request.sessionId(), userId);

        chatMessageRepository.save(sessionId, ChatMessageRole.USER, request.message());

        long startedAtNanos = System.nanoTime();
        JsonNode chatbotResponse = chatbotServiceClient.chat(new ChatbotChatRequest(
                userId.toString(),
                sessionId.toString(),
                request.message(),
                request.patientId()
        ));
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);

        String answer = chatbotResponse.path("answer").asText("");
        chatMessageRepository.save(sessionId, ChatMessageRole.ASSISTANT, answer);
        saveUsage(userId, sessionId, chatbotResponse, latencyMs);
        saveAuditLog(userId, sessionId, request, chatbotResponse, latencyMs);

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

    private void saveUsage(UUID userId, UUID sessionId, JsonNode chatbotResponse, long latencyMs) {
        JsonNode usage = chatbotResponse.path("usage");
        usageLogRepository.save(
                userId,
                sessionId,
                textOrNull(chatbotResponse, "llm_provider"),
                textOrNull(chatbotResponse, "llm_model"),
                "chat",
                "success",
                latencyMs,
                usage.path("input_tokens").asInt(0),
                usage.path("output_tokens").asInt(0),
                BigDecimal.valueOf(usage.path("estimated_cost_usd").asDouble(0)),
                null
        );
    }

    private void saveAuditLog(
            UUID userId,
            UUID sessionId,
            ChatRequest request,
            JsonNode chatbotResponse,
            long latencyMs
    ) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("operation", "chat");
        metadata.put("latency_ms", latencyMs);
        metadata.put("intent", textOrNull(chatbotResponse, "intent"));
        metadata.put("tool_name", textOrNull(chatbotResponse, "tool_name"));
        metadata.put("intent_source", textOrNull(chatbotResponse, "intent_source"));
        metadata.put("answer_source", textOrNull(chatbotResponse, "answer_source"));
        metadata.put("patient_id", textOrNull(chatbotResponse, "patient_id"));
        metadata.put("request_patient_id", request.patientId());
        metadata.put("llm_provider", textOrNull(chatbotResponse, "llm_provider"));
        metadata.put("llm_model", textOrNull(chatbotResponse, "llm_model"));
        metadata.set("usage", chatbotResponse.path("usage"));
        if (chatbotResponse.has("needs_patient_selection")) {
            metadata.put("needs_patient_selection", chatbotResponse.path("needs_patient_selection").asBoolean(false));
        }

        auditLogRepository.save(
                userId,
                sessionId,
                "CHAT_COMPLETED",
                "chat_session",
                sessionId.toString(),
                metadata.toString()
        );
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        if (text == null || text.isBlank()) {
            return null;
        }
        return text;
    }

    private String titleFromMessage(String message) {
        String normalized = message == null ? "Cuộc trò chuyện mới" : message.strip();
        if (normalized.isEmpty()) {
            return "Cuộc trò chuyện mới";
        }
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }
}
