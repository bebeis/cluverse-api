package cluverse.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth2")
public record OAuth2Properties(
        Provider kakao,
        Provider google
) {
    public record Provider(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {}
}
