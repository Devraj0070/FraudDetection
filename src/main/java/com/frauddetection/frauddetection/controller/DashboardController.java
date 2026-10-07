package com.frauddetection.frauddetection.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardController(UserRepository userRepository,
                               AccountRepository accountRepository,
                               TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Authentication authentication, Model model) {

        String username = authentication != null ? authentication.getName() : null;
        User user = null;
        if (username != null) {
            user = userRepository.findByUsername(username).orElse(null);
        }

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

        BigDecimal balance = BigDecimal.ZERO;
        long totalCount = 0;
        long approvedCount = 0;
        long declinedCount = 0;
        long blockedCount = 0;
        List<Transaction> recentTransactions = Collections.emptyList();

        if (account != null) {
            balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            totalCount = transactionRepository.countByAccount(account);
            approvedCount = transactionRepository.countByAccountAndStatus(account, "APPROVED");
            declinedCount = transactionRepository.countByAccountAndStatus(account, "DECLINED");
            blockedCount = transactionRepository.countByAccountAndStatus(account, "BLOCKED");
            recentTransactions = transactionRepository.findTop5ByAccountOrderByTransactionTimeDesc(account);
        }

        model.addAttribute("username", username);
        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("balance", balance);
        model.addAttribute("totalTransactions", totalCount);
        model.addAttribute("approvedTransactions", approvedCount);
        model.addAttribute("declinedTransactions", declinedCount);
        model.addAttribute("blockedTransactions", blockedCount);
        model.addAttribute("recentTransactions", recentTransactions);

        return "user/dashboard";
    }
}
