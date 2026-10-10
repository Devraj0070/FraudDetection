package com.frauddetection.frauddetection.dto;

public record RegisterRequest(
        String username,
        String email,
        String password
) {}
