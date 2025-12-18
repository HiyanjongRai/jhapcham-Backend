package com.example.jhapcham.seller;

import java.math.BigDecimal;

public record SellerIncomeDTO(
        Long sellerProfileId,
        BigDecimal totalIncome,
        BigDecimal totalShippingCost,
        BigDecimal netIncome
) {}
