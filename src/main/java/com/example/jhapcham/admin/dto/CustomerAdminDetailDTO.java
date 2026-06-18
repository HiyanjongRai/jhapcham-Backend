package com.example.jhapcham.admin.dto;


import com.example.jhapcham.admin.application.*;
import com.example.jhapcham.admin.dto.*;
import com.example.jhapcham.order.dto.OrderSummaryDTO;
import com.example.jhapcham.product.dto.ProductResponseDTO;
import com.example.jhapcham.user.domain.Status;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerAdminDetailDTO {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String contactNumber;
    private Status status;

    private List<OrderSummaryDTO> orders;
    private List<ProductResponseDTO> wishlist;

    private double totalSpent;
    private int totalOrders;
    private String favoritePaymentMethod;
}
