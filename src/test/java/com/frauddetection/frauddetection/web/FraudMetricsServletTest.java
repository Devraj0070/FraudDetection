package com.frauddetection.frauddetection.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class FraudMetricsServletTest {

    @Test
    void shouldReturnJsonTelemetryMetrics() throws Exception {
        FraudMetricsServlet servlet = new FraudMetricsServlet();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/metrics");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));

        String body = response.getContentAsString();
        assertNotNull(body);
        assertTrue(body.contains("\"service\": \"Fraud Detection System\""));
        assertTrue(body.contains("\"status\": \"OPERATIONAL\""));
        assertTrue(body.contains("\"servlet\": \"FraudMetricsServlet\""));
        assertTrue(body.contains("\"engine\""));
    }
}
