package com.frauddetection.frauddetection.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.TransactionService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public TransactionController(TransactionService transactionService,
                                 TransactionRepository transactionRepository,
                                 UserRepository userRepository,
                                 AccountRepository accountRepository) {

        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/transaction")
    public String showTransactionPage() {
        return "user/transaction";
    }

    @PostMapping("/transaction")
    public String submitTransaction(@RequestParam double amount,
                                    Authentication authentication,
                                    HttpServletRequest request,
                                    Model model) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Account> accounts = accountRepository.findByUser(user);

        if (accounts.isEmpty()) {
            throw new RuntimeException("No account found for user");
        }

        Account account = accounts.get(0);

        Transaction transaction = new Transaction();

        transaction.setAmount(java.math.BigDecimal.valueOf(amount));
        transaction.setTransactionType("PAYMENT");
        transaction.setTransactionTime(LocalDateTime.now());
        transaction.setAccount(account);

        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        transaction.setIpAddress(ipAddress);
        transaction.setUserAgent(userAgent);

        transaction.setStatus("PENDING");

        transactionService.saveTransaction(transaction);

        model.addAttribute(
                "message",
                "Your transaction has been submitted for fraud analysis."
        );

        return "user/result";
    }

    @GetMapping("/history")
    public String showHistory(Authentication authentication, Model model) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Account> accounts = accountRepository.findByUser(user);

        List<Transaction> transactions = new java.util.ArrayList<>();

        for (Account account : accounts) {
            transactions.addAll(
                    transactionRepository
                            .findByAccountOrderByTransactionTimeDesc(account)
            );
        }

        model.addAttribute("transactions", transactions);

        return "user/history";
    }
}