package com.medicalchatbot.backend.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID create(UUID userId, String title) {
        return jdbcTemplate.queryForObject(
                "insert into chat_sessions (user_id, title) values (?, ?) returning id",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                userId,
                title
        );
    }

    public boolean existsForUser(UUID sessionId, UUID userId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists(select 1 from chat_sessions where id = ? and user_id = ?)",
                Boolean.class,
                sessionId,
                userId
        );
        return Boolean.TRUE.equals(exists);
    }

    public void touch(UUID sessionId) {
        jdbcTemplate.update(
                "update chat_sessions set updated_at = now() where id = ?",
                sessionId
        );
    }
}
