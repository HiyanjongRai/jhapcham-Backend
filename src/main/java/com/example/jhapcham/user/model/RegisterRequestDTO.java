package com.example.jhapcham.user.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank
        String username,

        // optional for seller
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password,

        // optional
        @Size(max = 30)
        String contactNumber


) {
}