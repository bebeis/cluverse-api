package cluverse.common.util;

import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.CommonExceptionMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringNormalizer {

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

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
