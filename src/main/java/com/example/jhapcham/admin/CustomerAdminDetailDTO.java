package com.example.jhapcham.admin;

import com.example.jhapcham.order.OrderSummaryDTO;
import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.report.ReportDTO;
import com.example.jhapcham.user.model.Status;
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
    private List<ReportDTO> reports;
    private List<ProductResponseDTO> wishlist;

    private double totalSpent;
    private int totalOrders;
    private String favoritePaymentMethod;
}
