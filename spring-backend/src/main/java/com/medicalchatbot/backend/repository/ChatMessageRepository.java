package com.medicalchatbot.backend.repository;

import java.util.UUID;

import com.medicalchatbot.backend.enums.ChatMessageRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ChatSessionRepository chatSessionRepository;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate, ChatSessionRepository chatSessionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatSessionRepository = chatSessionRepository;
    }

    public void save(UUID sessionId, ChatMessageRole role, String content) {
        jdbcTemplate.update(
                "insert into chat_messages (session_id, role, content) values (?, ?, ?)",
                sessionId,
                role.databaseValue(),
                content
        );
        chatSessionRepository.touch(sessionId);
    }
}
