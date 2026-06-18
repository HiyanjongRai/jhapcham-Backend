package com.example.jhapcham.user.dto;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
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