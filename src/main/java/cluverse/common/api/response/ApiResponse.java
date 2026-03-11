package cluverse.common.api.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiResponse<T> {
    private int code;
    private HttpStatus status;
    private String message;
    private T data;

    public ApiResponse(HttpStatus status, String message, T data) {
        this.code = status.value();
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> of(HttpStatus httpStatus, String message, T data) {
        return new ApiResponse<>(httpStatus, message, data);
    }

    public static <T> ApiResponse<T> of(HttpStatus httpStatus, T data) {
        return of(httpStatus, httpStatus.name(), data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return of(HttpStatus.OK, data);
    }

    public static ApiResponse<Void> ok() {
        return of(HttpStatus.OK, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return of(HttpStatus.CREATED, data);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return of(HttpStatus.BAD_REQUEST, message, null);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return of(HttpStatus.UNAUTHORIZED, message, null);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return of(HttpStatus.FORBIDDEN, message, null);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return of(HttpStatus.NOT_FOUND, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }
}
