package com.example.jhapcham.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCheckoutDTO {
    private Long id;
    private String firstName;
    private String profileImagePath;
    private String email;
    private String contactNumber;
}