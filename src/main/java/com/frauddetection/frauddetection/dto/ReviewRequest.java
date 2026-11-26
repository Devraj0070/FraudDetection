package com.frauddetection.frauddetection.dto;

/**
 * Request payload for administrator transaction approval or rejection.
 *
 * @param notes Optional administrator audit remarks or rationale.
 */
public record ReviewRequest(String notes) {
}
