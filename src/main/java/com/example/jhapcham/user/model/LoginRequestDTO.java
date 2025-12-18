package com.example.jhapcham.user.model;


import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String usernameOrEmail,
        @NotBlank String password
) {
}
