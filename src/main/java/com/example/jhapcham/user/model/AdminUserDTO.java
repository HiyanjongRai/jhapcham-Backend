package com.example.jhapcham.user.model;

import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminUserDTO {

    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String contactNumber;
    private Role role;
    private Status status;


}