package com.example.jhapcham.user.dto;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
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