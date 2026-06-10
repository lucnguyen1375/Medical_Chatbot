package com.medicalchatbot.backend.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import com.medicalchatbot.backend.dto.response.CostSummaryResponse;
import com.medicalchatbot.backend.dto.response.ModelPricingListResponse;
import com.medicalchatbot.backend.repository.ModelPricingRepository;
import com.medicalchatbot.backend.repository.UserRepository;
import com.medicalchatbot.backend.repository.UsageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CostManagementService {

    private static final String DEMO_USERNAME = "demo_user";

    private final UserRepository userRepository;
    private final UsageLogRepository usageLogRepository;
    private final ModelPricingRepository modelPricingRepository;
    private final ZoneId costZone;

    @Autowired
    public CostManagementService(
            UserRepository userRepository,
            UsageLogRepository usageLogRepository,
            ModelPricingRepository modelPricingRepository
    ) {
        this(userRepository, usageLogRepository, modelPricingRepository, ZoneId.systemDefault());
    }

    CostManagementService(
            UserRepository userRepository,
            UsageLogRepository usageLogRepository,
            ModelPricingRepository modelPricingRepository,
            ZoneId costZone
    ) {
        this.userRepository = userRepository;
        this.usageLogRepository = usageLogRepository;
        this.modelPricingRepository = modelPricingRepository;
        this.costZone = costZone;
    }

    public CostSummaryResponse demoUserCostSummary(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kho\u1ea3ng th\u1eddi gian th\u1ed1ng k\u00ea chi ph\u00ed kh\u00f4ng h\u1ee3p l\u1ec7."
            );
        }

        UUID userId = getDemoUserId();
        OffsetDateTime startInclusive = from.atStartOfDay(costZone).toOffsetDateTime();
        OffsetDateTime endExclusive = to.plusDays(1).atStartOfDay(costZone).toOffsetDateTime();
        CostSummaryResponse totals = usageLogRepository.summarizeCost(userId, startInclusive, endExclusive);

        return new CostSummaryResponse(
                from,
                to,
                totals.requestCount(),
                totals.inputTokens(),
                totals.outputTokens(),
                totals.totalTokens(),
                totals.estimatedCostUsd(),
                usageLogRepository.summarizeCostByModel(userId, startInclusive, endExclusive),
                usageLogRepository.summarizeCostByDay(userId, startInclusive, endExclusive, costZone.getId()),
                usageLogRepository.findMissingPricingModels(userId, startInclusive, endExclusive)
        );
    }

    public ModelPricingListResponse activePricing() {
        return new ModelPricingListResponse(modelPricingRepository.findActivePricing());
    }

    private UUID getDemoUserId() {
        return userRepository.findIdByUsername(DEMO_USERNAME)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Kh\u00f4ng t\u00ecm th\u1ea5y ng\u01b0\u1eddi d\u00f9ng demo."
                ));
    }
}
