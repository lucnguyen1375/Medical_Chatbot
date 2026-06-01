package com.medicalchatbot.backend.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.medicalchatbot.backend.dto.CostByDay;
import com.medicalchatbot.backend.dto.CostByModel;
import com.medicalchatbot.backend.dto.CostSummaryResponse;
import com.medicalchatbot.backend.dto.MissingPricingModel;
import com.medicalchatbot.backend.dto.QuotaUsageSummary;
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
                estimatedCostUsd == null ? BigDecimal.ZERO : estimatedCostUsd,
                errorMessage
        );
    }

    public QuotaUsageSummary summarizeSuccessfulUsage(
            UUID userId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        return jdbcTemplate.queryForObject(
                """
                select
                    coalesce(sum(request_count), 0)::int as used_requests,
                    coalesce(sum(input_tokens), 0)::int as used_input_tokens,
                    coalesce(sum(output_tokens), 0)::int as used_output_tokens,
                    coalesce(sum(estimated_cost_usd), 0)::numeric(12, 6) as used_cost_usd
                from usage_logs
                where user_id = ?
                  and status = 'success'
                  and created_at >= ?
                  and created_at < ?
                """,
                (rs, rowNum) -> new QuotaUsageSummary(
                        rs.getInt("used_requests"),
                        rs.getInt("used_input_tokens"),
                        rs.getInt("used_output_tokens"),
                        rs.getBigDecimal("used_cost_usd")
                ),
                userId,
                startInclusive,
                endExclusive
        );
    }

    public CostSummaryResponse summarizeCost(
            UUID userId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        return jdbcTemplate.queryForObject(
                """
                select
                    coalesce(sum(request_count), 0)::int as request_count,
                    coalesce(sum(input_tokens), 0)::int as input_tokens,
                    coalesce(sum(output_tokens), 0)::int as output_tokens,
                    coalesce(sum(estimated_cost_usd), 0)::numeric(12, 6) as estimated_cost_usd
                from usage_logs
                where user_id = ?
                  and status = 'success'
                  and created_at >= ?
                  and created_at < ?
                """,
                (rs, rowNum) -> new CostSummaryResponse(
                        null,
                        null,
                        rs.getInt("request_count"),
                        rs.getInt("input_tokens"),
                        rs.getInt("output_tokens"),
                        rs.getInt("input_tokens") + rs.getInt("output_tokens"),
                        rs.getBigDecimal("estimated_cost_usd"),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                userId,
                startInclusive,
                endExclusive
        );
    }

    public List<CostByModel> summarizeCostByModel(
            UUID userId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        return jdbcTemplate.query(
                """
                select
                    coalesce(llm_provider, 'unknown') as llm_provider,
                    coalesce(llm_model, 'unknown') as llm_model,
                    coalesce(sum(request_count), 0)::int as request_count,
                    coalesce(sum(input_tokens), 0)::int as input_tokens,
                    coalesce(sum(output_tokens), 0)::int as output_tokens,
                    coalesce(sum(estimated_cost_usd), 0)::numeric(12, 6) as estimated_cost_usd
                from usage_logs
                where user_id = ?
                  and status = 'success'
                  and created_at >= ?
                  and created_at < ?
                group by coalesce(llm_provider, 'unknown'), coalesce(llm_model, 'unknown')
                order by estimated_cost_usd desc, request_count desc, llm_provider, llm_model
                """,
                (rs, rowNum) -> new CostByModel(
                        rs.getString("llm_provider"),
                        rs.getString("llm_model"),
                        rs.getInt("request_count"),
                        rs.getInt("input_tokens"),
                        rs.getInt("output_tokens"),
                        rs.getInt("input_tokens") + rs.getInt("output_tokens"),
                        rs.getBigDecimal("estimated_cost_usd")
                ),
                userId,
                startInclusive,
                endExclusive
        );
    }

    public List<CostByDay> summarizeCostByDay(
            UUID userId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive,
            String zoneId
    ) {
        return jdbcTemplate.query(
                """
                select
                    usage_date,
                    coalesce(sum(request_count), 0)::int as request_count,
                    coalesce(sum(input_tokens), 0)::int as input_tokens,
                    coalesce(sum(output_tokens), 0)::int as output_tokens,
                    coalesce(sum(estimated_cost_usd), 0)::numeric(12, 6) as estimated_cost_usd
                from (
                    select
                        cast(created_at at time zone ? as date) as usage_date,
                        request_count,
                        input_tokens,
                        output_tokens,
                        estimated_cost_usd
                    from usage_logs
                    where user_id = ?
                      and status = 'success'
                      and created_at >= ?
                      and created_at < ?
                ) usage_by_day
                group by usage_date
                order by usage_date
                """,
                (rs, rowNum) -> new CostByDay(
                        rs.getDate("usage_date").toLocalDate(),
                        rs.getInt("request_count"),
                        rs.getInt("input_tokens"),
                        rs.getInt("output_tokens"),
                        rs.getInt("input_tokens") + rs.getInt("output_tokens"),
                        rs.getBigDecimal("estimated_cost_usd")
                ),
                zoneId,
                userId,
                startInclusive,
                endExclusive
        );
    }

    public List<MissingPricingModel> findMissingPricingModels(
            UUID userId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        return jdbcTemplate.query(
                """
                select
                    usage.llm_provider,
                    usage.llm_model,
                    coalesce(sum(usage.request_count), 0)::int as request_count
                from usage_logs usage
                left join model_pricing pricing
                  on lower(pricing.provider) = lower(usage.llm_provider)
                 and lower(pricing.model) = lower(usage.llm_model)
                 and pricing.active = true
                where usage.user_id = ?
                  and usage.status = 'success'
                  and usage.created_at >= ?
                  and usage.created_at < ?
                  and usage.llm_provider is not null
                  and usage.llm_model is not null
                  and pricing.id is null
                group by usage.llm_provider, usage.llm_model
                order by request_count desc, usage.llm_provider, usage.llm_model
                """,
                (rs, rowNum) -> new MissingPricingModel(
                        rs.getString("llm_provider"),
                        rs.getString("llm_model"),
                        rs.getInt("request_count")
                ),
                userId,
                startInclusive,
                endExclusive
        );
    }
}
