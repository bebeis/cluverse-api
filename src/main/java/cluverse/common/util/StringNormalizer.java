package cluverse.common.util;

import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.CommonExceptionMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringNormalizer {

    private static final String EMAIL_DOMAIN_SEPARATOR = "@";

    public static String requireTrimmed(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BadRequestException(CommonExceptionMessage.REQUIRED_VALUE_MISSING.getMessage());
        }
        return trimmed;
    }

    public static String requireTrimmedLowerCase(String value) {
        return requireTrimmed(value).toLowerCase(Locale.ROOT);
    }

    public static String requireNormalizedEmail(String email) {
        return requireTrimmedLowerCase(email);
    }

    public static String extractEmailDomain(String email) {
        String normalizedEmail = requireNormalizedEmail(email);
        int atIndex = normalizedEmail.lastIndexOf(EMAIL_DOMAIN_SEPARATOR);
        return normalizedEmail.substring(atIndex + 1);
    }

    public static String normalizeOptionalDomain(String domain) {
        String trimmedDomain = trimToNull(domain);
        if (trimmedDomain == null) {
            return null;
        }
        return trimmedDomain.toLowerCase(Locale.ROOT);
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
