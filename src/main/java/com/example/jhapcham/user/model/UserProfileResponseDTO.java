package com.example.jhapcham.user.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponseDTO {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String address;
    private String contactNumber;
    private String profileImagePath;
    private Role role;
    private Status status;


}