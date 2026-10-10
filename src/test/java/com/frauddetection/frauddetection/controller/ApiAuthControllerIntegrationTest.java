package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.CsrfResponse;
import com.frauddetection.frauddetection.dto.LoginRequest;
import com.frauddetection.frauddetection.dto.RegisterRequest;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.UserService;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test verifying:
 * 1. CSRF token resolution via /api/auth/csrf and XSRF-TOKEN cookie generation.
 * 2. Rejection of unauthenticated POST requests lacking valid CSRF tokens (HTTP 403).
 * 3. Successful user registration with CSRF protection.
 * 4. Programmatic authentication establishing JSESSIONID.
 * 5. Session-authenticated /api/auth/me endpoint verification.
 * 6. Session termination and invalidation via /api/auth/logout.
 */
@SpringBootTest
class ApiAuthControllerIntegrationTest {

    private static final Set<String> TEST_USERNAMES = Set.of("swing_test_user", "swing_reg_user");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanTestUsers();
    }

    @AfterEach
    void tearDown() {
        cleanTestUsers();
    }

    private void cleanTestUsers() {
        for (String uname : TEST_USERNAMES) {
            userRepository.findByUsername(uname).ifPresent(u -> {
                accountRepository.findByUser(u).forEach(accountRepository::delete);
                userRepository.delete(u);
            });
        }
    }

    @Test
    @DisplayName("GET /api/auth/csrf returns valid CSRF token and sets XSRF-TOKEN cookie")
    void getCsrfToken_returnsValidTokenAndSetsCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        CsrfResponse csrfResponse = objectMapper.readValue(body, CsrfResponse.class);
        assertNotNull(csrfResponse.token(), "Token must not be null");
        assertFalse(csrfResponse.token().isBlank(), "Token must not be blank");

        Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(xsrfCookie, "XSRF-TOKEN cookie must be set by CookieCsrfTokenRepository");
    }

    @Test
    @DisplayName("POST /api/auth/login without CSRF token is rejected with 403 Forbidden")
    void login_withoutCsrfToken_isForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest("some_user", "some_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid CSRF token is rejected with 403 Forbidden")
    void login_withInvalidCsrfToken_isForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest("some_user", "some_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-CSRF-TOKEN", "invalid-token-12345")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/register without CSRF token is rejected with 403 Forbidden")
    void register_withoutCsrfToken_isForbidden() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("swing_reg_user", "reg@test.com", "Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Registration, Login, Session Verification and Logout complete lifecycle")
    void fullAuthLifecycle_succeedsWithCsrfAndSession() throws Exception {
        // Step 1: Fetch CSRF token
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        CsrfResponse csrf = objectMapper.readValue(csrfResult.getResponse().getContentAsString(), CsrfResponse.class);
        Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        // Step 2: Register new user with valid CSRF header and cookie
        RegisterRequest regRequest = new RegisterRequest("swing_reg_user", "reg@test.com", "Password@123");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(xsrfCookie)
                        .header(csrf.headerName(), csrf.token())
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("swing_reg_user"))
                .andReturn();

        AuthResponse regAuth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        assertTrue(regAuth.success());
        assertNotNull(regAuth.accountNumber(), "Account number must be generated upon registration");

        // Step 3: Attempt login with invalid password -> 401 Unauthorized
        LoginRequest badLogin = new LoginRequest("swing_reg_user", "WrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(xsrfCookie)
                        .header(csrf.headerName(), csrf.token())
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        // Step 4: Login with correct credentials -> 200 OK + Session
        LoginRequest goodLogin = new LoginRequest("swing_reg_user", "Password@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(xsrfCookie)
                        .header(csrf.headerName(), csrf.token())
                        .content(objectMapper.writeValueAsString(goodLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("swing_reg_user"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "HTTP session must be established upon successful login");

        // Step 5: Access /api/auth/me using the authenticated session
        mockMvc.perform(get("/api/auth/me")
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("swing_reg_user"));

        // Step 6: Logout with session and CSRF
        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(xsrfCookie)
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 7: Access /api/auth/me after logout -> 401 Unauthorized
        mockMvc.perform(get("/api/auth/me")
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Duplicate registration returns 400 Bad Request")
    void duplicateRegistration_returnsBadRequest() throws Exception {
        // Create initial user
        User user = new User();
        user.setUsername("swing_test_user");
        user.setEmail("swing_test@example.com");
        user.setPassword("Secret@123");
        user.setRole("USER");
        userService.registerUser(user);

        // Fetch CSRF
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        CsrfResponse csrf = objectMapper.readValue(csrfResult.getResponse().getContentAsString(), CsrfResponse.class);
        Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        // Attempt duplicate registration
        RegisterRequest duplicate = new RegisterRequest("swing_test_user", "another@example.com", "Password@123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(xsrfCookie)
                        .header(csrf.headerName(), csrf.token())
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isString());
    }
}
