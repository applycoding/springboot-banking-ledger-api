package com.arpanbom.ledger.error;

import com.arpanbom.ledger.domain.LedgerException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LedgerException.class)
    ProblemDetail handleLedger(LedgerException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusOf(ex), ex.getMessage());
        problem.setTitle(ex.title());
        problem.setProperty("code", ex.code());
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The account was updated concurrently. Retry the request.");
        problem.setTitle("Concurrent Update");
        problem.setProperty("code", "concurrent_update");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access is denied");
        problem.setTitle("Forbidden");
        problem.setProperty("code", "forbidden");
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Invalid username or password");
        problem.setTitle("Unauthorized");
        problem.setProperty("code", "invalid_credentials");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        problem.setTitle("Internal Server Error");
        problem.setProperty("code", "internal_error");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Bad Request");
        problem.setProperty("code", "validation_failed");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
        problem.setTitle("Bad Request");
        problem.setProperty("code", "malformed_json");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static HttpStatus statusOf(LedgerException ex) {
        return switch (ex) {
            case LedgerException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case LedgerException.InvalidAmount ignored -> HttpStatus.BAD_REQUEST;
            case LedgerException.InvalidIdempotencyKey ignored -> HttpStatus.BAD_REQUEST;
            case LedgerException.InsufficientFunds ignored -> HttpStatus.UNPROCESSABLE_ENTITY;
            case LedgerException.AccountNotOpen ignored -> HttpStatus.UNPROCESSABLE_ENTITY;
            case LedgerException.SameAccount ignored -> HttpStatus.UNPROCESSABLE_ENTITY;
            case LedgerException.CurrencyMismatch ignored -> HttpStatus.UNPROCESSABLE_ENTITY;
            case LedgerException.UnsupportedCurrency ignored -> HttpStatus.UNPROCESSABLE_ENTITY;
            case LedgerException.IdempotencyConflict ignored -> HttpStatus.CONFLICT;
            case LedgerException.ConcurrentUpdate ignored -> HttpStatus.CONFLICT;
            case LedgerException.DuplicateEmail ignored -> HttpStatus.CONFLICT;
            case LedgerException.UnbalancedJournal ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
