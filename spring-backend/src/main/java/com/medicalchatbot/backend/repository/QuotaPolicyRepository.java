package com.medicalchatbot.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.medicalchatbot.backend.dto.QuotaPolicyInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuotaPolicyRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuotaPolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<QuotaPolicyInfo> findByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                select
                    q.name,
                    q.daily_request_limit,
                    q.daily_token_limit,
                    q.daily_cost_limit_usd
                from app_users u
                join quota_policies q on q.id = u.quota_policy_id
                where u.id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new QuotaPolicyInfo(
                            rs.getString("name"),
                            rs.getInt("daily_request_limit"),
                            rs.getInt("daily_token_limit"),
                            rs.getBigDecimal("daily_cost_limit_usd")
                    ));
                },
                userId
        );
    }
}
