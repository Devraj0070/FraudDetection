package com.frauddetection.frauddetection.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransactionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTransactionException(
            TransactionException exception,
            Model model) {

        return errorPage(model, exception.getMessage());
    }

    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleApplicationException(
            ApplicationException exception,
            Model model) {

        return errorPage(model, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleArgumentTypeMismatch(Model model) {
        return errorPage(model, "Amount must be a valid number.");
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleDataAccessException(Model model) {
        return errorPage(model, "The transaction could not be completed. Please try again.");
    }

    private String errorPage(Model model, String message) {
        model.addAttribute("errorTitle", "Transaction Notice");
        model.addAttribute("errorMessage", message);
        return "error";
    }
}
