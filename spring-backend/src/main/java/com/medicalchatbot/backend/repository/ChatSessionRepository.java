package com.medicalchatbot.backend.repository;

import java.util.List;
import java.util.UUID;

import com.medicalchatbot.backend.dto.ChatContextMessage;
import com.medicalchatbot.backend.dto.ChatMessageItem;
import com.medicalchatbot.backend.dto.ChatSessionMemory;
import com.medicalchatbot.backend.dto.ChatSessionSummary;
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

    public ChatSessionMemory findMemoryForSession(UUID sessionId, UUID userId) {
        return jdbcTemplate.query(
                """
                select
                    active_patient_id,
                    memory_summary,
                    last_intent,
                    last_tool_name,
                    last_resource_type,
                    last_resource_id
                from chat_sessions
                where id = ? and user_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return ChatSessionMemory.empty();
                    }
                    return new ChatSessionMemory(
                            rs.getString("active_patient_id"),
                            rs.getString("memory_summary"),
                            rs.getString("last_intent"),
                            rs.getString("last_tool_name"),
                            rs.getString("last_resource_type"),
                            rs.getString("last_resource_id")
                    );
                },
                sessionId,
                userId
        );
    }

    public void updateMemory(UUID sessionId, ChatSessionMemory memory) {
        jdbcTemplate.update(
                """
                update chat_sessions
                set
                    active_patient_id = ?,
                    memory_summary = ?,
                    last_intent = ?,
                    last_tool_name = ?,
                    last_resource_type = ?,
                    last_resource_id = ?,
                    updated_at = now()
                where id = ?
                """,
                memory.activePatientId(),
                memory.memorySummary(),
                memory.lastIntent(),
                memory.lastToolName(),
                memory.lastResourceType(),
                memory.lastResourceId(),
                sessionId
        );
    }

    public List<ChatContextMessage> findRecentMessagesForContext(UUID sessionId, UUID userId, int limit) {
        return jdbcTemplate.query(
                """
                select role, content
                from (
                    select
                        m.role,
                        m.content,
                        m.created_at
                    from chat_messages m
                    join chat_sessions s on s.id = m.session_id
                    where s.id = ? and s.user_id = ?
                    order by m.created_at desc
                    limit ?
                ) recent
                order by created_at asc
                """,
                (rs, rowNum) -> new ChatContextMessage(
                        rs.getString("role"),
                        rs.getString("content")
                ),
                sessionId,
                userId,
                limit
        );
    }

    public List<ChatSessionSummary> findRecentSessionsForUser(UUID userId, int limit) {
        return jdbcTemplate.query(
                """
                select
                    s.id,
                    s.title,
                    s.created_at,
                    s.updated_at,
                    s.active_patient_id,
                    coalesce(message_counts.message_count, 0) as message_count,
                    left(coalesce(last_message.content, ''), 160) as last_message_preview
                from chat_sessions s
                left join lateral (
                    select count(*)::int as message_count
                    from chat_messages m
                    where m.session_id = s.id
                ) message_counts on true
                left join lateral (
                    select m.content
                    from chat_messages m
                    where m.session_id = s.id
                    order by m.created_at desc
                    limit 1
                ) last_message on true
                where s.user_id = ?
                order by s.updated_at desc
                limit ?
                """,
                (rs, rowNum) -> new ChatSessionSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("title"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class),
                        rs.getObject("updated_at", java.time.OffsetDateTime.class),
                        rs.getString("active_patient_id"),
                        rs.getInt("message_count"),
                        rs.getString("last_message_preview")
                ),
                userId,
                limit
        );
    }

    public List<ChatMessageItem> findMessagesForSession(UUID sessionId, UUID userId) {
        return jdbcTemplate.query(
                """
                select
                    m.id,
                    m.role,
                    m.content,
                    m.created_at
                from chat_messages m
                join chat_sessions s on s.id = m.session_id
                where s.id = ? and s.user_id = ?
                order by m.created_at asc
                """,
                (rs, rowNum) -> new ChatMessageItem(
                        rs.getObject("id", UUID.class),
                        rs.getString("role"),
                        rs.getString("content"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)
                ),
                sessionId,
                userId
        );
    }
}
