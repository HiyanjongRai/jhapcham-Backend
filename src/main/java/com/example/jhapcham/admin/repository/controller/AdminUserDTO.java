package com.example.jhapcham.admin.repository.controller;

import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import lombok.Builder;
import lombok.Data;

@Data
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
