package cluverse.member.service.response;

import cluverse.member.domain.Terms;

import java.time.LocalDateTime;

public record TermsResponse(
        Long termsId,
        String termsType,
        String title,
        String content,
        String version,
        boolean required,
        LocalDateTime effectiveAt
) {
    public static TermsResponse from(Terms terms) {
        return new TermsResponse(
                terms.getId(),
                terms.getTermsType(),
                terms.getTitle(),
                terms.getContent(),
                terms.getVersion(),
                terms.isRequired(),
                terms.getEffectiveAt()
        );
    }
}
