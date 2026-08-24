package cluverse.post.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class ViewCountCookieResolver {

    static final String COOKIE_NAME = "cluverse_viewer";
    private static final int COOKIE_MAX_AGE_SECONDS = 365 * 24 * 60 * 60;

    public String resolve(HttpServletRequest request, HttpServletResponse response) {
        String existing = find(request);
        if (existing != null) {
            return existing;
        }
        String issued = UUID.randomUUID().toString();
        response.addHeader(
                "Set-Cookie",
                cookieHeader(request, issued)
        );
        return issued;
    }

    private String cookieHeader(HttpServletRequest request, String value) {
        boolean secure = request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String sameSite = secure ? "None" : "Lax";
        String secureAttribute = secure ? "; Secure" : "";
        return "%s=%s; Max-Age=%d; Path=/; HttpOnly%s; SameSite=%s"
                .formatted(COOKIE_NAME, value, COOKIE_MAX_AGE_SECONDS, secureAttribute, sameSite);
    }

    private String find(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
