package com.frauddetection.frauddetection.controller;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.frauddetection.frauddetection.dto.TransactionRequest;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.ApplicationException;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.TransactionService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST endpoint for programmatic and Swing desktop client transaction submissions.
 * Integrates directly with {@link TransactionService} and AI fraud detection pipeline.
 */
@RestController
@RequestMapping("/api/transactions")
public class ApiTransactionController {

    private static final Logger log = LoggerFactory.getLogger(ApiTransactionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudPredictionRepository fraudPredictionRepository;

    @Autowired
    public ApiTransactionController(
            TransactionService transactionService,
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            FraudPredictionRepository fraudPredictionRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudPredictionRepository = fraudPredictionRepository;
    }

    public ApiTransactionController(
            TransactionService transactionService,
            UserRepository userRepository,
            AccountRepository accountRepository) {
        this(transactionService, userRepository, accountRepository, null, null);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestBody TransactionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TransactionResponse(
                    false,
                    "Authentication required.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TransactionResponse(
                    false,
                    "User account not found.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        // Validate request payload
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    "Transaction request payload cannot be empty.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        if (request.amount() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    "Transaction amount is required.",
                    null, null, request.transactionType(), null, null, null, null, null, null
            ));
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    "Amount must be greater than zero.",
                    null, request.amount(), request.transactionType(), null, null, null, null, null, null
            ));
        }

        if (request.amount().scale() > 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    "Amount cannot have more than 2 decimal places.",
                    null, request.amount(), request.transactionType(), null, null, null, null, null, null
            ));
        }

        if (request.transactionType() == null || request.transactionType().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    "Transaction type is required.",
                    null, request.amount(), null, null, null, null, null, null, null
            ));
        }

        TransactionType type;
        try {
            type = TransactionType.valueOf(request.transactionType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    "Invalid transaction type: " + request.transactionType() + ". Valid types are PAYMENT, TRANSFER, CASH_OUT, DEBIT, CASH_IN.",
                    null, request.amount(), request.transactionType(), null, null, null, null, null, null
            ));
        }

        String remoteIp = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "SafePay-Desktop";

        FraudPrediction fraudPrediction;
        try {
            fraudPrediction = transactionService.processPayment(
                    user,
                    request.amount(),
                    type,
                    remoteIp,
                    userAgent
            );
        } catch (TransactionException ex) {
            log.warn("Transaction validation notice for user {}: {}", username, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    ex.getMessage(),
                    null, request.amount(), type.name(), "DECLINED", null,
                    ex.getMessage(), "HIGH", null, null
            ));
        } catch (ApplicationException ex) {
            log.warn("Application error processing transaction for user {}: {}", username, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    ex.getMessage(),
                    null, request.amount(), type.name(), null, null,
                    ex.getMessage(), null, null, null
            ));
        } catch (Exception ex) {
            log.error("Unexpected error executing transaction for user {}: {}", username, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new TransactionResponse(
                    false,
                    "An unexpected error occurred while processing the transaction: " + ex.getMessage(),
                    null, request.amount(), type.name(), null, null,
                    null, null, null, null
            ));
        }

        Transaction transaction = fraudPrediction != null ? fraudPrediction.getTransaction() : null;
        String status = transaction != null ? transaction.getStatus() : "UNKNOWN";
        Long txId = transaction != null ? transaction.getId() : null;
        String timeStr = transaction != null && transaction.getTransactionTime() != null
                ? transaction.getTransactionTime().format(TIME_FORMATTER)
                : "";

        boolean isBlocked = "BLOCKED".equalsIgnoreCase(status);
        boolean isDeclined = "DECLINED".equalsIgnoreCase(status);
        boolean isApproved = "APPROVED".equalsIgnoreCase(status);

        double probability = fraudPrediction != null ? fraudPrediction.getProbability() : 0.0;
        String riskAssessment;
        if (isBlocked) {
            riskAssessment = "HIGH";
        } else if (probability < 0.30) {
            riskAssessment = "LOW";
        } else if (probability < 0.70) {
            riskAssessment = "MEDIUM";
        } else {
            riskAssessment = "HIGH";
        }

        String detectionReason = fraudPrediction != null ? fraudPrediction.getDetectionReason() : null;
        if (detectionReason == null || detectionReason.isBlank()) {
            if (isBlocked) {
                boolean isMlFraud = fraudPrediction != null && "FRAUD".equalsIgnoreCase(fraudPrediction.getPrediction());
                boolean isRuleAnomaly = fraudPrediction != null && fraudPrediction.isRuleAnomaly();
                if (isMlFraud && isRuleAnomaly) {
                    detectionReason = "Machine Learning & Behavioral Anomaly";
                } else if (isMlFraud) {
                    detectionReason = "Machine Learning Fraud Detection";
                } else {
                    detectionReason = "Behavioral Anomaly / Suspicious Activity";
                }
            } else if (isDeclined) {
                if (request.amount().compareTo(type.getTransactionLimit()) > 0) {
                    detectionReason = "Limit Exceeded (Max ₹" + String.format("%,.0f", type.getTransactionLimit().doubleValue()) + ")";
                } else {
                    detectionReason = "Insufficient Account Balance";
                }
            } else {
                detectionReason = "Normal / Legitimate";
            }
        }

        // Retrieve current balance for response
        BigDecimal newBalance = null;
        Account account = accountRepository.findTopByUserOrderByIdAsc(user).orElse(null);
        if (account != null) {
            newBalance = account.getBalance();
        }

        String message;
        if (isApproved) {
            message = "Your transaction was approved and processed successfully.";
        } else if (isBlocked) {
            message = "Your transaction was blocked by SafePay AI Fraud Protection: " + detectionReason;
        } else {
            message = "Your transaction was declined: " + detectionReason;
        }

        return ResponseEntity.ok(new TransactionResponse(
                true,
                message,
                txId,
                request.amount(),
                type.name(),
                status,
                timeStr,
                detectionReason,
                riskAssessment,
                probability,
                newBalance
        ));
    }

    /**
     * Retrieves the authenticated user's transaction history with optional type, status, and search filters.
     */
    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Authentication required."
            ));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "User account not found."
            ));
        }

        TransactionType filterType = null;
        if (type != null && !type.isBlank()) {
            filterType = TransactionType.fromModelValue(type);
            if (filterType == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Invalid transaction type filter: " + type + ". Valid types: PAYMENT, TRANSFER, CASH_OUT, DEBIT, CASH_IN."
                ));
            }
        }

        String filterStatus = null;
        if (status != null && !status.isBlank()) {
            String upperStatus = status.trim().toUpperCase();
            if (!"APPROVED".equals(upperStatus) && !"DECLINED".equals(upperStatus) && !"BLOCKED".equals(upperStatus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Invalid transaction status filter: " + status + ". Valid statuses: APPROVED, DECLINED, BLOCKED."
                ));
            }
            filterStatus = upperStatus;
        }

        List<Account> accounts = accountRepository.findByUser(user);
        List<Transaction> allTransactions = new ArrayList<>();
        if (accounts != null && transactionRepository != null) {
            for (Account account : accounts) {
                List<Transaction> txList = transactionRepository.findByAccountOrderByTransactionTimeDesc(account);
                if (txList != null) {
                    allTransactions.addAll(txList);
                }
            }
        }

        allTransactions.sort((t1, t2) -> {
            if (t1.getTransactionTime() == null && t2.getTransactionTime() == null) return 0;
            if (t1.getTransactionTime() == null) return 1;
            if (t2.getTransactionTime() == null) return -1;
            return t2.getTransactionTime().compareTo(t1.getTransactionTime());
        });

        String searchTrimmed = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        if (searchTrimmed != null && searchTrimmed.startsWith("#")) {
            searchTrimmed = searchTrimmed.substring(1).trim();
        }

        List<TransactionSummaryDto> resultDtos = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (filterType != null && t.getTransactionType() != filterType) {
                continue;
            }
            if (filterStatus != null && (t.getStatus() == null || !filterStatus.equalsIgnoreCase(t.getStatus()))) {
                continue;
            }
            if (searchTrimmed != null && !searchTrimmed.isEmpty()) {
                boolean matchesId = t.getId() != null && String.valueOf(t.getId()).contains(searchTrimmed);
                boolean matchesAmount = t.getAmount() != null && t.getAmount().toPlainString().contains(searchTrimmed);
                boolean matchesType = t.getTransactionType() != null && t.getTransactionType().name().toLowerCase().contains(searchTrimmed);
                if (!matchesId && !matchesAmount && !matchesType) {
                    continue;
                }
            }

            String timeStr = t.getTransactionTime() != null ? t.getTransactionTime().format(TIME_FORMATTER) : "";
            String typeStr = t.getTransactionType() != null ? t.getTransactionType().name() : "PAYMENT";
            resultDtos.add(new TransactionSummaryDto(
                    t.getId(),
                    t.getAmount(),
                    typeStr,
                    t.getStatus(),
                    timeStr
            ));
        }

        return ResponseEntity.ok(resultDtos);
    }

    /**
     * Retrieves detailed receipt and fraud evaluation for a specific transaction.
     * Enforces ownership: only the account owner or an administrator can access it.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionDetails(
            @PathVariable("id") Long id,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TransactionResponse(
                    false,
                    "Authentication required.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TransactionResponse(
                    false,
                    "User account not found.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionResponse(
                    false,
                    "Invalid transaction ID.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        if (transactionRepository == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new TransactionResponse(
                    false,
                    "Transaction repository unavailable.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        Optional<Transaction> txOpt = transactionRepository.findById(id);
        if (txOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new TransactionResponse(
                    false,
                    "Transaction not found with ID: " + id,
                    null, null, null, null, null, null, null, null, null
            ));
        }

        Transaction transaction = txOpt.get();

        // Enforce ownership: transaction must belong to authenticated user (or user must be ADMIN)
        Account account = transaction.getAccount();
        boolean isOwner = account != null && account.getUser() != null && user.getId() != null
                && user.getId().equals(account.getUser().getId());
        boolean isAdmin = user.getRole() != null
                && ("ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole()));

        if (!isOwner && !isAdmin) {
            log.warn("Cross-user transaction access denied: User {} attempted to access transaction #{} owned by {}",
                    username, id, (account != null && account.getUser() != null ? account.getUser().getUsername() : "unknown"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new TransactionResponse(
                    false,
                    "Access denied: Transaction does not belong to the authenticated user.",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        FraudPrediction fraudPrediction = null;
        if (fraudPredictionRepository != null) {
            fraudPrediction = fraudPredictionRepository.findByTransaction(transaction).orElse(null);
        }

        String status = transaction.getStatus() != null ? transaction.getStatus() : "UNKNOWN";
        boolean isBlocked = "BLOCKED".equalsIgnoreCase(status);
        boolean isDeclined = "DECLINED".equalsIgnoreCase(status);
        boolean isApproved = "APPROVED".equalsIgnoreCase(status);

        double probability = fraudPrediction != null ? fraudPrediction.getProbability() : 0.0;
        String riskAssessment;
        if (isBlocked) {
            riskAssessment = "HIGH";
        } else if (probability < 0.30) {
            riskAssessment = "LOW";
        } else if (probability < 0.70) {
            riskAssessment = "MEDIUM";
        } else {
            riskAssessment = "HIGH";
        }

        String detectionReason = fraudPrediction != null ? fraudPrediction.getDetectionReason() : null;
        if (detectionReason == null || detectionReason.isBlank()) {
            if (isBlocked) {
                detectionReason = "Flagged for fraud review";
            } else if (isDeclined) {
                detectionReason = "Transaction declined";
            } else {
                detectionReason = "Normal / Legitimate";
            }
        }

        String timeStr = transaction.getTransactionTime() != null
                ? transaction.getTransactionTime().format(TIME_FORMATTER)
                : "";

        BigDecimal balance = account != null ? account.getBalance() : null;
        String typeStr = transaction.getTransactionType() != null ? transaction.getTransactionType().name() : "PAYMENT";

        String message;
        if (isApproved) {
            message = "Transaction approved and settled.";
        } else if (isBlocked) {
            message = "Transaction blocked by SafePay AI Fraud Protection: " + detectionReason;
        } else {
            message = "Transaction declined: " + detectionReason;
        }

        return ResponseEntity.ok(new TransactionResponse(
                true,
                message,
                transaction.getId(),
                transaction.getAmount(),
                typeStr,
                status,
                timeStr,
                detectionReason,
                riskAssessment,
                probability,
                balance
        ));
    }
}
