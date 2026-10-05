package com.frauddetection.frauddetection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.User;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    Optional<Account> findByAccountNumber(String accountNumber);
}