package cluverse.auth.controller;

import cluverse.auth.client.OAuth2Client;
import cluverse.auth.client.OAuth2ClientManager;
import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.AuthService;
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

    private final OAuth2ClientManager oAuth2ClientManager;
    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/{provider}")
    public void authorize(@PathVariable String provider, HttpServletResponse response) throws IOException {
        OAuth2Client client = oAuth2ClientManager.getClient(provider);
        response.sendRedirect(client.getAuthorizationUrl());
    }

    @GetMapping("/{provider}/callback")
    public void callback(@PathVariable String provider,
                         @RequestParam String code,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        OAuth2Client client = oAuth2ClientManager.getClient(provider);
        OAuthUserInfo userInfo = client.getUserInfo(code);

        authService.loginWithOAuth(userInfo, client.provider(), request.getRemoteAddr(), request);
        response.sendRedirect(frontendUrl);
    }
}
