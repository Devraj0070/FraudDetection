package com.frauddetection.frauddetection.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.CsrfResponse;
import com.frauddetection.frauddetection.dto.LoginRequest;
import com.frauddetection.frauddetection.dto.RegisterRequest;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.ApplicationException;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public ApiAuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserService userService,
            UserRepository userRepository,
            AccountRepository accountRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Resolves the active CSRF token, triggering deferred resolution so that
     * the XSRF-TOKEN cookie is generated and written to the response.
     */
    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }
        if (csrfToken != null) {
            // Invoking getToken() evaluates deferred token and triggers cookie serialization
            String token = csrfToken.getToken();
            return ResponseEntity.ok(new CsrfResponse(
                    token,
                    csrfToken.getHeaderName(),
                    csrfToken.getParameterName()
            ));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Programmatic session login for REST and Swing desktop clients.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (loginRequest == null || loginRequest.username() == null || loginRequest.password() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(
                    false,
                    "Username and password are required.",
                    null, null, null, null
            ));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username().trim(),
                            loginRequest.password()
                    )
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            Account account = user != null
                    ? accountRepository.findTopByUserOrderByIdAsc(user).orElse(null)
                    : null;

            String role = (user != null && user.getRole() != null) ? user.getRole() : "USER";
            String email = (user != null && user.getEmail() != null) ? user.getEmail() : "";
            String accountNumber = (account != null && account.getAccountNumber() != null)
                    ? account.getAccountNumber()
                    : "";

            return ResponseEntity.ok(new AuthResponse(
                    true,
                    "Login successful",
                    authentication.getName(),
                    email,
                    role,
                    accountNumber
            ));

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(
                    false,
                    "Invalid username or password.",
                    null, null, null, null
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(
                    false,
                    "Authentication failure: " + ex.getMessage(),
                    null, null, null, null
            ));
        }
    }

    /**
     * User registration for desktop and REST clients.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        if (registerRequest == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(
                    false,
                    "Registration details are required.",
                    null, null, null, null
            ));
        }

        try {
            User user = new User();
            user.setUsername(registerRequest.username());
            user.setEmail(registerRequest.email());
            user.setPassword(registerRequest.password());
            user.setRole("USER");

            User savedUser = userService.registerUser(user);
            Account account = accountRepository.findTopByUserOrderByIdAsc(savedUser).orElse(null);
            String accountNumber = (account != null && account.getAccountNumber() != null)
                    ? account.getAccountNumber()
                    : "";

            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                    true,
                    "Registration successful! Please sign in with your credentials.",
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getRole(),
                    accountNumber
            ));

        } catch (ApplicationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(
                    false,
                    ex.getMessage(),
                    null, null, null, null
            ));
        }
    }

    /**
     * Session logout for desktop and REST clients.
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new AuthResponse(
                true,
                "Logged out successfully.",
                null, null, null, null
        ));
    }

    /**
     * Current authenticated session state check.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(
                    false,
                    "Not authenticated",
                    null, null, null, null
            ));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        Account account = user != null
                ? accountRepository.findTopByUserOrderByIdAsc(user).orElse(null)
                : null;

        String role = (user != null && user.getRole() != null) ? user.getRole() : "USER";
        String email = (user != null && user.getEmail() != null) ? user.getEmail() : "";
        String accountNumber = (account != null && account.getAccountNumber() != null)
                ? account.getAccountNumber()
                : "";

        return ResponseEntity.ok(new AuthResponse(
                true,
                "Authenticated",
                authentication.getName(),
                email,
                role,
                accountNumber
        ));
    }
}
