package cluverse.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginSessionManagerTest {

    private final LoginSessionManager loginSessionManager = new LoginSessionManager();

    @Test
    void 로그인_세션을_생성할_수_있다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        LoginMember loginMember = new LoginMember(1L, "luna", cluverse.member.domain.MemberRole.MEMBER);

        when(request.getSession(true)).thenReturn(session);

        loginSessionManager.createSession(request, loginMember);

        verify(session).setAttribute(LoginMemberArgumentResolver.SESSION_KEY, loginMember);
    }

    @Test
    void 기존_세션이_있으면_무효화한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session);

        loginSessionManager.invalidateSession(request);

        verify(session).invalidate();
    }
}
