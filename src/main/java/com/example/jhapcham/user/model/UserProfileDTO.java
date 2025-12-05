package com.example.jhapcham.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String contactNumber;
    private String profileImageUrl;


}