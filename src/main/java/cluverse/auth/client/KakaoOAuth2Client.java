package cluverse.auth.client;

import cluverse.auth.properties.OAuth2Properties;
import cluverse.member.domain.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuth2Client implements OAuth2Client {

    private static final String AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();
    private final OAuth2Properties.Provider properties;

    public KakaoOAuth2Client(OAuth2Properties properties) {
        this.properties = properties.kakao();
    }

    @Override
    public String providerKey() {
        return "kakao";
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public String getAuthorizationUrl() {
        return AUTH_URL
                + "?client_id=" + properties.clientId()
                + "&redirect_uri=" + properties.redirectUri()
                + "&response_type=code";
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        String accessToken = exchangeToken(code);
        return fetchUserInfo(accessToken);
    }

    private String exchangeToken(String code) {
        KakaoTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=authorization_code"
                        + "&client_id=" + properties.clientId()
                        + "&client_secret=" + properties.clientSecret()
                        + "&redirect_uri=" + properties.redirectUri()
                        + "&code=" + code)
                .retrieve()
                .body(KakaoTokenResponse.class);
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        KakaoUserInfo userInfo = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfo.class);

        return new OAuthUserInfo(
                String.valueOf(userInfo.id()),
                userInfo.kakaoAccount().email(),
                userInfo.kakaoAccount().profile().nickname()
        );
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    private record KakaoUserInfo(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        record KakaoAccount(
                String email,
                Profile profile
        ) {
            record Profile(String nickname) {}
        }
    }
}
