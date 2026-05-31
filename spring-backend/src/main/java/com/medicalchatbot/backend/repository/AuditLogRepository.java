package com.medicalchatbot.backend.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(
            UUID userId,
            UUID sessionId,
            String action,
            String resourceType,
            String resourceId,
            String metadataJson
    ) {
        jdbcTemplate.update(
                """
                insert into audit_logs (
                    user_id,
                    session_id,
                    action,
                    resource_type,
                    resource_id,
                    metadata_json
                )
                values (?, ?, ?, ?, ?, coalesce(cast(? as jsonb), '{}'::jsonb))
                """,
                userId,
                sessionId,
                action,
                resourceType,
                resourceId,
                metadataJson
        );
    }
}
