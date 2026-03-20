package cluverse.common.api;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.ForbiddenException;
import cluverse.common.exception.NotFoundException;
import cluverse.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ApiControllerAdvice {

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> bindException(BindException e, HttpServletRequest request) {
        logClientException(HttpStatus.BAD_REQUEST, request, e);
        return ApiResponse.of(HttpStatus.BAD_REQUEST,
                e.getBindingResult().getAllErrors().getFirst().getDefaultMessage(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> methodArgumentNotValidException(MethodArgumentNotValidException e,
                                                               HttpServletRequest request) {
        logClientException(HttpStatus.BAD_REQUEST, request, e);
        return ApiResponse.badRequest(e.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> httpMessageNotReadableException(HttpMessageNotReadableException e,
                                                               HttpServletRequest request) {
        logClientException(HttpStatus.BAD_REQUEST, request, e);
        return ApiResponse.badRequest("요청 본문 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> badRequestException(BadRequestException e, HttpServletRequest request) {
        logClientException(HttpStatus.BAD_REQUEST, request, e);
        return ApiResponse.badRequest(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Object> unauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        logClientException(HttpStatus.UNAUTHORIZED, request, e);
        return ApiResponse.unauthorized(e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Object> forbiddenException(ForbiddenException e, HttpServletRequest request) {
        logClientException(HttpStatus.FORBIDDEN, request, e);
        return ApiResponse.forbidden(e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> notFoundException(NotFoundException e, HttpServletRequest request) {
        logClientException(HttpStatus.NOT_FOUND, request, e);
        return ApiResponse.notFound(e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> noResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        logClientException(HttpStatus.NOT_FOUND, request, e);
        return ApiResponse.notFound(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> exception(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception. status={}, method={}, uri={}, exceptionType={}, message={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getMethod(),
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e.getMessage()
        );
        return ApiResponse.error("서버 내부 오류가 발생했습니다.");
    }

    private void logClientException(HttpStatus status, HttpServletRequest request, Exception e) {
        log.warn("Handled exception. status={}, method={}, uri={}, exceptionType={}, message={}",
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e.getMessage()
        );
    }
}
