package com.gymflow.gym.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    ProblemDetail forbidden(ForbiddenOperationException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado", "O usuário não possui permissão para esta operação", request);
    }

    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(Exception ex, HttpServletRequest request) {
        String detail = ex instanceof ConflictException ? ex.getMessage() : "O recurso foi alterado ou viola uma restrição de integridade";
        return problem(HttpStatus.CONFLICT, "Conflito", detail, request);
    }

    @ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class})
    ProblemDetail invalid(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Falha de validação", "Um ou mais campos são inválidos", request);
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class})
    ProblemDetail malformed(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
            "O corpo ou os parâmetros não seguem o contrato da API", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled API exception type={}", ex.getClass().getName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
            "Não foi possível concluir a operação", request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://gymflow.local/problems/" + status.value()));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
