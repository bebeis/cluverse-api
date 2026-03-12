package cluverse.auth.client;

import cluverse.member.domain.OAuthProvider;

public interface OAuth2Client {

    String providerKey();

    OAuthProvider provider();

    String getAuthorizationUrl();

    OAuthUserInfo getUserInfo(String code);
}
