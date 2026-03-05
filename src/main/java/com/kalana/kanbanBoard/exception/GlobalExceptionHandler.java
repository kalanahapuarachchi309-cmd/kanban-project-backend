package com.kalana.kanbanBoard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse(404, ex.getMessage(), LocalDateTime.now(), null));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new ErrorResponse(403, ex.getMessage(), LocalDateTime.now(), null));
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleSpringSecurityForbidden(
                        org.springframework.security.access.AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new ErrorResponse(403, "You do not have permission to perform this action",
                                                LocalDateTime.now(),
                                                null));
        }

        @ExceptionHandler({ AuthenticationException.class, AuthenticationCredentialsNotFoundException.class })
        public ResponseEntity<ErrorResponse> handleUnauthorized(Exception ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new ErrorResponse(401, "Authentication required", LocalDateTime.now(), null));
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse(400, ex.getMessage(), LocalDateTime.now(), null));
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new ErrorResponse(409, ex.getMessage(), LocalDateTime.now(), null));
        }

        @ExceptionHandler(EmailDeliveryException.class)
        public ResponseEntity<ErrorResponse> handleEmailDelivery(EmailDeliveryException ex) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(new ErrorResponse(503, ex.getMessage(), LocalDateTime.now(), null));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
                List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(FieldError::getDefaultMessage)
                                .collect(Collectors.toList());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse(400, "Validation failed", LocalDateTime.now(), errors));
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(new ErrorResponse(405, ex.getMessage(), LocalDateTime.now(), null));
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse(404, ex.getMessage(), LocalDateTime.now(), null));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ErrorResponse(500, ex.getMessage(), LocalDateTime.now(), null));
        }
}
