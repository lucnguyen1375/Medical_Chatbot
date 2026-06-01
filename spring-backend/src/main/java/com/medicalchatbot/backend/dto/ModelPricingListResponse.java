package com.medicalchatbot.backend.dto;

import java.util.List;

public record ModelPricingListResponse(
        List<ModelPricingInfo> pricing
) {
}
