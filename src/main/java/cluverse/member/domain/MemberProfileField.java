package cluverse.member.domain;

import cluverse.common.exception.BadRequestException;
import cluverse.member.exception.MemberExceptionMessage;
import java.util.Arrays;

public enum MemberProfileField {
    UNIVERSITY,
    ENTRANCE_YEAR,
    BIO,
    PROFILE_IMAGE_URL,
    LINK_GITHUB,
    LINK_NOTION,
    LINK_PORTFOLIO,
    LINK_INSTAGRAM,
    LINK_ETC;

    public static MemberProfileField from(String value) {
        return Arrays.stream(values())
                .filter(field -> field.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(MemberExceptionMessage.INVALID_PROFILE_VISIBLE_FIELD.getMessage()));
    }
}
