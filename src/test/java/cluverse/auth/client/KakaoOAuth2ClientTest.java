package cluverse.auth.client;

import cluverse.auth.properties.OAuth2Properties;
import cluverse.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class KakaoOAuth2ClientTest {

    @Test
    void 인가에_사용한_리다이렉트_URI로_토큰을_교환한다() {
        OAuth2Properties properties = properties("kakao-client", "kakao-secret");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "authorization_code",
                        "client_id", "kakao-client",
                        "client_secret", "kakao-secret",
                        "redirect_uri", "https://cluverse-web.vercel.app/login/callback/kakao",
                        "code", "authorization-code"
                )))
                .andRespond(withSuccess("{\"access_token\":\"kakao-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header("Authorization", "Bearer kakao-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 12345,
                          "kakao_account": {
                            "email": "kakao@example.com",
                            "profile": {"nickname": "카카오사용자"}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        KakaoOAuth2Client client = new KakaoOAuth2Client(properties, builder.build());

        OAuthUserInfo result = client.getUserInfo(new OAuthCredential(
                "authorization-code",
                null,
                "https://cluverse-web.vercel.app/login/callback/kakao"
        ));

        assertThat(result).isEqualTo(new OAuthUserInfo("12345", "kakao@example.com", "카카오사용자"));
        server.verify();
    }

    @Test
    void 카카오가_인가_코드를_거절하면_인증_실패로_변환한다() {
        OAuth2Properties properties = properties("kakao-client", "kakao-secret");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));
        KakaoOAuth2Client client = new KakaoOAuth2Client(properties, builder.build());

        assertThatThrownBy(() -> client.getUserInfo(new OAuthCredential(
                "invalid-code",
                null,
                "https://cluverse-web.vercel.app/login/callback/kakao"
        )))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("소셜 인증 정보가 유효하지 않습니다.");
        server.verify();
    }

    private OAuth2Properties properties(String kakaoClientId, String kakaoClientSecret) {
        return new OAuth2Properties(
                new OAuth2Properties.Provider(kakaoClientId, kakaoClientSecret, "unused"),
                new OAuth2Properties.Provider("google-client", "unused", "unused")
        );
    }
}
