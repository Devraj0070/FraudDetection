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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;

@ExtendWith(MockitoExtension.class)
class ApiAdminControllerTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private ApiAdminController controller;

    @Test
    void showsAlertsNewestFirst() {
        FraudAlert newest = new FraudAlert();
        FraudAlert older = new FraudAlert();

        when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(newest, older));

        ResponseEntity<List<FraudAlert>> response = controller.getAlerts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(newest, older), response.getBody());
        verify(fraudAlertRepository).findAllByOrderByCreatedAtDesc();
    }
}
