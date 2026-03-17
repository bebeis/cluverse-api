package cluverse.auth.service;

import cluverse.common.auth.LoginMember;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthTokenStore {

    private static final long TTL_SECONDS = 300;

    private record Entry(LoginMember loginMember, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String save(LoginMember loginMember) {
        evictExpired();
        String token = UUID.randomUUID().toString();
        store.put(token, new Entry(loginMember, Instant.now().plusSeconds(TTL_SECONDS)));
        return token;
    }

    public LoginMember consume(String token) {
        Entry entry = store.remove(token);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return null;
        }
        return entry.loginMember();
    }

    private void evictExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
