package cluverse.auth.client;

import cluverse.auth.properties.OAuth2Properties;
import cluverse.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleOAuth2ClientTest {

    @Test
    void 프론트가_받은_액세스_토큰의_대상_클라이언트를_검증하고_사용자를_조회한다() {
        OAuth2Properties properties = properties("google-client");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?access_token=google-token"))
                .andRespond(withSuccess("{\"aud\":\"google-client\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/oauth2/v3/userinfo"))
                .andExpect(header("Authorization", "Bearer google-token"))
                .andRespond(withSuccess("""
                        {"sub":"google-id","email":"google@example.com","name":"구글사용자"}
                        """, MediaType.APPLICATION_JSON));
        GoogleOAuth2Client client = new GoogleOAuth2Client(properties, builder.build());

        OAuthUserInfo result = client.getUserInfo(new OAuthCredential(null, "google-token", null));

        assertThat(result).isEqualTo(new OAuthUserInfo("google-id", "google@example.com", "구글사용자"));
        server.verify();
    }

    @Test
    void 다른_클라이언트에_발급된_구글_토큰은_거부한다() {
        OAuth2Properties properties = properties("google-client");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?access_token=foreign-token"))
                .andRespond(withSuccess("{\"aud\":\"foreign-client\"}", MediaType.APPLICATION_JSON));
        GoogleOAuth2Client client = new GoogleOAuth2Client(properties, builder.build());

        assertThatThrownBy(() -> client.getUserInfo(new OAuthCredential(null, "foreign-token", null)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("소셜 인증 정보가 유효하지 않습니다.");
        server.verify();
    }

    private OAuth2Properties properties(String googleClientId) {
        return new OAuth2Properties(
                new OAuth2Properties.Provider("kakao-client", "unused", "unused"),
                new OAuth2Properties.Provider(googleClientId, "unused", "unused")
        );
    }
}
