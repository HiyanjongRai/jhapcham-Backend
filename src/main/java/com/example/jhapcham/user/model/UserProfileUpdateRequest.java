package com.example.jhapcham.user.model;

import lombok.Data;


@Data

public class UserProfileUpdateRequest {
    private String fullName;
    private String email;
    private String contactNumber;
    private String newPassword;
}
