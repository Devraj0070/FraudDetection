package com.frauddetection.frauddetection.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionService;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Service
public class TransactionService {

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_DECLINED = "DECLINED";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final FraudPredictionService fraudPredictionService;
    private final FraudPredictionRepository fraudPredictionRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            FraudPredictionService fraudPredictionService,
            FraudPredictionRepository fraudPredictionRepository,
            FraudAlertRepository fraudAlertRepository) {

        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.fraudPredictionService = fraudPredictionService;
        this.fraudPredictionRepository = fraudPredictionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional
    public FraudPrediction processPayment(
            User user,
            BigDecimal amount,
            TransactionType transactionType,
            String ipAddress,
            String userAgent) {

        validateAmount(amount);

        if (transactionType == null) {
            throw new TransactionException("Transaction type is required.");
        }

        Account account = accountRepository
                .findFirstByUserOrderByIdAsc(user)
                .orElseThrow(() -> new TransactionException("No account found for user."));

        BigDecimal balance = account.getBalance();
        if (balance == null) {
            throw new TransactionException("Account balance is unavailable.");
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(transactionType);
        transaction.setTransactionTime(LocalDateTime.now());
        transaction.setAccount(account);
        transaction.setIpAddress(ipAddress);
        transaction.setUserAgent(userAgent);
        transaction.setStatus("PENDING");

        FraudPredictionResult predictionResult = predict(transaction);
        applyTransactionOutcome(transaction, account, balance, predictionResult);

        Transaction savedTransaction = transactionRepository.save(transaction);
        FraudPrediction fraudPrediction = savePrediction(
                savedTransaction,
                predictionResult
        );

        if (STATUS_BLOCKED.equals(savedTransaction.getStatus())) {
            saveFraudAlert(savedTransaction);
        }

        return fraudPrediction;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransactionException("Amount must be greater than zero.");
        }
    }

    private FraudPredictionResult predict(Transaction transaction) {
        try {
            return fraudPredictionService.predict(transaction);
        } catch (RuntimeException exception) {
            throw new TransactionException(
                    "Fraud prediction could not be completed.",
                    exception
            );
        }
    }

    private void applyTransactionOutcome(
            Transaction transaction,
            Account account,
            BigDecimal balance,
            FraudPredictionResult predictionResult) {

        if ("FRAUD".equals(predictionResult.getPrediction())) {
            transaction.setStatus(STATUS_BLOCKED);
            return;
        }

        if (!"LEGITIMATE".equals(predictionResult.getPrediction())) {
            throw new TransactionException("Fraud prediction returned an unsupported result.");
        }

        if (transaction.getTransactionType() == TransactionType.CASH_IN) {
            account.setBalance(balance.add(transaction.getAmount()));
            accountRepository.save(account);
            transaction.setStatus(STATUS_APPROVED);
            return;
        }

        if (balance.compareTo(transaction.getAmount()) < 0) {
            transaction.setStatus(STATUS_DECLINED);
            return;
        }

        account.setBalance(balance.subtract(transaction.getAmount()));
        accountRepository.save(account);
        transaction.setStatus(STATUS_APPROVED);
    }

    private FraudPrediction savePrediction(
            Transaction transaction,
            FraudPredictionResult predictionResult) {

        FraudPrediction fraudPrediction = new FraudPrediction();
        fraudPrediction.setPrediction(predictionResult.getPrediction());
        fraudPrediction.setProbability(predictionResult.getProbability());
        fraudPrediction.setModelName(predictionResult.getModelName());
        fraudPrediction.setPredictionTime(LocalDateTime.now());
        fraudPrediction.setTransaction(transaction);

        fraudPredictionRepository.save(fraudPrediction);
        return fraudPrediction;
    }

    private void saveFraudAlert(Transaction transaction) {
        FraudAlert fraudAlert = new FraudAlert();
        fraudAlert.setAlertType("FRAUD");
        fraudAlert.setSeverity("HIGH");
        fraudAlert.setStatus("OPEN");
        fraudAlert.setCreatedAt(LocalDateTime.now());
        fraudAlert.setTransaction(transaction);
        fraudAlertRepository.save(fraudAlert);
    }
}
