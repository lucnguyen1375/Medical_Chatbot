package com.medicalchatbot.backend;

import com.medicalchatbot.backend.repository.AppUserRepository;
import com.medicalchatbot.backend.repository.AuditLogRepository;
import com.medicalchatbot.backend.repository.ChatMessageRepository;
import com.medicalchatbot.backend.repository.ChatSessionRepository;
import com.medicalchatbot.backend.repository.UsageLogRepository;
import com.medicalchatbot.backend.service.ChatbotServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.flyway.enabled=false")
class SpringBackendApplicationTests {

	@MockitoBean
	private AppUserRepository appUserRepository;

	@MockitoBean
	private ChatSessionRepository chatSessionRepository;

	@MockitoBean
	private ChatMessageRepository chatMessageRepository;

	@MockitoBean
	private UsageLogRepository usageLogRepository;

	@MockitoBean
	private AuditLogRepository auditLogRepository;

	@MockitoBean
	private ChatbotServiceClient chatbotServiceClient;

	@Test
	void contextLoads() {
	}

}
