package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.auth.LoginMember;
import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberAuth;
import cluverse.member.domain.MemberRole;
import cluverse.member.domain.OAuthProvider;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private PasswordConfig passwordConfig;

    @InjectMocks
    private AuthService authService;

    @Test
    void 이메일_로그인_성공() {
        // given
        Member member = mock(Member.class);
        MemberAuth memberAuth = mock(MemberAuth.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(memberQueryRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(member.getMemberAuth()).thenReturn(memberAuth);
        when(memberAuth.getPasswordHash()).thenReturn("hashed");
        when(passwordConfig.matches("password123", "hashed")).thenReturn(true);
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("testuser");
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(request.getSession(true)).thenReturn(session);

        // when
        LoginMember result = authService.loginWithEmail("test@example.com", "password123", "127.0.0.1", request);

        // then
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("testuser");
        verify(member).updateLastLogin("127.0.0.1");
        verify(session).setAttribute(anyString(), any());
    }

    @Test
    void 이메일_로그인_실패_이메일_없음() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(memberQueryRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.loginWithEmail("none@example.com", "password", "127.0.0.1", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    void 이메일_로그인_실패_비밀번호_불일치() {
        // given
        Member member = mock(Member.class);
        MemberAuth memberAuth = mock(MemberAuth.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(memberQueryRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(member.getMemberAuth()).thenReturn(memberAuth);
        when(memberAuth.getPasswordHash()).thenReturn("hashed");
        when(passwordConfig.matches("wrongpass", "hashed")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.loginWithEmail("test@example.com", "wrongpass", "127.0.0.1", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    void OAuth_로그인_기존_회원() {
        // given
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-id-123", "test@example.com", "testuser");
        Member member = mock(Member.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(memberQueryRepository.findBySocialAccount(OAuthProvider.KAKAO, "provider-id-123"))
                .thenReturn(Optional.of(member));
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("testuser");
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(request.getSession(true)).thenReturn(session);

        // when
        LoginMember result = authService.loginWithOAuth(userInfo, OAuthProvider.KAKAO, "127.0.0.1", request);

        // then
        assertThat(result.memberId()).isEqualTo(1L);
        verify(member).updateLastLogin("127.0.0.1");
        verify(memberRepository, never()).save(any());
    }

    @Test
    void OAuth_로그인_신규_회원_자동_가입() {
        // given
        OAuthUserInfo userInfo = new OAuthUserInfo("new-provider-id", "new@example.com", "newuser");
        Member newMember = mock(Member.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(memberQueryRepository.findBySocialAccount(OAuthProvider.GOOGLE, "new-provider-id"))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByNickname("newuser")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(newMember);
        when(newMember.getId()).thenReturn(2L);
        when(newMember.getNickname()).thenReturn("newuser");
        when(newMember.getRole()).thenReturn(MemberRole.MEMBER);
        when(request.getSession(true)).thenReturn(session);

        // when
        LoginMember result = authService.loginWithOAuth(userInfo, OAuthProvider.GOOGLE, "127.0.0.1", request);

        // then
        assertThat(result.memberId()).isEqualTo(2L);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void 로그아웃_세션_존재() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        // when
        authService.logout(request);

        // then
        verify(session).invalidate();
    }

    @Test
    void 로그아웃_세션_없음() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        // when
        authService.logout(request);

        // then
        verify(request).getSession(false);
    }
}
