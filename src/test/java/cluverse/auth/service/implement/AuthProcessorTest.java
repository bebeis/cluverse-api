package cluverse.auth.service.implement;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthProcessorTest {

    @Mock
    private AuthReader authReader;

    @Mock
    private AuthWriter authWriter;

    @InjectMocks
    private AuthProcessor authProcessor;

    @Test
    void 회원가입시_가입_후_마지막_로그인을_갱신한다() {
        MemberRegisterRequest request = new MemberRegisterRequest(
                "test@example.com", "password123", "testuser", 10L, List.of(1L));
        Member member = mock(Member.class);
        when(authWriter.register(request)).thenReturn(member);
        when(member.isActive()).thenReturn(true);

        Member result = authProcessor.register(request, "127.0.0.1");

        assertThat(result).isSameAs(member);
        verify(authWriter).register(request);
        verify(authWriter).updateLastLogin(member, "127.0.0.1");
    }

    @Test
    void 이메일_로그인시_마지막_로그인을_갱신한다() {
        Member member = mock(Member.class);
        when(authReader.readByEmailAndPassword("test@example.com", "password123")).thenReturn(member);
        when(member.isActive()).thenReturn(true);

        Member result = authProcessor.loginWithEmail("test@example.com", "password123", "127.0.0.1");

        assertThat(result).isSameAs(member);
        verify(authWriter).updateLastLogin(member, "127.0.0.1");
    }

    @Test
    void OAuth_기존회원은_소셜가입_없이_로그인한다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-id-123", "test@example.com", "testuser");
        Member member = mock(Member.class);
        when(authReader.findBySocialAccount(OAuthProvider.KAKAO, "provider-id-123")).thenReturn(Optional.of(member));
        when(member.isActive()).thenReturn(true);

        Member result = authProcessor.loginWithOAuth(userInfo, OAuthProvider.KAKAO, "127.0.0.1");

        assertThat(result).isSameAs(member);
        verify(authWriter).updateLastLogin(member, "127.0.0.1");
        verify(authWriter, never()).registerBySocial(any(), any());
    }

    @Test
    void OAuth_신규회원은_자동_가입_후_로그인한다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("new-provider-id", "new@example.com", "newuser");
        Member newMember = mock(Member.class);
        when(authReader.findBySocialAccount(OAuthProvider.GOOGLE, "new-provider-id")).thenReturn(Optional.empty());
        when(authWriter.registerBySocial(userInfo, OAuthProvider.GOOGLE)).thenReturn(newMember);
        when(newMember.isActive()).thenReturn(true);

        Member result = authProcessor.loginWithOAuth(userInfo, OAuthProvider.GOOGLE, "127.0.0.1");

        assertThat(result).isSameAs(newMember);
        verify(authWriter).registerBySocial(userInfo, OAuthProvider.GOOGLE);
        verify(authWriter).updateLastLogin(newMember, "127.0.0.1");
    }

    @Test
    void 탈퇴한_회원은_로그인할_수_없다() {
        Member member = mock(Member.class);
        when(authReader.readByEmailAndPassword("test@example.com", "password123")).thenReturn(member);
        when(member.isActive()).thenReturn(false);

        assertThatThrownBy(() -> authProcessor.loginWithEmail("test@example.com", "password123", "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
        verify(authWriter, never()).updateLastLogin(any(), any());
    }
}
