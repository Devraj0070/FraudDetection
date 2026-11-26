package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.frauddetection.frauddetection.dto.AdminAlertResponse;
import com.frauddetection.frauddetection.dto.AdminPendingTransactionDto;
import com.frauddetection.frauddetection.dto.ReviewRequest;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class ApiAdminControllerTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ApiAdminController controller;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole("ADMIN");
    }

    @Test
    void showsAlertsNewestFirst() {
        FraudAlert newest = new FraudAlert();
        newest.setId(10L);
        newest.setAlertType("SUSPICIOUS_VELOCITY");
        FraudAlert older = new FraudAlert();
        older.setId(5L);
        older.setAlertType("HIGH_AMOUNT");

        when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(newest, older));

        ResponseEntity<List<AdminAlertResponse>> response = controller.getAlerts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(10L, response.getBody().get(0).id());
        assertEquals("SUSPICIOUS_VELOCITY", response.getBody().get(0).alertType());
        assertEquals(5L, response.getBody().get(1).id());
        verify(fraudAlertRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void shouldReturnPendingTransactions() {
        Transaction tx1 = new Transaction();
        tx1.setId(101L);
        tx1.setStatus("PENDING_REVIEW");

        when(transactionService.getPendingTransactions()).thenReturn(List.of(tx1));

        ResponseEntity<List<AdminPendingTransactionDto>> response = controller.getPendingTransactions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(101L, response.getBody().get(0).id());
        assertEquals("PENDING_REVIEW", response.getBody().get(0).status());
        verify(transactionService).getPendingTransactions();
    }

    @Test
    void shouldApprovePendingTransactionSuccessfully() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        Transaction approvedTx = new Transaction();
        approvedTx.setId(200L);
        approvedTx.setStatus("APPROVED");

        when(transactionService.approveTransaction(eq(200L), eq(adminUser), eq("Legitimate transaction verified")))
                .thenReturn(approvedTx);

        ResponseEntity<?> response = controller.approveTransaction(
                200L,
                new ReviewRequest("Legitimate transaction verified"),
                authentication
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("APPROVED", body.get("status"));
        assertEquals(200L, body.get("transactionId"));
    }

    @Test
    void shouldRejectPendingTransactionSuccessfully() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        Transaction rejectedTx = new Transaction();
        rejectedTx.setId(201L);
        rejectedTx.setStatus("DECLINED");

        when(transactionService.rejectTransaction(eq(201L), eq(adminUser), eq("Confirmed fraud")))
                .thenReturn(rejectedTx);

        ResponseEntity<?> response = controller.rejectTransaction(
                201L,
                new ReviewRequest("Confirmed fraud"),
                authentication
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("DECLINED", body.get("status"));
        assertEquals(201L, body.get("transactionId"));
    }

    @Test
    void shouldReturnBadRequestWhenApprovingAlreadyProcessedTransaction() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        when(transactionService.approveTransaction(eq(300L), eq(adminUser), any()))
                .thenThrow(new TransactionException("Transaction #300 is not pending review (current status: APPROVED)."));

        ResponseEntity<?> response = controller.approveTransaction(
                300L,
                new ReviewRequest("Repeated approval attempt"),
                authentication
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("message").toString().contains("not pending review"));
    }

    @Test
    void shouldApproveViaAlertIdSuccessfully() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        Transaction tx = new Transaction();
        tx.setId(400L);
        tx.setStatus("PENDING_REVIEW");

        FraudAlert alert = new FraudAlert();
        alert.setId(50L);
        alert.setTransaction(tx);

        when(fraudAlertRepository.findById(50L)).thenReturn(Optional.of(alert));

        Transaction approvedTx = new Transaction();
        approvedTx.setId(400L);
        approvedTx.setStatus("APPROVED");

        when(transactionService.approveTransaction(eq(400L), eq(adminUser), any()))
                .thenReturn(approvedTx);

        ResponseEntity<?> response = controller.approveAlert(50L, new ReviewRequest("Alert approved"), authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenApprovingNonExistentAlert() {
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.approveAlert(999L, new ReviewRequest("test"), authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
