package com.gymflow.workout.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException ex, HttpServletRequest request) { return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request); }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException ex, HttpServletRequest request) { return problem(HttpStatus.FORBIDDEN, "Acesso negado", "O usuário não possui permissão para esta operação", request); }

    @ExceptionHandler(EligibilityConflictException.class)
    ProblemDetail eligibility(EligibilityConflictException ex, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.CONFLICT, "Exercício inelegível", ex.getMessage(), request);
        detail.setProperty("exerciseIds", ex.getExerciseIds());
        return detail;
    }

    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(Exception ex, HttpServletRequest request) {
        String message = ex instanceof ConflictException ? ex.getMessage() : "O recurso foi alterado ou viola uma restrição de integridade";
        return problem(HttpStatus.CONFLICT, "Conflito", message, request);
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    ProblemDetail unavailable(DependencyUnavailableException ex, HttpServletRequest request) { return problem(HttpStatus.SERVICE_UNAVAILABLE, "Serviço temporariamente indisponível", ex.getMessage(), request); }

    @ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail invalid(Exception ex, HttpServletRequest request) { return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), request); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Falha de validação", "Um ou mais campos são inválidos", request);
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled API exception type={}", ex.getClass().getName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Não foi possível concluir a operação", request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message == null ? title : message);
        detail.setTitle(title);
        detail.setType(URI.create("https://gymflow.local/problems/" + status.value()));
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
