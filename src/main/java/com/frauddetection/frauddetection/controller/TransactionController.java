package com.frauddetection.frauddetection.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.ApplicationException;
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
    public String submitTransaction(@RequestParam BigDecimal amount,
                                    @RequestParam TransactionType transactionType,
                                    Authentication authentication,
                                    HttpServletRequest request,
                                    Model model) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApplicationException("User not found"));

        FraudPrediction fraudPrediction = transactionService.processPayment(
                user,
                amount,
                transactionType,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        Transaction transaction = fraudPrediction.getTransaction();

        model.addAttribute("message", transactionMessage(transaction.getStatus()));
        model.addAttribute("prediction", fraudPrediction.getPrediction());
        model.addAttribute("fraudProbability", fraudPrediction.getProbability());
        model.addAttribute("modelName", fraudPrediction.getModelName());
        model.addAttribute("transactionStatus", transaction.getStatus());
        model.addAttribute("transactionAmount", transaction.getAmount());
        model.addAttribute("transactionType", transaction.getTransactionType());
        model.addAttribute("transactionTime", transaction.getTransactionTime());

        return "user/result";
    }

    private String transactionMessage(String status) {
        return switch (status) {
            case "APPROVED" -> "Your transaction was approved.";
            case "DECLINED" -> "Your transaction was declined because the account balance is insufficient.";
            case "BLOCKED" -> "Your transaction was blocked for fraud review.";
            default -> throw new IllegalStateException("Unsupported transaction status: " + status);
        };
    }

    @GetMapping("/history")
    public String showHistory(Authentication authentication, Model model) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApplicationException("User not found"));

        List<Account> accounts = accountRepository.findByUser(user);

        List<Transaction> transactions = new java.util.ArrayList<>();

        for (Account account : accounts) {
            transactions.addAll(
                    transactionRepository
                            .findByAccountOrderByTransactionTimeDesc(account)
            );
        }

        transactions.sort((t1, t2) -> {
            if (t1.getTransactionTime() == null && t2.getTransactionTime() == null) return 0;
            if (t1.getTransactionTime() == null) return 1;
            if (t2.getTransactionTime() == null) return -1;
            return t2.getTransactionTime().compareTo(t1.getTransactionTime());
        });

        model.addAttribute("transactions", transactions);

        return "user/history";
    }
}
