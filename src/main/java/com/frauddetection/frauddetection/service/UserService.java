package com.frauddetection.frauddetection.service;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

@Service
public class UserService {

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

    public User registerUser(User user) {

        String encodedPassword =
                passwordEncoder.encode(user.getPassword());

        user.setPassword(encodedPassword);

        User savedUser = userRepository.save(user);

        Account account = new Account();

        account.setAccountNumber(
                String.format("1000000000%02d", savedUser.getId())
        );

        account.setAccountType("SAVINGS");
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("ACTIVE");
        account.setUser(savedUser);

        accountRepository.save(account);

        return savedUser;
    }
}