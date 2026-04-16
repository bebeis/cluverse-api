package cluverse.member.client;

import java.time.LocalDateTime;

public interface StudentVerificationEmailClient {

    void sendVerificationCode(String email, String code, LocalDateTime expiresAt);
}
