package com.example.jhapcham.seller.dto;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import java.math.BigDecimal;

public record SellerIncomeDTO(
        Long sellerProfileId,
        BigDecimal totalIncome,
        BigDecimal totalShippingCost,
        BigDecimal totalCommission,
        BigDecimal netIncome
) {}
