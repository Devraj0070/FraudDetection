package com.frauddetection.frauddetection.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@fraudguard.internal");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            User savedAdmin = userRepository.save(admin);

            Account adminAccount = new Account();
            adminAccount.setAccountNumber(String.format("1000000000%02d", savedAdmin.getId()));
            adminAccount.setAccountType("CORPORATE");
            adminAccount.setBalance(new BigDecimal("1000000.00"));
            adminAccount.setStatus("ACTIVE");
            adminAccount.setUser(savedAdmin);
            accountRepository.save(adminAccount);

            log.info("Initialized default ADMIN account: username='admin'");
        }

        if (userRepository.findByUsername("user").isEmpty()) {
            User demoUser = new User();
            demoUser.setUsername("user");
            demoUser.setEmail("user@fraudguard.internal");
            demoUser.setPassword(passwordEncoder.encode("user123"));
            demoUser.setRole("USER");
            User savedUser = userRepository.save(demoUser);

            Account userAccount = new Account();
            userAccount.setAccountNumber(String.format("1000000000%02d", savedUser.getId()));
            userAccount.setAccountType("SAVINGS");
            userAccount.setBalance(new BigDecimal("50000.00"));
            userAccount.setStatus("ACTIVE");
            userAccount.setUser(savedUser);
            accountRepository.save(userAccount);

            log.info("Initialized default demo USER account: username='user'");
        }
    }
}
