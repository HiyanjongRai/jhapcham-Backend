package com.example.jhapcham.user.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserProfileUpdateRequestDTO {

    private String fullName;
    private String contactNumber;
    private String address;

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    private MultipartFile profileImage;

}