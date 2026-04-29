package com.example.bankcards.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ApiError(
        int status,
        String error,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp,
        String path,
        Map<String, String> validationErrors,
        List<String> errors
) {

    // Фабричный метод для ошибок валидации
    public static ApiError ofValidationErrors(
            Map<String, String> validationErrors,
            String path) {
        return ApiError.builder()
                .status(400)
                .error("Bad Request")
                .message("Ошибка валидации")
                .timestamp(LocalDateTime.now())
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }

    // Фабричный метод для простых ошибок
    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    // Фабричный метод с дополнительными деталями
    public static ApiError of(int status, String error, String message, String path, List<String> errors) {
        return ApiError.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .errors(errors)
                .build();
    }
}
