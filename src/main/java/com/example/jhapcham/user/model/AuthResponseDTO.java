package com.example.jhapcham.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {

    private Long userId;
    private String username;
    private String email;
    private Role role;
    private Status status;
    private String message;
    private String token;

}