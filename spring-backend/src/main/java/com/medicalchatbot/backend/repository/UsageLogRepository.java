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
            String llmProvider,
            String llmModel,
            String operation,
            String status,
            long latencyMs,
            int inputTokens,
            int outputTokens,
            BigDecimal estimatedCostUsd,
            String errorMessage
    ) {
        jdbcTemplate.update(
                """
                insert into usage_logs (
                    user_id,
                    session_id,
                    llm_provider,
                    llm_model,
                    operation,
                    status,
                    latency_ms,
                    request_count,
                    input_tokens,
                    output_tokens,
                    estimated_cost_usd,
                    error_message
                )
                values (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                """,
                userId,
                sessionId,
                llmProvider,
                llmModel,
                operation,
                status,
                latencyMs,
                inputTokens,
                outputTokens,
                estimatedCostUsd,
                errorMessage
        );
    }
}
