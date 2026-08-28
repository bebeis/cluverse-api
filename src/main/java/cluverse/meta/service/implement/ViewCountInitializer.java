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
            // 이미 초기화된 대부분의 요청은 락을 건드리지 않는 빠른 경로로 끝낸다.
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
            // 락을 기다리는 사이 다른 요청이 적재했을 수 있으므로 MySQL을 읽기 전에 다시 확인한다.
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
            // 임대 시간이 끝난 뒤 다른 요청이 얻은 락을 지우지 않도록 소유 토큰을 확인한다.
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
