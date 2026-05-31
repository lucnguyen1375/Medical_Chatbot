package com.medicalchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalchatbot.backend.dto.ChatContextMessage;
import com.medicalchatbot.backend.dto.ChatRequest;
import com.medicalchatbot.backend.dto.ChatResponse;
import com.medicalchatbot.backend.dto.ChatSessionMemory;
import com.medicalchatbot.backend.dto.ChatbotChatRequest;
import com.medicalchatbot.backend.repository.AppUserRepository;
import com.medicalchatbot.backend.repository.AuditLogRepository;
import com.medicalchatbot.backend.repository.ChatMessageRepository;
import com.medicalchatbot.backend.repository.ChatSessionRepository;
import com.medicalchatbot.backend.repository.UsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ChatApplicationServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ChatbotServiceClient chatbotServiceClient;

    @Test
    void sessionMessagesRejectsSessionOutsideDemoUser() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000601");
        ChatApplicationService service = newService();

        when(appUserRepository.findIdByUsername("demo_user")).thenReturn(Optional.of(userId));
        when(chatSessionRepository.existsForUser(sessionId, userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.sessionMessages(sessionId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(chatSessionRepository, never()).findMessagesForSession(sessionId, userId);
    }

    @Test
    void chatUsesSessionMemoryWhenRequestHasNoPatientId() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000601");
        ChatApplicationService service = newService();
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "answer": "Theo du lieu FHIR...",
                  "intent": "medications",
                  "tool_name": "get_medication_requests",
                  "patient_id": "demo-patient-001",
                  "evidence": [
                    {
                      "resource_type": "MedicationRequest",
                      "id": "med-1",
                      "summary": "Amlodipine"
                    }
                  ],
                  "memory_update": {
                    "active_patient_id": "demo-patient-001",
                    "last_intent": "medications",
                    "last_tool_name": "get_medication_requests",
                    "last_resource_type": "MedicationRequest",
                    "last_resource_id": "med-1",
                    "summary": "Da xem thuoc cua demo-patient-001.",
                    "evidence_refs": [
                      {
                        "resource_type": "MedicationRequest",
                        "resource_id": "med-1",
                        "summary": "Amlodipine"
                      }
                    ]
                  },
                  "usage": {
                    "input_tokens": 0,
                    "output_tokens": 0,
                    "estimated_cost_usd": 0
                  }
                }
                """);

        when(appUserRepository.findIdByUsername("demo_user")).thenReturn(Optional.of(userId));
        when(chatSessionRepository.existsForUser(sessionId, userId)).thenReturn(true);
        when(chatSessionRepository.findMemoryForSession(sessionId, userId)).thenReturn(new ChatSessionMemory(
                "demo-patient-001",
                "Da xem huyet ap cua demo-patient-001.",
                "observations",
                "get_observations",
                "Observation",
                "obs-1"
        ));
        when(chatSessionRepository.findRecentMessagesForContext(sessionId, userId, 6)).thenReturn(List.of(
                new ChatContextMessage("user", "huyet ap cua benh nhan nay"),
                new ChatContextMessage("assistant", "Huyet ap 150/92 mmHg")
        ));
        when(chatbotServiceClient.chat(any(ChatbotChatRequest.class))).thenReturn(response);

        ChatResponse result = service.chat(new ChatRequest(
                sessionId,
                null,
                "benh nhan do dang dung thuoc gi?"
        ));

        ArgumentCaptor<ChatbotChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatbotChatRequest.class);
        verify(chatbotServiceClient).chat(requestCaptor.capture());
        ChatbotChatRequest chatbotRequest = requestCaptor.getValue();

        assertEquals("demo-patient-001", chatbotRequest.patientId());
        assertEquals("demo-patient-001", chatbotRequest.conversationContext().activePatientId());
        assertEquals("Observation", chatbotRequest.conversationContext().lastResourceType());
        assertEquals(2, chatbotRequest.conversationContext().recentMessages().size());
        assertEquals("demo-patient-001", result.patientId());

        ArgumentCaptor<ChatSessionMemory> memoryCaptor = ArgumentCaptor.forClass(ChatSessionMemory.class);
        verify(chatSessionRepository).updateMemory(org.mockito.ArgumentMatchers.eq(sessionId), memoryCaptor.capture());
        ChatSessionMemory savedMemory = memoryCaptor.getValue();
        assertEquals("demo-patient-001", savedMemory.activePatientId());
        assertEquals("medications", savedMemory.lastIntent());
        assertEquals("MedicationRequest", savedMemory.lastResourceType());
        assertEquals("med-1", savedMemory.lastResourceId());
    }

    @Test
    void allPatientResponseDoesNotOverwriteActivePatientContext() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000602");
        ChatApplicationService service = newService();
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "answer": "Danh sach benh nhan...",
                  "intent": "patients",
                  "tool_name": "search_patients",
                  "all_patients": true,
                  "patient_id": null,
                  "evidence": [],
                  "memory_update": {
                    "last_intent": "patients",
                    "last_tool_name": "search_patients",
                    "summary": "Da xem danh sach benh nhan."
                  },
                  "usage": {
                    "input_tokens": 0,
                    "output_tokens": 0,
                    "estimated_cost_usd": 0
                  }
                }
                """);

        when(appUserRepository.findIdByUsername("demo_user")).thenReturn(Optional.of(userId));
        when(chatSessionRepository.existsForUser(sessionId, userId)).thenReturn(true);
        when(chatSessionRepository.findMemoryForSession(sessionId, userId)).thenReturn(new ChatSessionMemory(
                "demo-patient-001",
                "Da xem huyet ap cua demo-patient-001.",
                "observations",
                "get_observations",
                "Observation",
                "obs-1"
        ));
        when(chatSessionRepository.findRecentMessagesForContext(sessionId, userId, 6)).thenReturn(List.of());
        when(chatbotServiceClient.chat(any(ChatbotChatRequest.class))).thenReturn(response);

        service.chat(new ChatRequest(sessionId, null, "danh sach benh nhan"));

        ArgumentCaptor<ChatSessionMemory> memoryCaptor = ArgumentCaptor.forClass(ChatSessionMemory.class);
        verify(chatSessionRepository).updateMemory(org.mockito.ArgumentMatchers.eq(sessionId), memoryCaptor.capture());
        ChatSessionMemory savedMemory = memoryCaptor.getValue();
        assertEquals("demo-patient-001", savedMemory.activePatientId());
        assertEquals("Observation", savedMemory.lastResourceType());
        assertEquals("obs-1", savedMemory.lastResourceId());
    }

    private ChatApplicationService newService() {
        return new ChatApplicationService(
                appUserRepository,
                chatSessionRepository,
                chatMessageRepository,
                usageLogRepository,
                auditLogRepository,
                chatbotServiceClient,
                new ObjectMapper()
        );
    }
}
