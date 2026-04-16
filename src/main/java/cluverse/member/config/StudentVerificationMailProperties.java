package cluverse.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDateTime;

@ConfigurationProperties(prefix = "student-verification.mail")
public record StudentVerificationMailProperties(
        String from,
        String subject
) {

    private static final String DEFAULT_FROM = "no-reply@cluverse.app";
    private static final String DEFAULT_SUBJECT = "[Cluverse] 학교 이메일 인증 코드";
    private static final String BODY_TEMPLATE = """
            학교 이메일 인증 코드: %s

            이 코드는 %s까지 유효합니다.
            본인이 요청하지 않았다면 이 메일을 무시해주세요.
            """;

    public String fromAddress() {
        return from == null || from.isBlank() ? DEFAULT_FROM : from;
    }

    public String subjectText() {
        return subject == null || subject.isBlank() ? DEFAULT_SUBJECT : subject;
    }

    public String bodyText(String code, LocalDateTime expiresAt) {
        return BODY_TEMPLATE.formatted(code, expiresAt);
    }
}
