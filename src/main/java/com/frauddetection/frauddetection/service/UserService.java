package com.frauddetection.frauddetection.service;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.ApplicationException;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

@Service
public class UserService {

    public static final BigDecimal INITIAL_OPENING_BALANCE = new BigDecimal("50000.00");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user) {
        if (user == null) {
            throw new ApplicationException("User details are required.");
        }

        String username = user.getUsername() != null ? user.getUsername().trim() : "";
        if (username.length() < 3 || username.length() > 50) {
            throw new ApplicationException("Username must be between 3 and 50 characters.");
        }

        String email = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ApplicationException("Please provide a valid email address.");
        }

        String rawPassword = user.getPassword();
        if (rawPassword == null || rawPassword.trim().length() < 6) {
            throw new ApplicationException("Password must be at least 6 characters in length.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ApplicationException("Username '" + username + "' is already taken.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException("An account with email '" + email + "' already exists.");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));

        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("USER");
        }

        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setAccountNumber(
                String.format("1000000000%02d", savedUser.getId())
        );
        account.setAccountType("SAVINGS");
        account.setBalance(INITIAL_OPENING_BALANCE);
        account.setStatus("ACTIVE");
        account.setUser(savedUser);

        accountRepository.save(account);

        return savedUser;
    }
}