package cluverse.auth.client;

import cluverse.auth.properties.OAuth2Properties;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.ExternalServiceException;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class GoogleOAuth2Client implements OAuth2Client {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient;
    private final OAuth2Properties.Provider properties;

    @Autowired
    public GoogleOAuth2Client(OAuth2Properties properties) {
        this(properties, RestClient.create());
    }

    GoogleOAuth2Client(OAuth2Properties properties, RestClient restClient) {
        this.properties = properties.google();
        this.restClient = restClient;
    }

    @Override
    public String providerKey() {
        return "google";
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(OAuthCredential credential) {
        if (isBlank(credential.accessToken())) {
            throw new BadRequestException("구글 액세스 토큰이 필요합니다.");
        }
        try {
            verifyAudience(credential.accessToken());
            return fetchUserInfo(credential.accessToken());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new UnauthorizedException("소셜 인증 정보가 유효하지 않습니다.");
            }
            throw new ExternalServiceException("구글 인증 서버를 일시적으로 이용할 수 없습니다.", e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("구글 인증 서버를 일시적으로 이용할 수 없습니다.", e);
        }
    }

    private void verifyAudience(String accessToken) {
        GoogleTokenInfo response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("oauth2.googleapis.com")
                        .path("/tokeninfo")
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .body(GoogleTokenInfo.class);
        if (response == null || !properties.clientId().equals(response.audience())) {
            throw new UnauthorizedException("소셜 인증 정보가 유효하지 않습니다.");
        }
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        GoogleUserInfo userInfo = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfo.class);

        if (userInfo == null || isBlank(userInfo.sub()) || isBlank(userInfo.email()) || isBlank(userInfo.name())) {
            throw new UnauthorizedException("구글 계정의 이메일과 프로필 제공 동의가 필요합니다.");
        }
        return new OAuthUserInfo(userInfo.sub(), userInfo.email(), userInfo.name());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GoogleTokenInfo(@JsonProperty("aud") String audience) {}

    private record GoogleUserInfo(String sub, String email, String name) {}
}
