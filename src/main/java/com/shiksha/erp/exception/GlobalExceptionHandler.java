package com.shiksha.erp.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(Exception ex, HttpServletRequest request, Model model) {
        log.warn("Resource not found: {} - {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "The requested page or record could not be found.");
        model.addAttribute("requestedUrl", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Internal server error occurred at URL: {}", request.getRequestURI(), ex);
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Something went wrong on our end. Please try again.");
        model.addAttribute("requestedUrl", request.getRequestURI());
        return "error/500";
    }
}
