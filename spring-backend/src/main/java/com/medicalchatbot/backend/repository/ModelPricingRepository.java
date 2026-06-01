package com.medicalchatbot.backend.repository;

import java.util.List;
import java.util.Optional;

import com.medicalchatbot.backend.dto.ModelPricingInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ModelPricingRepository {

    private final JdbcTemplate jdbcTemplate;

    public ModelPricingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ModelPricingInfo> findActiveByProviderAndModel(String provider, String model) {
        if (provider == null || model == null || provider.isBlank() || model.isBlank()) {
            return Optional.empty();
        }

        return jdbcTemplate.query(
                """
                select
                    provider,
                    model,
                    input_price_per_1m_tokens,
                    output_price_per_1m_tokens,
                    currency
                from model_pricing
                where lower(provider) = lower(?)
                  and lower(model) = lower(?)
                  and active = true
                limit 1
                """,
                (rs, rowNum) -> new ModelPricingInfo(
                        rs.getString("provider"),
                        rs.getString("model"),
                        rs.getBigDecimal("input_price_per_1m_tokens"),
                        rs.getBigDecimal("output_price_per_1m_tokens"),
                        rs.getString("currency")
                ),
                provider,
                model
        ).stream().findFirst();
    }

    public List<ModelPricingInfo> findActivePricing() {
        return jdbcTemplate.query(
                """
                select
                    provider,
                    model,
                    input_price_per_1m_tokens,
                    output_price_per_1m_tokens,
                    currency
                from model_pricing
                where active = true
                order by provider, model
                """,
                (rs, rowNum) -> new ModelPricingInfo(
                        rs.getString("provider"),
                        rs.getString("model"),
                        rs.getBigDecimal("input_price_per_1m_tokens"),
                        rs.getBigDecimal("output_price_per_1m_tokens"),
                        rs.getString("currency")
                )
        );
    }
}
