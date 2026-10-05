package com.frauddetection.frauddetection.fraud;

import org.springframework.stereotype.Component;

@Component
public class RiskScoreCalculator {

    public int calculateRiskScore(double amount) {

        int score = 0;

        if (amount >= 100000) {
            score += 60;
        } else if (amount >= 50000) {
            score += 40;
        } else if (amount >= 10000) {
            score += 20;
        }

        return Math.min(score, 100);
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