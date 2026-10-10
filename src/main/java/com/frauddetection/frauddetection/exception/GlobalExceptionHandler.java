package com.frauddetection.frauddetection.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Dual-purpose exception handler supporting both REST API JSON responses and HTML error pages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransactionException.class)
    public Object handleTransactionException(
            TransactionException exception,
            HttpServletRequest request,
            Model model) {

        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        return errorPage(model, exception.getMessage());
    }

    @ExceptionHandler(ApplicationException.class)
    public Object handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request,
            Model model) {

        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        return errorPage(model, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request,
            Model model) {

        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Amount must be a valid number.");
        }
        return errorPage(model, "Amount must be a valid number.");
    }

    @ExceptionHandler(DataAccessException.class)
    public Object handleDataAccessException(
            DataAccessException exception,
            HttpServletRequest request,
            Model model) {

        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "The transaction could not be completed. Please try again.");
        }
        return errorPage(model, "The transaction could not be completed. Please try again.");
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/api");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private String errorPage(Model model, String message) {
        if (model != null) {
            model.addAttribute("errorTitle", "Transaction Notice");
            model.addAttribute("errorMessage", message);
        }
        return "error";
    }
}
