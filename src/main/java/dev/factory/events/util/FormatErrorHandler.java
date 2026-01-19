package dev.factory.events.util;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class FormatErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBodyValidation(MethodArgumentNotValidException ex) {
        return Map.of(
                "error", "VALIDATION_ERROR",
                "details", ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(err -> err.getField() + ": " + err.getDefaultMessage())
                        .toList()
        );
    }
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleParamValidation(ConstraintViolationException ex) {
        return Map.of(
                "error", "VALIDATION_ERROR",
                "details", ex.getConstraintViolations()
                        .stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .toList()
        );
    }
}
