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
    public String showTransactionPage(Authentication authentication, Model model) {
        populateUserAndAccount(authentication, model);
        return "user/transaction";
    }

    @PostMapping("/transaction")
    public String submitTransaction(@RequestParam BigDecimal amount,
                                    @RequestParam TransactionType transactionType,
                                    Authentication authentication,
                                    HttpServletRequest request,
                                    Model model) {

        String username = authentication != null ? authentication.getName() : null;

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
        if (transaction != null) {
            if (transaction.getAmount() == null) {
                transaction.setAmount(amount);
            }
            if (transaction.getTransactionType() == null) {
                transaction.setTransactionType(transactionType);
            }
        }

        String status = transaction != null ? transaction.getStatus() : null;
        boolean isBlocked = "BLOCKED".equals(status);
        boolean isMlFraud = "FRAUD".equals(fraudPrediction.getPrediction());
        boolean isRuleAnomaly = fraudPrediction.isRuleAnomaly();

        double probability = fraudPrediction.getProbability();
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

        String detectionReason = fraudPrediction.getDetectionReason();
        if (detectionReason == null) {
            if (isBlocked) {
                if (isMlFraud && isRuleAnomaly) {
                    detectionReason = "Machine Learning & Behavioral Anomaly";
                } else if (isMlFraud) {
                    detectionReason = "Machine Learning Fraud Detection";
                } else {
                    detectionReason = "Behavioral Anomaly / Suspicious Activity";
                }
            } else if ("DECLINED".equals(status)) {
                if (transaction != null
                        && transaction.getAmount() != null
                        && transaction.getTransactionType() != null
                        && transaction.getAmount().compareTo(transaction.getTransactionType().getTransactionLimit()) > 0) {
                    detectionReason = "Limit Exceeded";
                } else {
                    detectionReason = "Balance Insufficient";
                }
            } else {
                detectionReason = "Normal / Legitimate";
            }
        }

        String classificationVerdict;
        if (isBlocked) {
            if (isMlFraud && isRuleAnomaly) {
                classificationVerdict = "ML FRAUD & BEHAVIORAL ANOMALY";
            } else if (isMlFraud) {
                classificationVerdict = "SUSPICIOUS / FLAGGED";
            } else {
                classificationVerdict = "BEHAVIORAL ANOMALY / FLAGGED";
            }
        } else {
            classificationVerdict = "VERIFIED LEGITIMATE";
        }

        model.addAttribute("message", transactionMessage(transaction));
        model.addAttribute("prediction", fraudPrediction.getPrediction());
        model.addAttribute("mlPrediction", fraudPrediction.getPrediction());
        model.addAttribute("fraudProbability", fraudPrediction.getProbability());
        model.addAttribute("modelName", fraudPrediction.getModelName());
        model.addAttribute("transactionStatus", transaction.getStatus());
        model.addAttribute("transactionAmount", transaction.getAmount());
        model.addAttribute("transactionType", transaction.getTransactionType());
        model.addAttribute("transactionTime", transaction.getTransactionTime());
        model.addAttribute("riskAssessment", riskAssessment);
        model.addAttribute("riskLevel", riskAssessment);
        model.addAttribute("aiEngineName", "AI Fraud Protection");
        model.addAttribute("detectionReason", detectionReason);
        model.addAttribute("ruleAnomaly", isRuleAnomaly);
        model.addAttribute("classificationVerdict", classificationVerdict);

        populateUserAndAccount(authentication, model);

        return "user/result";
    }

    private String transactionMessage(Transaction transaction) {
        if (transaction == null || transaction.getStatus() == null) {
            throw new IllegalStateException("Transaction status is missing.");
        }
        String status = transaction.getStatus();
        return switch (status) {
            case "APPROVED" -> "Your transaction was approved.";
            case "BLOCKED" -> "Your transaction was blocked because it was flagged for fraud review.";
            case "DECLINED" -> {
                if (transaction.getAmount() != null
                        && transaction.getTransactionType() != null
                        && transaction.getAmount().compareTo(transaction.getTransactionType().getTransactionLimit()) > 0) {
                    yield "Your transaction was declined because it exceeds the maximum limit for this transaction type.";
                }
                yield "Your transaction was declined because the account balance is insufficient.";
            }
            default -> throw new IllegalStateException("Unsupported transaction status: " + status);
        };
    }

    @GetMapping("/history")
    public String showHistory(Authentication authentication, Model model) {

        String username = authentication != null ? authentication.getName() : null;

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
        populateUserAndAccount(authentication, model);

        return "user/history";
    }

    private void populateUserAndAccount(Authentication authentication, Model model) {
        if (authentication == null) {
            return;
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        Account account = null;
        BigDecimal balance = BigDecimal.ZERO;
        if (user != null) {
            account = accountRepository.findTopByUserOrderByIdAsc(user)
                    .orElseGet(() -> {
                        List<Account> list = accountRepository.findByUser(user);
                        return list.isEmpty() ? null : list.get(0);
                    });
            if (account != null && account.getBalance() != null) {
                balance = account.getBalance();
            }
        }
        model.addAttribute("username", username);
        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("balance", balance);
    }
}
