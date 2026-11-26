package com.frauddetection.frauddetection.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

/**
 * Seeds initial demo accounts and administrator credentials for local development,
 * demonstration, and test environments.
 *
 * Gated to 'dev', 'demo', 'default', and 'test' Spring profiles to strictly prevent
 * default hardcoded credentials from being initialized in production environments.
 */
@Component
@Profile({"dev", "demo", "default", "test", "h2"})
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder,
                           Environment environment) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    public DataInitializer(UserRepository userRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this(userRepository, accountRepository, passwordEncoder, null);
    }

    @Override
    public void run(String... args) {
        // Defense-in-depth: abort immediately if a production profile is active
        if (environment != null) {
            for (String profile : environment.getActiveProfiles()) {
                if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                    log.warn("Production profile ('{}') is active. Default credential seeding is strictly disabled.", profile);
                    return;
                }
            }
        }

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

            log.info("Initialized default ADMIN account for development/demo: username='admin'");
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
            userAccount.setBalance(new BigDecimal("100000.00"));
            userAccount.setStatus("ACTIVE");
            userAccount.setUser(savedUser);
            accountRepository.save(userAccount);

            log.info("Initialized default demo USER account for development/demo: username='user'");
        }
    }
}
