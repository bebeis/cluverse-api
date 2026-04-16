package cluverse.member.service;

import cluverse.member.service.implement.StudentVerificationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudentVerificationCleanupService {

    private static final long CLEANUP_FIXED_DELAY_MILLISECONDS = 3_600_000L;

    private final StudentVerificationWriter studentVerificationWriter;

    @Scheduled(fixedDelay = CLEANUP_FIXED_DELAY_MILLISECONDS)
    @Transactional
    public void expirePendingEmailChallenges() {
        studentVerificationWriter.expirePendingEmailChallenges(LocalDateTime.now());
    }
}
