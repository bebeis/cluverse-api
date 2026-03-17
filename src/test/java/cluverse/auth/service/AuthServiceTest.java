package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.implement.AuthReader;
import cluverse.auth.service.implement.AuthWriter;
import cluverse.common.auth.LoginMember;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberRole;
import cluverse.member.domain.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthReader authReader;

    @Mock
    private AuthWriter authWriter;

    @Mock
    private OAuthTokenStore oAuthTokenStore;

    @InjectMocks
    private AuthService authService;

    @Test
    void 이메일_로그인_성공() {
        Member member = mock(Member.class);

        when(authReader.readByEmailAndPassword("test@example.com", "password123")).thenReturn(member);
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("testuser");
        when(member.getRole()).thenReturn(MemberRole.MEMBER);

        LoginMember result = authService.loginWithEmail("test@example.com", "password123", "127.0.0.1");

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("testuser");
        verify(authWriter).updateLastLogin(member, "127.0.0.1");
    }

    @Test
    void OAuth_로그인_기존_회원() {
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-id-123", "test@example.com", "testuser");
        Member member = mock(Member.class);

        when(authReader.findBySocialAccount(OAuthProvider.KAKAO, "provider-id-123"))
                .thenReturn(Optional.of(member));
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("testuser");
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(oAuthTokenStore.save(any(LoginMember.class))).thenReturn("test-token");

        String result = authService.loginWithOAuthAndCreateToken(userInfo, OAuthProvider.KAKAO, "127.0.0.1");

        assertThat(result).isEqualTo("test-token");
        verify(authWriter).updateLastLogin(member, "127.0.0.1");
        verify(authWriter, never()).registerBySocial(any(), any());
    }

    @Test
    void OAuth_로그인_신규_회원_자동_가입() {
        OAuthUserInfo userInfo = new OAuthUserInfo("new-provider-id", "new@example.com", "newuser");
        Member newMember = mock(Member.class);

        when(authReader.findBySocialAccount(OAuthProvider.GOOGLE, "new-provider-id"))
                .thenReturn(Optional.empty());
        when(authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE)).thenReturn(newMember);
        when(newMember.getId()).thenReturn(2L);
        when(newMember.getNickname()).thenReturn("newuser");
        when(newMember.getRole()).thenReturn(MemberRole.MEMBER);
        when(oAuthTokenStore.save(any(LoginMember.class))).thenReturn("new-token");

        String result = authService.loginWithOAuthAndCreateToken(userInfo, OAuthProvider.GOOGLE, "127.0.0.1");

        assertThat(result).isEqualTo("new-token");
        verify(authWriter).registerBySocial(userInfo, OAuthProvider.GOOGLE);
    }
}
