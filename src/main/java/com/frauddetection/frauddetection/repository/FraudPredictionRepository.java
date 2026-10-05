package com.frauddetection.frauddetection.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;

public interface FraudPredictionRepository extends JpaRepository<FraudPrediction, Long> {

    Optional<FraudPrediction> findByTransaction(Transaction transaction);
}