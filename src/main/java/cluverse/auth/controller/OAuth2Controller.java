package cluverse.auth.controller;

import cluverse.auth.client.GoogleOAuth2Client;
import cluverse.auth.client.KakaoOAuth2Client;
import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.AuthService;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.BadRequestException;
import cluverse.member.domain.OAuthProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final KakaoOAuth2Client kakaoOAuth2Client;
    private final GoogleOAuth2Client googleOAuth2Client;
    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/{provider}")
    public void authorize(@PathVariable String provider, HttpServletResponse response) throws IOException {
        String authorizationUrl = switch (provider) {
            case "kakao" -> kakaoOAuth2Client.getAuthorizationUrl();
            case "google" -> googleOAuth2Client.getAuthorizationUrl();
            default -> throw new BadRequestException(AuthExceptionMessage.UNSUPPORTED_OAUTH_PROVIDER.getMessage());
        };
        response.sendRedirect(authorizationUrl);
    }

    @GetMapping("/{provider}/callback")
    public void callback(@PathVariable String provider,
                         @RequestParam String code,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        OAuthUserInfo userInfo = switch (provider) {
            case "kakao" -> kakaoOAuth2Client.getUserInfo(code);
            case "google" -> googleOAuth2Client.getUserInfo(code);
            default -> throw new BadRequestException(AuthExceptionMessage.UNSUPPORTED_OAUTH_PROVIDER.getMessage());
        };
        OAuthProvider oAuthProvider = resolveProvider(provider);

        authService.loginWithOAuth(userInfo, oAuthProvider, request.getRemoteAddr(), request);
        response.sendRedirect(frontendUrl);
    }

    private OAuthProvider resolveProvider(String provider) {
        return switch (provider) {
            case "kakao" -> OAuthProvider.KAKAO;
            case "google" -> OAuthProvider.GOOGLE;
            default -> throw new BadRequestException(AuthExceptionMessage.UNSUPPORTED_OAUTH_PROVIDER.getMessage());
        };
    }
}
