package cluverse.common.auth;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.util.AntPathMatcher;

import java.util.List;

public class AuthInterceptor implements HandlerInterceptor {

    private static final String GET_METHOD = "GET";
    private static final String OPTIONS_METHOD = "OPTIONS";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
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
        HttpSession session = request.getSession(false);
        requireAuthenticated(session);
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

    private void requireAuthenticated(HttpSession session) {
        if (session == null || session.getAttribute(LoginMemberArgumentResolver.SESSION_KEY) == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
