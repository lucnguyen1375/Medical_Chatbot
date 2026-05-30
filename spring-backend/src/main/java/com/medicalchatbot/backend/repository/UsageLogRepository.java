package com.medicalchatbot.backend.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsageLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public UsageLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(
            UUID userId,
            UUID sessionId,
            int inputTokens,
            int outputTokens,
            BigDecimal estimatedCostUsd
    ) {
        jdbcTemplate.update(
                """
                insert into usage_logs (
                    user_id,
                    session_id,
                    request_count,
                    input_tokens,
                    output_tokens,
                    estimated_cost_usd
                )
                values (?, ?, 1, ?, ?, ?)
                """,
                userId,
                sessionId,
                inputTokens,
                outputTokens,
                estimatedCostUsd
        );
    }
}
