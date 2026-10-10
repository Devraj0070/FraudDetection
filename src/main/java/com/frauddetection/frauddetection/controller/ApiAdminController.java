package com.frauddetection.frauddetection.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;

/**
 * REST controller for administrator fraud monitoring.
 * Exposes fraud alerts ordered newest first to authenticated users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ApiAdminController {

    private final FraudAlertRepository fraudAlertRepository;

    public ApiAdminController(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<FraudAlert>> getAlerts() {
        List<FraudAlert> alerts = fraudAlertRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(alerts);
    }
}
