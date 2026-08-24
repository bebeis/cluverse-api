package cluverse.auth.client;

import cluverse.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthRedirectUriPolicyTest {

    private final OAuthRedirectUriPolicy policy = new OAuthRedirectUriPolicy();

    @Test
    void 허용된_프론트_출처와_공급자_콜백_경로를_받는다() {
        assertThat(policy.validate("kakao", "https://cluverse-web.vercel.app/login/callback/kakao"))
                .isEqualTo("https://cluverse-web.vercel.app/login/callback/kakao");
        assertThat(policy.validate("google", "https://www.cluverse.cona.team/login/callback/google"))
                .isEqualTo("https://www.cluverse.cona.team/login/callback/google");
    }

    @Test
    void 허용되지_않은_출처나_공급자가_다른_콜백_경로를_거부한다() {
        assertThatThrownBy(() -> policy.validate("kakao", "https://attacker.example/login/callback/kakao"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> policy.validate("kakao", "https://cluverse-web.vercel.app/login/callback/google"))
                .isInstanceOf(BadRequestException.class);
    }
}
