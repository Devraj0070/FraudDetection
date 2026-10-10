package com.frauddetection.frauddetection.client;

public class ApiClientException extends Exception {

    private final int statusCode;

    public ApiClientException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public ApiClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiClientException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
