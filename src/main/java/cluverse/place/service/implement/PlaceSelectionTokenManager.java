package cluverse.place.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.VerifiedPlaceCandidate;
import cluverse.place.exception.PlaceExceptionMessage;
import cluverse.place.properties.LocalMapProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Component
public class PlaceSelectionTokenManager {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_VERSION = "v1";
    private static final int MAX_TOKEN_LENGTH = 8192;

    private final LocalMapProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PlaceSelectionTokenManager(LocalMapProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    PlaceSelectionTokenManager(LocalMapProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String issue(Long memberId, PlaceCandidate candidate) {
        validateSecret();
        Instant issuedAt = clock.instant();
        TokenPayload payload = new TokenPayload(
                TOKEN_VERSION,
                memberId,
                issuedAt.getEpochSecond(),
                issuedAt.plus(properties.selectionTokenTtl()).getEpochSecond(),
                candidate
        );
        byte[] payloadBytes = serialize(payload);
        return encode(payloadBytes) + "." + encode(sign(payloadBytes));
    }

    public VerifiedPlaceCandidate verify(Long memberId, String token) {
        validateTokenFormat(token);
        String[] parts = token.split("\\.", -1);
        byte[] payloadBytes = decode(parts[0]);
        byte[] actualSignature = decode(parts[1]);
        if (!MessageDigest.isEqual(sign(payloadBytes), actualSignature)) {
            throw invalidToken();
        }

        TokenPayload payload = deserialize(payloadBytes);
        if (!TOKEN_VERSION.equals(payload.version()) || !memberId.equals(payload.memberId())) {
            throw invalidToken();
        }
        if (!clock.instant().isBefore(Instant.ofEpochSecond(payload.expiresAt()))) {
            throw new BadRequestException(PlaceExceptionMessage.EXPIRED_SELECTION_TOKEN.getMessage());
        }
        return new VerifiedPlaceCandidate(payload.candidate());
    }

    private void validateTokenFormat(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw invalidToken();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw invalidToken();
        }
        validateSecret();
    }

    private byte[] serialize(TokenPayload payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("장소 선택 토큰을 직렬화할 수 없습니다.", e);
        }
    }

    private TokenPayload deserialize(byte[] payload) {
        try {
            return objectMapper.readValue(payload, TokenPayload.class);
        } catch (IOException e) {
            throw invalidToken();
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.selectionTokenSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("장소 선택 토큰에 서명할 수 없습니다.", e);
        }
    }

    private byte[] decode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw invalidToken();
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private void validateSecret() {
        if (properties.selectionTokenSecret() == null
                || properties.selectionTokenSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("local-map.selection-token-secret은 32바이트 이상이어야 합니다.");
        }
    }

    private BadRequestException invalidToken() {
        return new BadRequestException(PlaceExceptionMessage.INVALID_SELECTION_TOKEN.getMessage());
    }

    private record TokenPayload(
            String version,
            Long memberId,
            long issuedAt,
            long expiresAt,
            PlaceCandidate candidate
    ) {
    }
}
