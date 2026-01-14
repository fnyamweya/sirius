package com.sirius.api.error;

import com.sirius.api.tenant.SiriusRequestContextHolder;
import com.sirius.core.exception.SiriusErrorCode;
import com.sirius.core.exception.SiriusException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SiriusException.class)
    public ProblemDetail sirius(SiriusException ex) {
        HttpStatus status = switch (ex.code()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, IDEMPOTENCY_CONFLICT, INVARIANT_VIOLATION -> HttpStatus.CONFLICT;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", ex.code().name());
        if (ex.details() != null && !ex.details().isEmpty()) {
            pd.setProperty("details", ex.details());
        }
        try {
            pd.setProperty("correlation_id", SiriusRequestContextHolder.getRequired().correlationId());
        } catch (Exception ignored) {
        }
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Error");
        pd.setDetail("Request validation failed");
        pd.setProperty("code", SiriusErrorCode.VALIDATION_ERROR.name());

        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        pd.setProperty("field_errors", fields);
        try {
            pd.setProperty("correlation_id", SiriusRequestContextHolder.getRequired().correlationId());
        } catch (Exception ignored) {
        }
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Forbidden");
        pd.setProperty("code", SiriusErrorCode.FORBIDDEN.name());
        try {
            pd.setProperty("correlation_id", SiriusRequestContextHolder.getRequired().correlationId());
        } catch (Exception ignored) {
        }
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail fallback(Exception ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
        pd.setProperty("code", SiriusErrorCode.INTERNAL_ERROR.name());
        pd.setProperty("path", request.getRequestURI());
        try {
            pd.setProperty("correlation_id", SiriusRequestContextHolder.getRequired().correlationId());
        } catch (Exception ignored) {
        }
        return pd;
    }
}
