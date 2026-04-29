package pt.xavier.tms.shared.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        T data,
        ErrorResponse error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> failure(ErrorResponse error) {
        return new ApiResponse<>(null, error);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(
            String code,
            String message,
            String correlationId,
            Map<String, String> fields,
            Instant timestamp
    ) {

        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message, null, null, Instant.now());
        }

        public static ErrorResponse of(String code, String message, Map<String, String> fields) {
            return new ErrorResponse(code, message, null, fields, Instant.now());
        }

        public static ErrorResponse unexpected(String message, String correlationId) {
            return new ErrorResponse("INTERNAL_ERROR", message, correlationId, null, Instant.now());
        }
    }
}
