package com.frauddetection.frauddetection.controller;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frauddetection.frauddetection.dto.DashboardResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

/**
 * REST endpoint providing authenticated dashboard metrics and recent transactions
 * for the SafePay desktop client and API consumers.
 */
@RestController
@RequestMapping("/api/dashboard")
public class ApiDashboardController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ApiDashboardController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardData(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DashboardResponse(
                    false,
                    "Authentication required.",
                    null, null, null, null, null,
                    BigDecimal.ZERO, 0, 0, 0, 0, Collections.emptyList()
            ));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        Account account = null;
        if (user != null) {
            Optional<Account> accountOpt = accountRepository.findTopByUserOrderByIdAsc(user);
            if (accountOpt.isEmpty()) {
                List<Account> accounts = accountRepository.findByUser(user);
                account = accounts.isEmpty() ? null : accounts.get(0);
            } else {
                account = accountOpt.get();
            }
        }

        String role = (user != null && user.getRole() != null) ? user.getRole() : "USER";
        BigDecimal balance = BigDecimal.ZERO;
        String accountNumber = "";
        String accountType = "STANDARD";
        String accountStatus = "ACTIVE";
        long totalCount = 0;
        long approvedCount = 0;
        long declinedCount = 0;
        long blockedCount = 0;
        List<TransactionSummaryDto> recentDtos = new ArrayList<>();

        if (account != null) {
            balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            accountNumber = account.getAccountNumber() != null ? account.getAccountNumber() : "";
            accountType = account.getAccountType() != null ? account.getAccountType() : "STANDARD";
            accountStatus = account.getStatus() != null ? account.getStatus() : "ACTIVE";

            totalCount = transactionRepository.countByAccount(account);
            approvedCount = transactionRepository.countByAccountAndStatus(account, "APPROVED");
            declinedCount = transactionRepository.countByAccountAndStatus(account, "DECLINED");
            blockedCount = transactionRepository.countByAccountAndStatus(account, "BLOCKED");

            List<Transaction> recent = transactionRepository.findTop5ByAccountOrderByTransactionTimeDesc(account);
            if (recent != null) {
                for (Transaction t : recent) {
                    String timeStr = t.getTransactionTime() != null ? t.getTransactionTime().format(TIME_FORMATTER) : "";
                    String typeStr = t.getTransactionType() != null ? t.getTransactionType().name() : "PAYMENT";
                    recentDtos.add(new TransactionSummaryDto(
                            t.getId(),
                            t.getAmount(),
                            typeStr,
                            t.getStatus(),
                            timeStr
                    ));
                }
            }
        }

        return ResponseEntity.ok(new DashboardResponse(
                true,
                "Dashboard metrics loaded successfully",
                username,
                role,
                accountNumber,
                accountType,
                accountStatus,
                balance,
                totalCount,
                approvedCount,
                declinedCount,
                blockedCount,
                recentDtos
        ));
    }
}
