package cluverse.common.auth;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

public class AuthInterceptor implements HandlerInterceptor {

    private static final String GET_METHOD = "GET";
    private static final String OPTIONS_METHOD = "OPTIONS";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final LoginMemberExtractor loginMemberExtractor = new LoginMemberExtractor();
    private final List<String> publicGetPathPatterns;
    private final List<String> protectedGetPathPatterns;

    public AuthInterceptor(List<String> publicGetPathPatterns, List<String> protectedGetPathPatterns) {
        this.publicGetPathPatterns = publicGetPathPatterns;
        this.protectedGetPathPatterns = protectedGetPathPatterns;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicGetRequest(request)) {
            return true;
        }
        requireAuthenticated(request);
        return true;
    }

    private boolean isPublicGetRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if (OPTIONS_METHOD.equals(method)) {
            return true;
        }
        if (!GET_METHOD.equals(method)) {
            return false;
        }

        String requestUri = request.getRequestURI();
        if (protectedGetPathPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestUri))) {
            return false;
        }
        return publicGetPathPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

    private void requireAuthenticated(HttpServletRequest request) {
        if (loginMemberExtractor.extract(request) == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
