package cluverse.common.auth;

import cluverse.member.domain.MemberRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;

final class LoginMemberExtractor {

    private static final String BEARER_SCHEME = "Bearer";

    LoginMember extract(HttpServletRequest request) {
        LoginMember sessionMember = extractFromSession(request.getSession(false));
        if (sessionMember != null) {
            return sessionMember;
        }
        return extractFromAuthorization(request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    private LoginMember extractFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (LoginMember) session.getAttribute(LoginMemberArgumentResolver.SESSION_KEY);
    }

    private LoginMember extractFromAuthorization(String authorization) {
        if (authorization == null) {
            return null;
        }

        String[] parts = authorization.trim().split("\\s+");
        if (parts.length != 2 || !BEARER_SCHEME.equalsIgnoreCase(parts[0])) {
            return null;
        }

        try {
            long memberId = Long.parseLong(parts[1]);
            if (memberId <= 0) {
                return null;
            }
            return new LoginMember(memberId, null, MemberRole.MEMBER);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
