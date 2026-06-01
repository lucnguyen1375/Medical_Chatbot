package com.medicalchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalchatbot.backend.dto.QuotaPolicyInfo;
import com.medicalchatbot.backend.dto.QuotaUsageSummary;
import com.medicalchatbot.backend.exception.QuotaExceededException;
import com.medicalchatbot.backend.repository.AppUserRepository;
import com.medicalchatbot.backend.repository.AuditLogRepository;
import com.medicalchatbot.backend.repository.QuotaPolicyRepository;
import com.medicalchatbot.backend.repository.UsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private QuotaPolicyRepository quotaPolicyRepository;

    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void demoUserStatusReturnsRemainingQuota() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        QuotaService service = newService();

        when(appUserRepository.findIdByUsername("demo_user")).thenReturn(Optional.of(userId));
        when(quotaPolicyRepository.findByUserId(userId)).thenReturn(Optional.of(new QuotaPolicyInfo(
                "free_demo",
                50,
                1000,
                new BigDecimal("1.00")
        )));
        when(usageLogRepository.summarizeSuccessfulUsage(
                eq(userId),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new QuotaUsageSummary(
                12,
                100,
                50,
                new BigDecimal("0.25")
        ));

        var status = service.demoUserStatus();

        assertEquals("demo_user", status.user());
        assertEquals("free_demo", status.policy());
        assertEquals(12, status.usedRequests());
        assertEquals(150, status.usedTokens());
        assertEquals(38, status.remainingRequests());
        assertEquals(850, status.remainingTokens());
        assertEquals(new BigDecimal("0.75"), status.remainingCostUsd());
        assertEquals(true, status.allowed());
    }

    @Test
    void assertQuotaAvailableBlocksAndAuditsWhenDailyRequestLimitReached() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        QuotaService service = newService();

        when(quotaPolicyRepository.findByUserId(userId)).thenReturn(Optional.of(new QuotaPolicyInfo(
                "free_demo",
                1,
                1000,
                new BigDecimal("1.00")
        )));
        when(usageLogRepository.summarizeSuccessfulUsage(
                eq(userId),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new QuotaUsageSummary(
                1,
                0,
                0,
                BigDecimal.ZERO
        ));

        QuotaExceededException exception = assertThrows(
                QuotaExceededException.class,
                () -> service.assertQuotaAvailable(userId)
        );

        assertEquals(false, exception.quotaStatus().allowed());
        assertEquals(0, exception.quotaStatus().remainingRequests());
        verify(auditLogRepository).save(
                eq(userId),
                isNull(),
                eq("QUOTA_BLOCKED"),
                eq("app_user"),
                eq(userId.toString()),
                any(String.class)
        );
    }

    @Test
    void assertQuotaAvailableBlocksWhenDailyCostLimitReached() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        QuotaService service = newService();

        when(quotaPolicyRepository.findByUserId(userId)).thenReturn(Optional.of(new QuotaPolicyInfo(
                "free_demo",
                50,
                100000,
                new BigDecimal("0.01")
        )));
        when(usageLogRepository.summarizeSuccessfulUsage(
                eq(userId),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new QuotaUsageSummary(
                1,
                100,
                50,
                new BigDecimal("0.01")
        ));

        QuotaExceededException exception = assertThrows(
                QuotaExceededException.class,
                () -> service.assertQuotaAvailable(userId)
        );

        assertEquals(false, exception.quotaStatus().allowed());
        assertEquals(0, BigDecimal.ZERO.compareTo(exception.quotaStatus().remainingCostUsd()));
        assertEquals(
                "\u0110\u00e3 v\u01b0\u1ee3t qu\u00e1 h\u1ea1n m\u1ee9c chi ph\u00ed AI/ng\u00e0y.",
                exception.getMessage()
        );
    }

    private QuotaService newService() {
        return new QuotaService(
                appUserRepository,
                quotaPolicyRepository,
                usageLogRepository,
                auditLogRepository,
                new ObjectMapper(),
                ZoneId.of("Asia/Saigon")
        );
    }
}
