package com.frauddetection.frauddetection.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class FraudSecurityAuditFilterTest {

    @Test
    void shouldAttachSecurityHeadersAndMeasureLatency() throws Exception {
        FraudSecurityAuditFilter filter = new FraudSecurityAuditFilter();
        filter.init(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/transaction");
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("User-Agent", "JUnit-Test-Client");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        // Verify headers
        assertEquals("FraudGuard-v1.0", response.getHeader("X-Fraud-Audit-Tracker"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertNotNull(response.getHeader("X-Execution-Time-Millis"));

        filter.destroy();
    }
}
