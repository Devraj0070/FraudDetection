package com.frauddetection.frauddetection.dto;

public record CsrfResponse(
        String token,
        String headerName,
        String parameterName
) {}
