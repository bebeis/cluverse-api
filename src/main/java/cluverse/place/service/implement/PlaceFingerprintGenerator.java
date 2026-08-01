package cluverse.place.service.implement;

import cluverse.place.domain.PlaceSourceCandidate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class PlaceFingerprintGenerator {

    private static final int COORDINATE_SCALE = 7;

    public String generate(PlaceSourceCandidate candidate) {
        String source = String.join("|",
                candidate.provider().name(),
                normalize(candidate.name()),
                normalize(resolveAddress(candidate)),
                normalizeCoordinate(candidate.longitude()),
                normalizeCoordinate(candidate.latitude())
        );
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String resolveAddress(PlaceSourceCandidate candidate) {
        return candidate.roadAddress() == null || candidate.roadAddress().isBlank()
                ? candidate.address()
                : candidate.roadAddress();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeCoordinate(BigDecimal value) {
        return value.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }
}
