package cluverse.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class LoginSessionManager {

    public void createSession(HttpServletRequest request, LoginMember loginMember) {
        HttpSession session = request.getSession(true);
        session.setAttribute(LoginMemberArgumentResolver.SESSION_KEY, loginMember);
    }

    public void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
