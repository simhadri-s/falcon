package com.laserxprts.falcon.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.laserxprts.falcon.dto.response.ApiErrorResponse;

public final class ApiErrorResponseFactory {

    private ApiErrorResponseFactory() {
    }

    public static ApiErrorResponse build(HttpStatus status, String message, String path) {
        return ApiErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(path)
            .build();
    }

    public static ApiErrorResponse build(HttpStatus status, String message, WebRequest request) {
        return build(status, message, extractPath(request));
    }

    private static String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }

        String description = request.getDescription(false);
        if (description != null && description.startsWith("uri=")) {
            return description.substring(4);
        }
        return description;
    }
}
