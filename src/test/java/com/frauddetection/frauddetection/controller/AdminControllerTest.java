package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private Model model;

    @InjectMocks
    private AdminController controller;

    @Test
    void showsAlertsNewestFirst() {
        FraudAlert newest = new FraudAlert();
        FraudAlert older = new FraudAlert();

        when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(newest, older));

        String view = controller.showAlerts(model);

        assertEquals("admin/alerts", view);
        verify(fraudAlertRepository).findAllByOrderByCreatedAtDesc();
        verify(model).addAttribute("alerts", List.of(newest, older));
    }
}
