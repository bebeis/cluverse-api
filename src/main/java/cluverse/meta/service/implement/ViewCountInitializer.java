package cluverse.meta.service.implement;

import cluverse.meta.properties.ViewCountProperties;
import cluverse.meta.repository.TotalViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ViewCountInitializer {

    private final TotalViewCountRepository totalViewCountRepository;
    private final PostMetaReader postMetaReader;
    private final ViewCountProperties properties;

    public long ensureInitialized(Long postId) {
        for (int attempt = 0; attempt < properties.initializationAttempts(); attempt++) {
            Long existing = totalViewCountRepository.read(postId);
            if (existing != null) {
                return existing;
            }

            String ownerToken = UUID.randomUUID().toString();
            if (totalViewCountRepository.tryAcquireInitialization(postId, ownerToken)) {
                return initializeAsOwner(postId, ownerToken);
            }
            pause();
        }
        throw new IllegalStateException("조회수 카운터 초기화 대기 시간을 초과했습니다: postId=" + postId);
    }

    private long initializeAsOwner(Long postId, String ownerToken) {
        try {
            Long existing = totalViewCountRepository.read(postId);
            if (existing != null) {
                return existing;
            }
            long base = postMetaReader.readViewCount(postId);
            totalViewCountRepository.initializeIfAbsent(postId, base);
            Long initialized = totalViewCountRepository.read(postId);
            if (initialized == null) {
                throw new IllegalStateException("조회수 카운터 초기화 결과가 사라졌습니다: postId=" + postId);
            }
            return initialized;
        } finally {
            totalViewCountRepository.releaseInitialization(postId, ownerToken);
        }
    }

    private void pause() {
        try {
            Thread.sleep(properties.initializationWait().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("조회수 카운터 초기화 대기가 중단되었습니다.", exception);
        }
    }
}
