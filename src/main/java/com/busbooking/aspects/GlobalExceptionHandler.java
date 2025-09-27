package com.busbooking.aspects;

import com.busbooking.exceptions.ConcurrencyException;
import com.busbooking.exceptions.PaymentFailureException;
import com.busbooking.exceptions.ResourceNotFoundException;
import com.busbooking.exceptions.SeatUnavailableException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Enterprise Error Handling Strategy: Global exception handler for consistent error responses. */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Handles Jakarta Bean Validation errors
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", "Validation Error");

        // Get all validation errors
        List<String> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());

        body.put("messages", errors);
        log.warn("Validation failure: {}", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /** Handles custom ResourceNotFoundException (404 Not Found). */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        Map<String, Object> body = getErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles custom SeatUnavailableException and ConcurrencyException (409 Conflict/400 Bad
     * Request).
     */
    @ExceptionHandler({SeatUnavailableException.class, ConcurrencyException.class})
    public ResponseEntity<Object> handleConcurrencyAndSeatException(
            RuntimeException ex, WebRequest request) {
        HttpStatus status =
                ex instanceof ConcurrencyException ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        Map<String, Object> body = getErrorBody(status, ex.getMessage(), request);
        log.error("Booking error: {}", ex.getMessage());
        return new ResponseEntity<>(body, status);
    }

    /** Handles custom PaymentFailureException (500 Internal Server Error/400 Bad Request). */
    @ExceptionHandler(PaymentFailureException.class)
    public ResponseEntity<Object> handlePaymentFailureException(
            PaymentFailureException ex, WebRequest request) {
        Map<String, Object> body = getErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        log.error("Payment failure during confirmation: {}", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /** Handles custom PaymentFailureException (500 Internal Server Error/400 Bad Request). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handlePaymentFailureException(
            IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> body = getErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /** Generic structure for error response. */
    private Map<String, Object> getErrorBody(
            HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).substring(4));
        return body;
    }
}
