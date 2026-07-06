package team2.goodsmap.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final int status;
    private final String message;
    private final List<FieldError> errors;

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, List<FieldError> errors) {
        return new ErrorResponse(status.value(), message, errors);
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String message;
    }
}