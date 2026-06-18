package com.example.jhapcham.refund.dto;

import lombok.Data;

@Data
public class CustomerPayoutDetailsRequestDTO {
    private String customerQrUrl;
    private String customerAccountDetails;
}
