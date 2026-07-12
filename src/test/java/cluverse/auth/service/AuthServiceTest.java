package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.implement.AuthProcessor;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.auth.LoginMember;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberRole;
import cluverse.member.domain.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthProcessor authProcessor;

    @InjectMocks
    private AuthService authService;

    @Test
    void 이메일_로그인은_프로세서_결과를_LoginMember로_매핑한다() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("testuser");
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(authProcessor.loginWithEmail("test@example.com", "password123", "127.0.0.1")).thenReturn(member);

        LoginMember result = authService.loginWithEmail("test@example.com", "password123", "127.0.0.1");

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("testuser");
        verify(authProcessor).loginWithEmail("test@example.com", "password123", "127.0.0.1");
    }

    @Test
    void OAuth_로그인은_프로세서에_위임한다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-id-123", "test@example.com", "testuser");
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("testuser");
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(authProcessor.loginWithOAuth(userInfo, OAuthProvider.KAKAO, "127.0.0.1")).thenReturn(member);

        LoginMember result = authService.loginWithOAuth(userInfo, OAuthProvider.KAKAO, "127.0.0.1");

        assertThat(result.memberId()).isEqualTo(1L);
        verify(authProcessor).loginWithOAuth(userInfo, OAuthProvider.KAKAO, "127.0.0.1");
    }

    @Test
    void 회원가입은_프로세서에_위임한다() {
        MemberRegisterRequest request = new MemberRegisterRequest(
                "new@example.com", "password123", "newuser", 10L, List.of(1L));
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(3L);
        when(member.getNickname()).thenReturn("newuser");
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(authProcessor.register(request, "127.0.0.1")).thenReturn(member);

        LoginMember result = authService.register(request, "127.0.0.1");

        assertThat(result.memberId()).isEqualTo(3L);
        assertThat(result.nickname()).isEqualTo("newuser");
        verify(authProcessor).register(request, "127.0.0.1");
    }
}
