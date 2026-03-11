package cluverse.auth.client;

import cluverse.auth.properties.OAuth2Properties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOAuth2Client {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient = RestClient.create();
    private final OAuth2Properties.Provider properties;

    public GoogleOAuth2Client(OAuth2Properties properties) {
        this.properties = properties.google();
    }

    public String getAuthorizationUrl() {
        return AUTH_URL
                + "?client_id=" + properties.clientId()
                + "&redirect_uri=" + properties.redirectUri()
                + "&response_type=code"
                + "&scope=email profile";
    }

    public OAuthUserInfo getUserInfo(String code) {
        String accessToken = exchangeToken(code);
        return fetchUserInfo(accessToken);
    }

    private String exchangeToken(String code) {
        GoogleTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=authorization_code"
                        + "&client_id=" + properties.clientId()
                        + "&client_secret=" + properties.clientSecret()
                        + "&redirect_uri=" + properties.redirectUri()
                        + "&code=" + code)
                .retrieve()
                .body(GoogleTokenResponse.class);
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        GoogleUserInfo userInfo = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfo.class);

        return new OAuthUserInfo(userInfo.sub(), userInfo.email(), userInfo.name());
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    private record GoogleUserInfo(String sub, String email, String name) {}
}
