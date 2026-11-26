package com.frauddetection.frauddetection.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.FundsReservation;
import com.frauddetection.frauddetection.entity.FundsReservation.ReservationStatus;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.fraud.FraudRuleEngine;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionService;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.FundsReservationRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Service
public class TransactionService {

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_DECLINED = "DECLINED";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final FraudPredictionService fraudPredictionService;
    private final FraudPredictionRepository fraudPredictionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudRuleEngine fraudRuleEngine;
    private final FundsReservationRepository fundsReservationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Autowired
    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            FraudPredictionService fraudPredictionService,
            FraudPredictionRepository fraudPredictionRepository,
            FraudAlertRepository fraudAlertRepository,
            FraudRuleEngine fraudRuleEngine,
            FundsReservationRepository fundsReservationRepository) {

        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.fraudPredictionService = fraudPredictionService;
        this.fraudPredictionRepository = fraudPredictionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudRuleEngine = fraudRuleEngine;
        this.fundsReservationRepository = fundsReservationRepository;
    }

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            FraudPredictionService fraudPredictionService,
            FraudPredictionRepository fraudPredictionRepository,
            FraudAlertRepository fraudAlertRepository,
            FraudRuleEngine fraudRuleEngine) {

        this(
                transactionRepository,
                accountRepository,
                fraudPredictionService,
                fraudPredictionRepository,
                fraudAlertRepository,
                fraudRuleEngine,
                null
        );
    }

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            FraudPredictionService fraudPredictionService,
            FraudPredictionRepository fraudPredictionRepository,
            FraudAlertRepository fraudAlertRepository) {

        this(
                transactionRepository,
                accountRepository,
                fraudPredictionService,
                fraudPredictionRepository,
                fraudAlertRepository,
                new FraudRuleEngine(transactionRepository),
                null
        );
    }

    /**
     * Obtains the total amount of active funds held for pending reviews for an account.
     */
    public BigDecimal getReservedFunds(Account account) {
        if (account == null || fundsReservationRepository == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = fundsReservationRepository.sumActiveReservationsByAccount(account);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Enforces the fundamental financial invariant:
     * Available Balance = Account Balance - Active Reservations
     */
    public BigDecimal getAvailableBalance(Account account) {
        if (account == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal ledgerBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        BigDecimal reservedFunds = getReservedFunds(account);
        return ledgerBalance.subtract(reservedFunds);
    }

    /**
     * Returns the ledger balance of the account.
     */
    public BigDecimal getLedgerBalance(Account account) {
        if (account == null || account.getBalance() == null) {
            return BigDecimal.ZERO;
        }
        return account.getBalance();
    }

    @Transactional
    public FraudPrediction processPayment(
            User user,
            BigDecimal amount,
            TransactionType transactionType,
            String receiverAccountNumber,
            String ipAddress,
            String userAgent) {
        return processPayment(user, amount, transactionType, receiverAccountNumber, ipAddress, userAgent, null);
    }

    @Transactional
    public FraudPrediction processPayment(
            User user,
            BigDecimal amount,
            TransactionType transactionType,
            String receiverAccountNumber,
            String ipAddress,
            String userAgent,
            String idempotencyKey) {

        validateAmount(amount);
        if (transactionType == null) {
            throw new TransactionException("Transaction type is required.");
        }

        // FINDING 1: Mandatory receiver validation for TRANSFER operations
        if (transactionType == TransactionType.TRANSFER) {
            if (receiverAccountNumber == null || receiverAccountNumber.trim().isEmpty()) {
                throw new TransactionException("Receiver account number is mandatory for TRANSFER operations.");
            }
        }

        // Unlocked read to resolve the sender account associated with authenticated user
        Account senderRef = accountRepository
                .findFirstByUserOrderByIdAsc(user)
                .orElseThrow(() -> new TransactionException("No account found for user."));

        validateAccountStatus(senderRef);

        // Persistent financial idempotency check (stored result lookup)
        if (idempotencyKey != null && !idempotencyKey.isBlank() && transactionRepository != null) {
            String trimmedKey = idempotencyKey.trim();
            java.util.Optional<Transaction> existingTxOpt = transactionRepository.findByIdempotencyKey(trimmedKey);
            if (existingTxOpt.isPresent()) {
                Transaction existingTx = existingTxOpt.get();
                if (existingTx.getAccount() != null && existingTx.getAccount().getUser() != null
                        && user != null && user.getId() != null
                        && !user.getId().equals(existingTx.getAccount().getUser().getId())) {
                    throw new TransactionException("Idempotency key does not belong to the authenticated user.");
                }
                if (fraudPredictionRepository != null) {
                    java.util.Optional<FraudPrediction> existingPred = fraudPredictionRepository.findByTransaction(existingTx);
                    if (existingPred.isPresent()) {
                        return existingPred.get();
                    }
                }
                FraudPrediction fallbackPred = new FraudPrediction();
                fallbackPred.setTransaction(existingTx);
                fallbackPred.setPrediction("APPROVED".equals(existingTx.getStatus()) ? "LEGITIMATE" : "FRAUD");
                fallbackPred.setProbability(0.0);
                return fallbackPred;
            }
        }

        Account senderAccount;
        Account receiverAccount = null;

        if (transactionType == TransactionType.TRANSFER) {
            String targetAccNum = receiverAccountNumber.trim();
            Account receiverRef = accountRepository
                    .findByAccountNumber(targetAccNum)
                    .orElseThrow(() -> new TransactionException("Receiver account not found with account number: " + targetAccNum));

            if (senderRef.getId() != null && senderRef.getId().equals(receiverRef.getId())) {
                throw new TransactionException("Cannot transfer funds to the same account.");
            }
            if (senderRef.getAccountNumber() != null && senderRef.getAccountNumber().equalsIgnoreCase(receiverRef.getAccountNumber())) {
                throw new TransactionException("Cannot transfer funds to the same account.");
            }

            validateAccountStatus(receiverRef);

            // Acquire locks on both accounts in ascending ID order to prevent deadlocks
            if (senderRef.getId() != null && receiverRef.getId() != null) {
                List<Account> lockedAccounts = accountRepository.findAccountsWithLockInOrder(
                        senderRef.getId(),
                        receiverRef.getId()
                );
                if (lockedAccounts.size() != 2) {
                    throw new TransactionException("Could not acquire locks for both transfer accounts.");
                }
                senderAccount = lockedAccounts.stream()
                        .filter(a -> a.getId().equals(senderRef.getId()))
                        .findFirst()
                        .orElseThrow(() -> new TransactionException("Sender account lock failed."));
                receiverAccount = lockedAccounts.stream()
                        .filter(a -> a.getId().equals(receiverRef.getId()))
                        .findFirst()
                        .orElseThrow(() -> new TransactionException("Receiver account lock failed."));
                refreshEntityIfManaged(senderAccount);
                refreshEntityIfManaged(receiverAccount);
            } else {
                senderAccount = senderRef;
                receiverAccount = receiverRef;
            }

            validateAccountStatus(senderAccount);
            validateAccountStatus(receiverAccount);
        } else {
            // Single-account financial operation: acquire pessimistic row lock
            if (senderRef.getId() != null) {
                senderAccount = accountRepository.findByIdWithLock(senderRef.getId())
                        .orElseThrow(() -> new TransactionException("Account not found."));
                refreshEntityIfManaged(senderAccount);
            } else {
                senderAccount = senderRef;
            }
            validateAccountStatus(senderAccount);
        }

        // Duplicate transaction submission prevention (sliding window protection for non-idempotent UI multi-clicks)
        if ((idempotencyKey == null || idempotencyKey.isBlank()) && transactionRepository != null) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(2);
            List<Transaction> duplicates = transactionRepository.findRecentDuplicates(
                    senderAccount, amount, transactionType, receiverAccount, cutoffTime
            );
            if (duplicates != null && !duplicates.isEmpty()) {
                throw new TransactionException("Duplicate transaction detected. An identical transaction was just submitted. Please wait before retrying.");
            }
        }

        Transaction transaction = new Transaction();
        transaction.setAccount(senderAccount);
        transaction.setReceiverAccount(receiverAccount);
        transaction.setAmount(amount);
        transaction.setTransactionType(transactionType);
        transaction.setTransactionTime(LocalDateTime.now());
        transaction.setIpAddress(ipAddress);
        transaction.setUserAgent(userAgent);
        transaction.setIdempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey.trim() : null);
        transaction.setStatus("PENDING");

        BigDecimal balance = senderAccount.getBalance();
        if (balance == null) {
            throw new TransactionException("Account balance is unavailable.");
        }

        FraudPredictionResult predictionResult = predict(transaction);
        boolean ruleAnomaly = fraudRuleEngine != null && fraudRuleEngine.isSuspicious(transaction);
        applyTransactionOutcome(transaction, senderAccount, receiverAccount, balance, predictionResult, ruleAnomaly);

        Transaction savedTransaction = transactionRepository.save(transaction);
        String detectionReason = determineDetectionReason(savedTransaction, predictionResult, ruleAnomaly);
        FraudPrediction fraudPrediction = savePrediction(savedTransaction, predictionResult, ruleAnomaly, detectionReason);

        if (STATUS_PENDING_REVIEW.equals(savedTransaction.getStatus())) {
            // Reserve transaction amount atomically for manual review
            if (transactionType != TransactionType.CASH_IN && fundsReservationRepository != null) {
                FundsReservation reservation = new FundsReservation(senderAccount, savedTransaction, amount);
                reservation.setNotes("Held for fraud review (" + detectionReason + ")");
                fundsReservationRepository.save(reservation);
            }
            saveFraudAlert(savedTransaction);
        }

        return fraudPrediction;
    }

    @Transactional
    public FraudPrediction processPayment(
            User user,
            BigDecimal amount,
            TransactionType transactionType,
            String ipAddress,
            String userAgent) {
        return processPayment(user, amount, transactionType, null, ipAddress, userAgent);
    }

    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByStatusOrderByTransactionTimeDesc(STATUS_PENDING_REVIEW);
    }

    @Transactional
    public Transaction approveTransaction(Long transactionId, User adminUser, String reviewNotes) {
        if (transactionId == null) {
            throw new TransactionException("Transaction ID is required.");
        }
        if (adminUser == null) {
            throw new TransactionException("Reviewing administrator is required.");
        }

        // Lock transaction row to prevent race conditions with concurrent reviews
        Transaction transaction = transactionRepository.findByIdWithLock(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found with ID: " + transactionId));

        if (!STATUS_PENDING_REVIEW.equals(transaction.getStatus())) {
            throw new TransactionException("Transaction #" + transactionId + " is not pending review (current status: " + transaction.getStatus() + ").");
        }

        Account senderAccount = transaction.getAccount();
        if (senderAccount == null) {
            throw new TransactionException("Transaction has no associated sender account.");
        }
        validateAccountStatus(senderAccount);

        BigDecimal amount = transaction.getAmount();
        TransactionType type = transaction.getTransactionType();

        // Lock account(s) in consistent ascending ID order
        Account lockedSender;
        Account lockedReceiver = null;
        if (type == TransactionType.TRANSFER) {
            Account receiverAccount = transaction.getReceiverAccount();
            if (receiverAccount == null) {
                throw new TransactionException("Transfer transaction has no associated receiver account.");
            }
            if (senderAccount.getId() != null && senderAccount.getId().equals(receiverAccount.getId())) {
                throw new TransactionException("Sender and receiver accounts are identical.");
            }

            if (senderAccount.getId() != null && receiverAccount.getId() != null) {
                List<Account> locked = accountRepository.findAccountsWithLockInOrder(
                        senderAccount.getId(), receiverAccount.getId()
                );
                if (locked.size() != 2) {
                    throw new TransactionException("Could not acquire locks for both transfer accounts.");
                }
                lockedSender = locked.stream().filter(a -> a.getId().equals(senderAccount.getId())).findFirst().orElseThrow();
                lockedReceiver = locked.stream().filter(a -> a.getId().equals(receiverAccount.getId())).findFirst().orElseThrow();
                refreshEntityIfManaged(lockedSender);
                refreshEntityIfManaged(lockedReceiver);
            } else {
                lockedSender = senderAccount;
                lockedReceiver = receiverAccount;
            }

            validateAccountStatus(lockedSender);
            validateAccountStatus(lockedReceiver);
        } else {
            if (senderAccount.getId() != null) {
                lockedSender = accountRepository.findByIdWithLock(senderAccount.getId())
                        .orElseThrow(() -> new TransactionException("Account not found."));
                refreshEntityIfManaged(lockedSender);
            } else {
                lockedSender = senderAccount;
            }
            validateAccountStatus(lockedSender);
        }

        // Verify that the reservation exists and is valid (for outflow transactions)
        FundsReservation reservation = null;
        if (type != TransactionType.CASH_IN && fundsReservationRepository != null) {
            reservation = fundsReservationRepository.findByTransactionWithLock(transaction).orElse(null);
            if (reservation != null) {
                if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                    throw new TransactionException("Funds reservation for transaction #" + transactionId + " is not active (status: " + reservation.getStatus() + ").");
                }
                if (reservation.getAmount().compareTo(amount) != 0) {
                    throw new TransactionException("Funds reservation amount (" + reservation.getAmount() + ") does not match transaction amount (" + amount + ").");
                }
            }
        }

        // Consume the reservation exactly once
        if (reservation != null) {
            reservation.setStatus(ReservationStatus.CONSUMED);
            reservation.setUpdatedAt(LocalDateTime.now());
            reservation.setNotes("Consumed upon administrator approval");
            fundsReservationRepository.save(reservation);
        }

        // Complete the debit and receiver credit atomically
        if (type == TransactionType.TRANSFER) {
            if (lockedSender.getBalance().compareTo(amount) < 0) {
                throw new TransactionException("Insufficient funds: Account balance is less than transaction amount.");
            }
            BigDecimal newSenderBal = lockedSender.getBalance().subtract(amount);
            if (newSenderBal.signum() < 0) {
                throw new TransactionException("Insufficient funds.");
            }
            lockedSender.setBalance(newSenderBal);
            lockedReceiver.setBalance(lockedReceiver.getBalance().add(amount));

            accountRepository.save(lockedSender);
            accountRepository.save(lockedReceiver);
        } else if (type == TransactionType.CASH_IN) {
            lockedSender.setBalance(lockedSender.getBalance().add(amount));
            accountRepository.save(lockedSender);
        } else {
            // PAYMENT, CASH_OUT, DEBIT
            if (lockedSender.getBalance().compareTo(amount) < 0) {
                throw new TransactionException("Insufficient funds: Account balance is less than transaction amount.");
            }
            BigDecimal newBal = lockedSender.getBalance().subtract(amount);
            if (newBal.signum() < 0) {
                throw new TransactionException("Insufficient funds.");
            }
            lockedSender.setBalance(newBal);
            accountRepository.save(lockedSender);
        }

        // Apply approved outcome
        transaction.setStatus(STATUS_APPROVED);
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Update FraudAlert review metadata
        LocalDateTime now = LocalDateTime.now();
        List<FraudAlert> alerts = fraudAlertRepository.findByTransaction(transaction);
        if (!alerts.isEmpty()) {
            for (FraudAlert alert : alerts) {
                alert.setReviewingAdmin(adminUser);
                alert.setReviewedAt(now);
                alert.setDecision("APPROVED");
                alert.setReviewNotes(reviewNotes != null ? reviewNotes : "Approved by administrator");
                alert.setStatus("RESOLVED");
                fraudAlertRepository.save(alert);
            }
        } else {
            FraudAlert alert = new FraudAlert();
            alert.setAlertType("FRAUD_REVIEW");
            alert.setSeverity("MEDIUM");
            alert.setStatus("RESOLVED");
            alert.setCreatedAt(now);
            alert.setTransaction(transaction);
            alert.setReviewingAdmin(adminUser);
            alert.setReviewedAt(now);
            alert.setDecision("APPROVED");
            alert.setReviewNotes(reviewNotes != null ? reviewNotes : "Approved by administrator");
            fraudAlertRepository.save(alert);
        }

        return savedTransaction;
    }

    @Transactional
    public Transaction rejectTransaction(Long transactionId, User adminUser, String reviewNotes) {
        if (transactionId == null) {
            throw new TransactionException("Transaction ID is required.");
        }
        if (adminUser == null) {
            throw new TransactionException("Reviewing administrator is required.");
        }

        Transaction transaction = transactionRepository.findByIdWithLock(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found with ID: " + transactionId));

        if (!STATUS_PENDING_REVIEW.equals(transaction.getStatus())) {
            throw new TransactionException("Transaction #" + transactionId + " is not pending review (current status: " + transaction.getStatus() + ").");
        }

        Account senderAccount = transaction.getAccount();
        if (senderAccount != null && senderAccount.getId() != null) {
            accountRepository.findByIdWithLock(senderAccount.getId());
        }

        // Release the reservation exactly once
        if (transaction.getTransactionType() != TransactionType.CASH_IN && fundsReservationRepository != null) {
            FundsReservation reservation = fundsReservationRepository.findByTransactionWithLock(transaction).orElse(null);
            if (reservation != null) {
                if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                    throw new TransactionException("Funds reservation for transaction #" + transactionId + " is not active (status: " + reservation.getStatus() + ").");
                }
                reservation.setStatus(ReservationStatus.RELEASED);
                reservation.setUpdatedAt(LocalDateTime.now());
                reservation.setNotes("Released upon administrator rejection");
                fundsReservationRepository.save(reservation);
            }
        }

        // Leave balances unchanged and decline transaction
        transaction.setStatus(STATUS_DECLINED);
        Transaction savedTransaction = transactionRepository.save(transaction);

        LocalDateTime now = LocalDateTime.now();
        List<FraudAlert> alerts = fraudAlertRepository.findByTransaction(transaction);
        if (!alerts.isEmpty()) {
            for (FraudAlert alert : alerts) {
                alert.setReviewingAdmin(adminUser);
                alert.setReviewedAt(now);
                alert.setDecision("REJECTED");
                alert.setReviewNotes(reviewNotes != null ? reviewNotes : "Rejected by administrator");
                alert.setStatus("CLOSED");
                fraudAlertRepository.save(alert);
            }
        } else {
            FraudAlert alert = new FraudAlert();
            alert.setAlertType("FRAUD_REVIEW");
            alert.setSeverity("HIGH");
            alert.setStatus("CLOSED");
            alert.setCreatedAt(now);
            alert.setTransaction(transaction);
            alert.setReviewingAdmin(adminUser);
            alert.setReviewedAt(now);
            alert.setDecision("REJECTED");
            alert.setReviewNotes(reviewNotes != null ? reviewNotes : "Rejected by administrator");
            fraudAlertRepository.save(alert);
        }

        return savedTransaction;
    }

    private void refreshEntityIfManaged(Object entity) {
        if (entityManager != null && entity != null) {
            try {
                if (entityManager.contains(entity)) {
                    entityManager.refresh(entity);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new TransactionException("Transaction amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Transaction amount must be greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new TransactionException("Transaction amount cannot have more than two decimal places.");
        }
    }

    private void validateAccountStatus(Account account) {
        if (account == null) {
            throw new TransactionException("Account not found.");
        }
        if (account.getStatus() != null && !"ACTIVE".equalsIgnoreCase(account.getStatus().trim())) {
            throw new TransactionException("Account is not active (" + account.getStatus().trim() + ").");
        }
    }

    private FraudPredictionResult predict(Transaction transaction) {
        try {
            FraudPredictionResult result = fraudPredictionService.predict(transaction);
            if (result == null) {
                throw new TransactionException("Fraud prediction could not be completed.");
            }
            return result;
        } catch (RuntimeException exception) {
            if (exception instanceof TransactionException) {
                throw exception;
            }
            throw new TransactionException("Fraud prediction could not be completed.", exception);
        }
    }

    private void applyTransactionOutcome(
            Transaction transaction,
            Account senderAccount,
            Account receiverAccount,
            BigDecimal senderBalance,
            FraudPredictionResult predictionResult,
            boolean ruleAnomaly) {

        if (predictionResult == null) {
            throw new TransactionException("Fraud prediction could not be completed.");
        }

        BigDecimal activeHolds = getReservedFunds(senderAccount);
        BigDecimal currentSenderBal = senderAccount.getBalance() != null ? senderAccount.getBalance() : senderBalance;
        BigDecimal availableBalance = currentSenderBal.subtract(activeHolds);

        boolean isFraud = "FRAUD".equals(predictionResult.getPrediction()) || ruleAnomaly;

        if (isFraud) {
            // Outflows cannot be reserved if available balance is insufficient
            if (transaction.getTransactionType() != TransactionType.CASH_IN && availableBalance.compareTo(transaction.getAmount()) < 0) {
                transaction.setStatus(STATUS_DECLINED);
                return;
            }
            transaction.setStatus(STATUS_PENDING_REVIEW);
            return;
        }

        if (!"LEGITIMATE".equals(predictionResult.getPrediction())) {
            throw new TransactionException("Fraud prediction returned an unsupported result.");
        }

        BigDecimal limit = transaction.getTransactionType().getTransactionLimit();
        if (transaction.getAmount().compareTo(limit) > 0) {
            transaction.setStatus(STATUS_DECLINED);
            return;
        }

        if (transaction.getTransactionType() == TransactionType.CASH_IN) {
            senderAccount.setBalance(senderBalance.add(transaction.getAmount()));
            accountRepository.save(senderAccount);
            transaction.setStatus(STATUS_APPROVED);
            return;
        }

        if (availableBalance.compareTo(transaction.getAmount()) < 0) {
            transaction.setStatus(STATUS_DECLINED);
            return;
        }

        BigDecimal newBalance = currentSenderBal.subtract(transaction.getAmount());
        if (newBalance.signum() < 0) {
            transaction.setStatus(STATUS_DECLINED);
            return;
        }

        senderAccount.setBalance(newBalance);
        accountRepository.save(senderAccount);
        transaction.setStatus(STATUS_APPROVED);

        if (transaction.getTransactionType() == TransactionType.TRANSFER && receiverAccount != null) {
            validateAccountStatus(receiverAccount);
            BigDecimal receiverNewBalance = receiverAccount.getBalance().add(transaction.getAmount());
            receiverAccount.setBalance(receiverNewBalance);
            accountRepository.save(receiverAccount);
        }
    }

    private FraudPrediction savePrediction(
            Transaction transaction,
            FraudPredictionResult predictionResult,
            boolean ruleAnomaly,
            String detectionReason) {

        FraudPrediction fraudPrediction = new FraudPrediction();
        fraudPrediction.setPrediction(predictionResult.getPrediction());
        fraudPrediction.setProbability(predictionResult.getProbability());
        fraudPrediction.setModelName(predictionResult.getModelName());
        fraudPrediction.setPredictionTime(LocalDateTime.now());
        fraudPrediction.setTransaction(transaction);
        fraudPrediction.setRuleAnomaly(ruleAnomaly);
        fraudPrediction.setDetectionReason(detectionReason);

        FraudPrediction savedPrediction = fraudPredictionRepository.save(fraudPrediction);
        if (savedPrediction != null) {
            savedPrediction.setRuleAnomaly(ruleAnomaly);
            savedPrediction.setDetectionReason(detectionReason);
            return savedPrediction;
        }
        return fraudPrediction;
    }

    private String determineDetectionReason(
            Transaction transaction,
            FraudPredictionResult predictionResult,
            boolean ruleAnomaly) {

        String status = transaction != null ? transaction.getStatus() : null;
        boolean mlFraud = predictionResult != null && "FRAUD".equals(predictionResult.getPrediction());

        if (STATUS_PENDING_REVIEW.equals(status)) {
            if (mlFraud && ruleAnomaly) {
                return "Machine Learning & Behavioral Anomaly";
            } else if (mlFraud) {
                return "Machine Learning Fraud Detection";
            } else if (ruleAnomaly) {
                return "Behavioral Anomaly / Suspicious Activity";
            }
            return "Security Policy Violation";
        }

        if (STATUS_DECLINED.equals(status)) {
            if (transaction != null && transaction.getAmount() != null
                    && transaction.getTransactionType() != null
                    && transaction.getAmount().compareTo(transaction.getTransactionType().getTransactionLimit()) > 0) {
                return "Limit Exceeded";
            }
            return "Balance Insufficient";
        }

        return "Normal / Legitimate";
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
