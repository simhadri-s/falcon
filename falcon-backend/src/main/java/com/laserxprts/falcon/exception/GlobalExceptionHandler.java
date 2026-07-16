package com.laserxprts.falcon.exception;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import org.springframework.dao.OptimisticLockingFailureException;
import com.laserxprts.falcon.dto.response.ApiErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, WebRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Endpoint not found", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException ex, WebRequest request) {
        return buildError(HttpStatus.CONFLICT, "This item was modified by someone else. Please refresh and try again.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String message = ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .findFirst()
            .orElse("Validation failed");
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Maximum upload size exceeded. Please ensure each file is under 5MB and total request size is under 5MB.", request);
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        MultipartException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, WebRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, defaultMessage(ex.getMessage(), "Invalid request"), request);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, WebRequest request) {
        return buildError(ex.getStatus(), defaultMessage(ex.getMessage(), ex.getStatus().getReasonPhrase()), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return buildError(status, defaultMessage(ex.getReason(), status.getReasonPhrase()), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(RuntimeException ex, WebRequest request) {
        HttpStatus status = inferStatus(ex.getMessage());
        String message = status.is5xxServerError()
            ? "An unexpected error occurred."
            : defaultMessage(ex.getMessage(), status.getReasonPhrase());

        if (status.is5xxServerError()) {
            log.error("Unhandled runtime exception", ex);
        }

        return buildError(status, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponseFactory.build(status, message, request));
    }

    private HttpStatus inferStatus(String message) {
        String normalized = defaultMessage(message, "").toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (containsAny(normalized, List.of("unauthorized", "unauthorised", "invalid email or password"))) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (containsAny(normalized, List.of("forbidden", "access denied"))) {
            return HttpStatus.FORBIDDEN;
        }

        if (containsAny(normalized, List.of("not found", "cannot find", "cannot be found", "doesn't exist", "does not exist", "not fund"))) {
            return HttpStatus.NOT_FOUND;
        }

        if (containsAny(normalized, List.of("already exists", "already exitst", "duplicate", "already cancelled", "cannot cancel", "cannot modify", "cannot delete"))) {
            return HttpStatus.CONFLICT;
        }

        if (containsAny(normalized, List.of(
            "invalid",
            "required",
            "empty",
            "null",
            "missing",
            "please upload",
            "please select",
            "only pdf",
            "only image",
            "validation failed",
            "delivery not available",
            "otp has expired"
        ))) {
            return HttpStatus.BAD_REQUEST;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean containsAny(String value, List<String> matches) {
        return matches.stream().anyMatch(value::contains);
    }

    private String defaultMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
