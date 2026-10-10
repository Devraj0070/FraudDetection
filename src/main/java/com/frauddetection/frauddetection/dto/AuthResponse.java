package com.frauddetection.frauddetection.dto;

public record AuthResponse(
        boolean success,
        String message,
        String username,
        String email,
        String role,
        String accountNumber
) {}
