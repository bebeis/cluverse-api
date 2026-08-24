package cluverse.auth.client;

import cluverse.auth.properties.OAuth2Properties;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.ExternalServiceException;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class KakaoOAuth2Client implements OAuth2Client {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final OAuth2Properties.Provider properties;

    @Autowired
    public KakaoOAuth2Client(OAuth2Properties properties) {
        this(properties, RestClient.create());
    }

    KakaoOAuth2Client(OAuth2Properties properties, RestClient restClient) {
        this.properties = properties.kakao();
        this.restClient = restClient;
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
    public OAuthUserInfo getUserInfo(OAuthCredential credential) {
        if (isBlank(credential.code()) || isBlank(credential.redirectUri())) {
            throw new BadRequestException("카카오 인가 코드와 콜백 주소가 필요합니다.");
        }
        try {
            String accessToken = exchangeToken(credential.code(), credential.redirectUri());
            return fetchUserInfo(accessToken);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new UnauthorizedException("소셜 인증 정보가 유효하지 않습니다.");
            }
            throw new ExternalServiceException("카카오 인증 서버를 일시적으로 이용할 수 없습니다.", e);
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("카카오 인증 서버를 일시적으로 이용할 수 없습니다.", e);
        }
    }

    private String exchangeToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        KakaoTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);
        if (response == null || isBlank(response.accessToken())) {
            throw new ExternalServiceException("카카오 인증 서버 응답이 올바르지 않습니다.", null);
        }
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        KakaoUserInfo userInfo = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfo.class);

        if (userInfo == null || userInfo.id() == null || userInfo.kakaoAccount() == null
                || isBlank(userInfo.kakaoAccount().email()) || userInfo.kakaoAccount().profile() == null
                || isBlank(userInfo.kakaoAccount().profile().nickname())) {
            throw new UnauthorizedException("카카오 계정의 이메일과 프로필 제공 동의가 필요합니다.");
        }
        return new OAuthUserInfo(
                String.valueOf(userInfo.id()),
                userInfo.kakaoAccount().email(),
                userInfo.kakaoAccount().profile().nickname()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
