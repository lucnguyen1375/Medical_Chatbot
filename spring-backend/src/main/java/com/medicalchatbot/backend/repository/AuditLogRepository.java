package com.medicalchatbot.backend.repository;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalchatbot.backend.entity.AuditLog;
import com.medicalchatbot.backend.entity.ChatSession;
import com.medicalchatbot.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    default void save(
            User user,
            ChatSession session,
            String action,
            String resourceType,
            String resourceId,
            JsonNode metadataJson
    ) {
        save(new AuditLog(user, session, action, resourceType, resourceId, metadataJson));
    }
}
