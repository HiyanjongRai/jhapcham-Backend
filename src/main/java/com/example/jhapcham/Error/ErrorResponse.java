package com.example.jhapcham.Error;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standardized error response format for all API errors.
 * Provides consistent structure for frontend error handling.
 */
public class ErrorResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private String timestamp;

    public ErrorResponse() {
        this.success = false;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public ErrorResponse(String errorCode, String message) {
        this.success = false;
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public ErrorResponse(String message) {
        this.success = false;
        this.errorCode = "ERROR";
        this.message = message;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
