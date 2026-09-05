package com.shiksha.erp.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ResourceNotFoundException.class,
            EntityNotFoundException.class,
            NoResourceFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(Exception ex, HttpServletRequest request, Model model) {
        log.warn("Resource not found: {} - {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "The requested page or record could not be found.");
        model.addAttribute("requestedUrl", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler({
            UnauthorizedAccessException.class,
            AccessDeniedException.class
    })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDeniedException(Exception ex, HttpServletRequest request, Model model) {
        log.warn("Access denied for URI {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Access Denied: You do not have permission to view or modify this resource.");
        model.addAttribute("requestedUrl", request.getRequestURI());
        return "error/403";
    }

    @ExceptionHandler({
            BusinessValidationException.class,
            DuplicateRecordException.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
            MaxUploadSizeExceededException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequestException(Exception ex, HttpServletRequest request, Model model) {
        log.warn("Bad request / validation failure at {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Invalid input or operation could not be processed.");
        model.addAttribute("requestedUrl", request.getRequestURI());
        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Internal server error occurred at URL: {}", request.getRequestURI(), ex);
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later or contact the administrator.");
        model.addAttribute("requestedUrl", request.getRequestURI());
        return "error/500";
    }
}
