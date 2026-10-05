package com.frauddetection.frauddetection.fraud.feature;

public class FeatureVector {

    private double amount;
    private int transactionHour;
    private int transactionDayOfWeek;
    private int recentTransactionCount;
    private double averageTransactionAmount;
    private double amountDifferenceFromAverage;

    public FeatureVector() {
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getTransactionHour() {
        return transactionHour;
    }

    public void setTransactionHour(int transactionHour) {
        this.transactionHour = transactionHour;
    }

    public int getTransactionDayOfWeek() {
        return transactionDayOfWeek;
    }

    public void setTransactionDayOfWeek(int transactionDayOfWeek) {
        this.transactionDayOfWeek = transactionDayOfWeek;
    }

    public int getRecentTransactionCount() {
        return recentTransactionCount;
    }

    public void setRecentTransactionCount(int recentTransactionCount) {
        this.recentTransactionCount = recentTransactionCount;
    }

    public double getAverageTransactionAmount() {
        return averageTransactionAmount;
    }

    public void setAverageTransactionAmount(double averageTransactionAmount) {
        this.averageTransactionAmount = averageTransactionAmount;
    }

    public double getAmountDifferenceFromAverage() {
        return amountDifferenceFromAverage;
    }

    public void setAmountDifferenceFromAverage(double amountDifferenceFromAverage) {
        this.amountDifferenceFromAverage = amountDifferenceFromAverage;
    }
}