package com.frauddetection.frauddetection.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frauddetection.frauddetection.dto.AdminAlertResponse;
import com.frauddetection.frauddetection.dto.AdminPendingTransactionDto;
import com.frauddetection.frauddetection.dto.ReviewRequest;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.TransactionService;

/**
 * REST controller for administrator fraud monitoring and review lifecycle.
 * Requires Spring Security role ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ApiAdminController {

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @Autowired
    public ApiAdminController(
            FraudAlertRepository fraudAlertRepository,
            TransactionService transactionService,
            UserRepository userRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    public ApiAdminController(FraudAlertRepository fraudAlertRepository) {
        this(fraudAlertRepository, null, null);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AdminAlertResponse>> getAlerts() {
        List<FraudAlert> alerts = fraudAlertRepository.findAllByOrderByCreatedAtDesc();
        List<AdminAlertResponse> dtos = alerts != null
                ? alerts.stream().map(AdminAlertResponse::fromEntity).toList()
                : List.of();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminPendingTransactionDto>> getPendingTransactions() {
        if (transactionService == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Transaction> pending = transactionService.getPendingTransactions();
        List<AdminPendingTransactionDto> dtos = pending != null
                ? pending.stream().map(AdminPendingTransactionDto::fromEntity).toList()
                : List.of();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/transactions/pending")
    public ResponseEntity<List<AdminPendingTransactionDto>> getPendingTransactionsAlias() {
        return getPendingTransactions();
    }

    @PostMapping("/transactions/{id}/approve")
    public ResponseEntity<?> approveTransaction(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ReviewRequest request,
            Authentication authentication) {

        if (transactionService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", "Transaction service unavailable."));
        }

        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Admin authentication required."));
        }

        String notes = (request != null && request.notes() != null && !request.notes().isBlank())
                ? request.notes()
                : "Approved by administrator";

        try {
            Transaction approved = transactionService.approveTransaction(id, admin, notes);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Transaction #" + id + " approved and settled successfully.",
                    "transactionId", approved.getId(),
                    "status", approved.getStatus()
            ));
        } catch (TransactionException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/transactions/{id}/reject")
    public ResponseEntity<?> rejectTransaction(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ReviewRequest request,
            Authentication authentication) {

        if (transactionService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", "Transaction service unavailable."));
        }

        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Admin authentication required."));
        }

        String notes = (request != null && request.notes() != null && !request.notes().isBlank())
                ? request.notes()
                : "Rejected by administrator";

        try {
            Transaction rejected = transactionService.rejectTransaction(id, admin, notes);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Transaction #" + id + " rejected successfully.",
                    "transactionId", rejected.getId(),
                    "status", rejected.getStatus()
            ));
        } catch (TransactionException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/alerts/{id}/approve")
    public ResponseEntity<?> approveAlert(
            @PathVariable("id") Long alertId,
            @RequestBody(required = false) ReviewRequest request,
            Authentication authentication) {

        FraudAlert alert = fraudAlertRepository.findById(alertId).orElse(null);
        if (alert == null || alert.getTransaction() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Alert not found with ID: " + alertId));
        }
        return approveTransaction(alert.getTransaction().getId(), request, authentication);
    }

    @PostMapping("/alerts/{id}/reject")
    public ResponseEntity<?> rejectAlert(
            @PathVariable("id") Long alertId,
            @RequestBody(required = false) ReviewRequest request,
            Authentication authentication) {

        FraudAlert alert = fraudAlertRepository.findById(alertId).orElse(null);
        if (alert == null || alert.getTransaction() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Alert not found with ID: " + alertId));
        }
        return rejectTransaction(alert.getTransaction().getId(), request, authentication);
    }

    private User getAuthenticatedAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (userRepository == null) {
            User fallback = new User();
            fallback.setUsername(authentication.getName());
            fallback.setRole("ADMIN");
            return fallback;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
