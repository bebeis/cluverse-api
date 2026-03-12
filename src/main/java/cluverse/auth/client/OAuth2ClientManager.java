package cluverse.auth.client;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuth2ClientManager {

    private final Map<String, OAuth2Client> clients;

    public OAuth2ClientManager(List<OAuth2Client> clientList) {
        this.clients = clientList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        client -> client.providerKey().toLowerCase(Locale.ROOT),
                        Function.identity()
                ));
    }

    public OAuth2Client getClient(String provider) {
        OAuth2Client client = clients.get(provider.toLowerCase(Locale.ROOT));
        if (client == null) {
            throw new BadRequestException(AuthExceptionMessage.UNSUPPORTED_OAUTH_PROVIDER.getMessage());
        }
        return client;
    }
}
