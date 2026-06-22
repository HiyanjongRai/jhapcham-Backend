package com.example.jhapcham.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode,
        Map<String, String> errors
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, errorCode, errors);
    }
}
