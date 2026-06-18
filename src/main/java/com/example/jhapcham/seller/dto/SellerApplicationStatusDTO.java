package com.example.jhapcham.seller.dto;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import java.time.LocalDateTime;

public record SellerApplicationStatusDTO(
        boolean submitted,
        String status,
        String message,
        String note,
        Long applicationId,
        LocalDateTime submittedAt
) {
}