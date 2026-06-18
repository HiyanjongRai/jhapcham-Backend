package com.example.jhapcham.user.dto;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
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
    private String accessToken;
    private String tokenType;
    private Long expiresInSeconds;

}
