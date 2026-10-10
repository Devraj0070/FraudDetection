package com.frauddetection.frauddetection.fraud;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Component
public class FraudRuleEngine {

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("50000.00");
    private static final BigDecimal EXTREME_AMOUNT_THRESHOLD = new BigDecimal("500000.00");
    private static final BigDecimal DAILY_CUMULATIVE_VOLUME_THRESHOLD = new BigDecimal("300000.00");
    private static final int ESTABLISHED_HISTORY_THRESHOLD = 3;

    private final TransactionRepository transactionRepository;

    public FraudRuleEngine(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public static class RuleEvaluationResult {
        private final int score;
        private final boolean suspicious;
        private final RiskLevel riskLevel;
        private final String primaryReason;
        private final List<String> triggeredSignals;
        private final boolean insufficientHistory;

        public RuleEvaluationResult(int score, boolean suspicious, RiskLevel riskLevel,
                                    String primaryReason, List<String> triggeredSignals,
                                    boolean insufficientHistory) {
            this.score = score;
            this.suspicious = suspicious;
            this.riskLevel = riskLevel;
            this.primaryReason = primaryReason;
            this.triggeredSignals = Collections.unmodifiableList(new ArrayList<>(triggeredSignals));
            this.insufficientHistory = insufficientHistory;
        }

        public int getScore() {
            return score;
        }

        public boolean isSuspicious() {
            return suspicious;
        }

        public RiskLevel getRiskLevel() {
            return riskLevel;
        }

        public String getPrimaryReason() {
            return primaryReason;
        }

        public List<String> getTriggeredSignals() {
            return triggeredSignals;
        }

        public boolean isInsufficientHistory() {
            return insufficientHistory;
        }
    }

    /**
     * Evaluates comprehensive behavioral anomaly patterns for a transaction.
     * Evaluates multi-window velocity, cumulative volume, spending baseline deviation,
     * failure rates (declined/blocked), and structuring patterns using real history.
     */
    public RuleEvaluationResult evaluate(Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            return new RuleEvaluationResult(0, false, RiskLevel.LOW,
                    "Invalid or missing transaction data", Collections.emptyList(), true);
        }

        BigDecimal amount = transaction.getAmount();
        Account account = transaction.getAccount();

        if (account == null) {
            return new RuleEvaluationResult(0, false, RiskLevel.LOW,
                    "No account history available", Collections.emptyList(), true);
        }

        LocalDateTime transactionTime = transaction.getTransactionTime() != null
                ? transaction.getTransactionTime()
                : LocalDateTime.now();

        LocalDateTime tenMinutesAgo = transactionTime.minusMinutes(10);
        LocalDateTime oneHourAgo = transactionTime.minusHours(1);
        LocalDateTime twentyFourHoursAgo = transactionTime.minusHours(24);
        LocalDateTime sevenDaysAgo = transactionTime.minusDays(7);

        List<Transaction> history = (transactionRepository != null)
                ? transactionRepository.findByAccountOrderByTransactionTimeDesc(account)
                : Collections.emptyList();

        int recent10mCount = 0;
        int recent10mLargeCount = 0;
        int dailyCount = 0;
        BigDecimal dailyVolume = BigDecimal.ZERO;
        int recent1hDeclinedOrBlockedCount = 0;
        int dailyDeclinedOrBlockedCount = 0;
        int weeklyCount = 0;
        int nearLimit24hCount = 0;

        int totalApprovedCount = 0;
        BigDecimal totalApprovedVolume = BigDecimal.ZERO;
        BigDecimal maxApprovedAmount = BigDecimal.ZERO;

        BigDecimal currentLimit = transaction.getTransactionType() != null
                ? transaction.getTransactionType().getTransactionLimit()
                : null;
        boolean isCurrentNearLimit = currentLimit != null && currentLimit.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(currentLimit.multiply(new BigDecimal("0.95"))) >= 0
                && amount.compareTo(currentLimit) <= 0;

        for (Transaction previous : history) {
            if (previous == null) {
                continue;
            }
            if (previous.getId() != null && transaction.getId() != null
                    && previous.getId().equals(transaction.getId())) {
                continue;
            }

            LocalDateTime prevTime = previous.getTransactionTime();
            BigDecimal prevAmount = previous.getAmount() != null ? previous.getAmount() : BigDecimal.ZERO;
            String prevStatus = previous.getStatus();

            // Approved history for baseline profiling
            if ("APPROVED".equalsIgnoreCase(prevStatus)) {
                totalApprovedCount++;
                totalApprovedVolume = totalApprovedVolume.add(prevAmount);
                if (prevAmount.compareTo(maxApprovedAmount) > 0) {
                    maxApprovedAmount = prevAmount;
                }
            }

            // Status checks for failure patterns (DECLINED or BLOCKED)
            boolean isFailed = "DECLINED".equalsIgnoreCase(prevStatus) || "BLOCKED".equalsIgnoreCase(prevStatus);

            if (prevTime != null) {
                // 10-minute rapid window
                if (prevTime.isAfter(tenMinutesAgo) && !prevTime.isAfter(transactionTime)) {
                    recent10mCount++;
                    if (prevAmount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
                        recent10mLargeCount++;
                    }
                }

                // 1-hour window for failures
                if (prevTime.isAfter(oneHourAgo) && !prevTime.isAfter(transactionTime)) {
                    if (isFailed) {
                        recent1hDeclinedOrBlockedCount++;
                    }
                }

                // 24-hour daily window
                if (prevTime.isAfter(twentyFourHoursAgo) && !prevTime.isAfter(transactionTime)) {
                    dailyCount++;
                    dailyVolume = dailyVolume.add(prevAmount);
                    if (isFailed) {
                        dailyDeclinedOrBlockedCount++;
                    }
                    if (previous.getTransactionType() != null && previous.getTransactionType().getTransactionLimit() != null) {
                        BigDecimal prevLimit = previous.getTransactionType().getTransactionLimit();
                        if (prevAmount.compareTo(prevLimit.multiply(new BigDecimal("0.95"))) >= 0
                                && prevAmount.compareTo(prevLimit) <= 0) {
                            nearLimit24hCount++;
                        }
                    }
                }

                // 7-day weekly window
                if (prevTime.isAfter(sevenDaysAgo) && !prevTime.isAfter(transactionTime)) {
                    weeklyCount++;
                }
            }
        }

        boolean insufficientHistory = totalApprovedCount < ESTABLISHED_HISTORY_THRESHOLD;
        List<String> signals = new ArrayList<>();
        int score = 0;

        // Pattern 1: Extreme rapid velocity (>= 10 in 10 minutes) -> automated script/bot attack
        if (recent10mCount >= 10) {
            score += 80;
            signals.add("Extreme rapid transaction velocity (>=10 transactions in 10 minutes)");
        }
        // Pattern 2: High velocity (>= 5 in 10 min) coupled with large amount (>= ₹50,000)
        else if (recent10mCount >= 5 && amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
            score += 80;
            signals.add("High velocity (>=5 in 10 min) coupled with large amount (>=₹50,000)");
        }
        // Pattern 3: Repeated large transactions (>= 2 prior large in 10 min + current large txn)
        else if (recent10mLargeCount >= 2 && amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
            score += 80;
            signals.add("Repeated large transactions (>=2 prior large in 10 min)");
        }
        // Pattern 4: Extreme outlier amount spike (>= ₹500,000) with prior rapid activity
        else if (amount.compareTo(EXTREME_AMOUNT_THRESHOLD) >= 0 && recent10mCount >= 2) {
            score += 80;
            signals.add("Extreme amount spike (>=₹500,000) with prior rapid activity");
        }

        // Pattern 5: Repeated Recent Declined or Blocked Attempts (card testing / brute force)
        if (recent1hDeclinedOrBlockedCount >= 3) {
            score += 80;
            signals.add("Repeated declined/blocked attempts in recent activity (>=3 in last hour)");
        } else if (dailyDeclinedOrBlockedCount >= 5) {
            score += 80;
            signals.add("Excessive declined/blocked attempts across 24 hours (>=5 attempts)");
        } else if (dailyDeclinedOrBlockedCount >= 3) {
            score += 40;
            signals.add("Multiple declined/blocked attempts in last 24 hours (>=3 attempts)");
        }

        // Pattern 6: Cumulative 24-hour volume / frequency drain
        if (dailyVolume.add(amount).compareTo(DAILY_CUMULATIVE_VOLUME_THRESHOLD) > 0 && dailyCount >= 5) {
            score += 80;
            signals.add("Excessive cumulative 24-hour volume (>₹300,000 across multiple transactions)");
        } else if (dailyCount >= 20) {
            score += 80;
            signals.add("Excessive 24-hour transaction frequency (>=20 transactions)");
        } else if (dailyCount >= 12) {
            score += 40;
            signals.add("Elevated 24-hour transaction frequency (>=12 transactions)");
        }

        // Pattern 7: Deviation from established account spending baseline
        if (!insufficientHistory && totalApprovedCount > 0) {
            BigDecimal averageHistoricalAmount = totalApprovedVolume.divide(
                    BigDecimal.valueOf(totalApprovedCount), 2, RoundingMode.HALF_UP
            );

            if (averageHistoricalAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (amount.compareTo(averageHistoricalAmount.multiply(BigDecimal.valueOf(10))) > 0
                        && amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0
                        && amount.compareTo(maxApprovedAmount.multiply(BigDecimal.valueOf(2))) > 0) {
                    score += 80;
                    signals.add("Extreme deviation from account spending baseline (>10x historical average)");
                } else if (amount.compareTo(averageHistoricalAmount.multiply(BigDecimal.valueOf(5))) > 0
                        && amount.compareTo(new BigDecimal("30000.00")) >= 0) {
                    score += 35;
                    signals.add("Significant deviation from account spending baseline (>5x historical average)");
                }
            }
        }

        // Pattern 8: Structuring / Smurfing (repeated transactions just below configured limits)
        if (isCurrentNearLimit && nearLimit24hCount >= 2) {
            score += 80;
            signals.add("Repeated structuring pattern (multiple transactions just below limits in 24h)");
        } else if (isCurrentNearLimit && nearLimit24hCount >= 1) {
            score += 35;
            signals.add("Potential structuring (transaction just below limit)");
        }

        // Pattern 9: 7-day sustained velocity
        if (weeklyCount >= 50) {
            score += 40;
            signals.add("Elevated 7-day transaction velocity (>=50 transactions in 7 days)");
        }

        // Moderate indicators if no high-risk pattern triggered
        if (score == 0) {
            if (recent10mCount >= 5) {
                score += 40;
                signals.add("Moderate velocity in 10 minutes (>=5 transactions)");
            } else if (recent10mLargeCount >= 1 && amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
                score += 30;
                signals.add("Large transaction preceded by a recent large transaction");
            }
        }

        int finalScore = Math.min(score, 100);
        boolean isSuspicious = finalScore >= 80;
        RiskLevel riskLevel = getRiskLevel(finalScore);

        String primaryReason = signals.isEmpty()
                ? (insufficientHistory ? "Normal activity (New account / Insufficient history)" : "Normal transaction pattern")
                : signals.get(0);

        return new RuleEvaluationResult(finalScore, isSuspicious, riskLevel, primaryReason, signals, insufficientHistory);
    }

    public int calculateRuleScore(Transaction transaction) {
        return evaluate(transaction).getScore();
    }

    public boolean isSuspicious(Transaction transaction) {
        return evaluate(transaction).isSuspicious();
    }

    public RiskLevel getRiskLevel(int score) {
        if (score <= 30) {
            return RiskLevel.LOW;
        } else if (score <= 60) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }
}
