package com.frauddetection.frauddetection.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;

@SpringBootTest
class AdminAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private FraudAlertRepository fraudAlertRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedUserCannotAccessAdminAlerts() throws Exception {
        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void userCannotAccessAdminAlerts() throws Exception {
        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAccessAdminAlerts() throws Exception {
        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/alerts"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAlertsPageRendersExistingAlerts() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("250.00"));
        transaction.setTransactionTime(LocalDateTime.of(2026, 10, 6, 12, 0));

        FraudAlert alert = new FraudAlert();
        alert.setAlertType("VELOCITY");
        alert.setSeverity("HIGH");
        alert.setStatus("OPEN");
        alert.setCreatedAt(LocalDateTime.of(2026, 10, 6, 12, 5));
        alert.setTransaction(transaction);

        Mockito.when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(alert));

        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/alerts"))
                .andExpect(model().attribute("alerts", List.of(alert)));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAlertsPageRendersWhenNoAlertsExist() throws Exception {
        Mockito.when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/alerts"))
                .andExpect(model().attribute("alerts", List.of()));
    }

    @Test
    void loginStaysPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerStaysPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void existingAuthenticatedUserAccessStillWorks() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void csrfProtectionIsStillEnabled() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }
}
