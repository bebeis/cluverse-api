package cluverse.auth.service.implement;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberAuth;
import cluverse.member.service.implement.MemberReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthReaderTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private PasswordConfig passwordConfig;

    @InjectMocks
    private AuthReader authReader;

    @Test
    void 이메일_로그인_성공() {
        Member member = mock(Member.class);
        MemberAuth memberAuth = mock(MemberAuth.class);

        when(memberReader.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(member.getMemberAuth()).thenReturn(memberAuth);
        when(memberAuth.getPasswordHash()).thenReturn("hashed");
        when(passwordConfig.matches("password123", "hashed")).thenReturn(true);

        Member result = authReader.readByEmailAndPassword("test@example.com", "password123");

        assertThat(result).isSameAs(member);
    }

    @Test
    void 이메일_로그인_실패_이메일_없음() {
        when(memberReader.findByEmail("none@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authReader.readByEmailAndPassword("none@example.com", "password"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    void 이메일_로그인_실패_비밀번호_불일치() {
        Member member = mock(Member.class);
        MemberAuth memberAuth = mock(MemberAuth.class);

        when(memberReader.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(member.getMemberAuth()).thenReturn(memberAuth);
        when(memberAuth.getPasswordHash()).thenReturn("hashed");
        when(passwordConfig.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authReader.readByEmailAndPassword("test@example.com", "wrongpass"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
    }
}
