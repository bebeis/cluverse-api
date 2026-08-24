package cluverse.auth.client;

import cluverse.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

@Component
public class OAuthRedirectUriPolicy {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://www.cluverse.cona.team",
            "https://cluverse.cona.team",
            "https://cluverse-web.vercel.app",
            "http://localhost:3000"
    );

    public String validate(String provider, String redirectUri) {
        try {
            URI uri = URI.create(redirectUri);
            String normalizedProvider = provider.toLowerCase(Locale.ROOT);
            String expectedPath = "/login/callback/" + normalizedProvider;
            String origin = originOf(uri);
            if (!ALLOWED_ORIGINS.contains(origin)
                    || !expectedPath.equals(uri.getPath())
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || uri.getUserInfo() != null) {
                throw invalidRedirectUri();
            }
            return uri.toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw invalidRedirectUri();
        }
    }

    private String originOf(URI uri) {
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw invalidRedirectUri();
        }
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://"
                + uri.getHost().toLowerCase(Locale.ROOT) + port;
    }

    private BadRequestException invalidRedirectUri() {
        return new BadRequestException("허용되지 않은 소셜 로그인 콜백 주소입니다.");
    }
}
