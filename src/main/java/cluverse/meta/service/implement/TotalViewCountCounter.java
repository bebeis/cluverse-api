package cluverse.meta.service.implement;

import cluverse.meta.repository.TotalViewCountRepository;
import cluverse.meta.repository.dto.TotalViewCountResult;
import cluverse.meta.repository.dto.TotalViewCountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TotalViewCountCounter {

    private static final int MAX_REINITIALIZE_COUNT = 3;

    private final TotalViewCountRepository totalViewCountRepository;
    private final ViewCountInitializer viewCountInitializer;
    private final LocalViewCountFallback localViewCountFallback;

    public ViewCountResult count(Long postId, String cookieId) {
        try {
            for (int attempt = 0; attempt < MAX_REINITIALIZE_COUNT; attempt++) {
                TotalViewCountResult result = totalViewCountRepository.count(postId, cookieId);
                if (result.status() == TotalViewCountStatus.REINITIALIZE) {
                    // 카운터가 없을 때 Lua가 1부터 만들지 않고 MySQL 누적값 적재를 먼저 요구한다.
                    viewCountInitializer.ensureInitialized(postId);
                    continue;
                }
                return new ViewCountResult(
                        result.viewCount(),
                        result.status() == TotalViewCountStatus.COUNTED,
                        ViewCountSource.REDIS_TOTAL
                );
            }
            throw new IllegalStateException("조회수 카운터 재초기화가 반복되었습니다: postId=" + postId);
        } catch (RedisConnectionFailureException | RedisSystemException exception) {
            // Redis 장애를 MySQL 쓰기로 전파하지 않고 인스턴스 로컬의 근사값으로 격리한다.
            return localViewCountFallback.count(postId, cookieId);
        }
    }
}
