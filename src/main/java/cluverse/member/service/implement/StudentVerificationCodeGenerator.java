package cluverse.member.service.implement;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;

@Component
public class StudentVerificationCodeGenerator {

    private static final int CODE_BOUND = 1_000_000;
    private static final String CODE_FORMAT = "%06d";
    private static final String CHALLENGE_ID_PREFIX = "evc_";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode() {
        return CODE_FORMAT.formatted(secureRandom.nextInt(CODE_BOUND));
    }

    public String generateChallengeId() {
        return CHALLENGE_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
