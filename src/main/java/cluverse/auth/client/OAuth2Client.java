package cluverse.auth.client;

import cluverse.member.domain.OAuthProvider;

public interface OAuth2Client {

    String providerKey();

    OAuthProvider provider();

    OAuthUserInfo getUserInfo(String code);
}
