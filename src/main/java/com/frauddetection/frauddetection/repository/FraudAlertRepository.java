package com.frauddetection.frauddetection.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.Transaction;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByTransaction(Transaction transaction);

    List<FraudAlert> findAllByOrderByCreatedAtDesc();
}