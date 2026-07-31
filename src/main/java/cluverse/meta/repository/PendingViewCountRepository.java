package cluverse.meta.repository;

import cluverse.meta.properties.ViewSurgeProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 미반영 조회수 증가량 버퍼 (조회수 증가 API V4).
 * 전체 조회수가 아니라 아직 post_view_count에 반영하지 않은 증가량만 둔다.
 * 모든 증가는 TTL을 무장한 Lua로 실행한다 — 추적 종료 후 유입된 고아 키는 TTL 뒤에 사라진다.
 */
@Repository
public class PendingViewCountRepository {

    private static final String KEY_PREFIX = "view:pending:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> viewCountGetAndResetScript;
    private final RedisScript<Long> viewCountIncreaseScript;
    private final byte[] getAndResetScriptBytes;
    private final String pendingTtlMillis;

    public PendingViewCountRepository(
            StringRedisTemplate redisTemplate,
            @Qualifier("viewCountGetAndResetScript") RedisScript<Long> viewCountGetAndResetScript,
            @Qualifier("viewCountIncreaseScript") RedisScript<Long> viewCountIncreaseScript,
            ViewSurgeProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.viewCountGetAndResetScript = viewCountGetAndResetScript;
        this.viewCountIncreaseScript = viewCountIncreaseScript;
        this.getAndResetScriptBytes = viewCountGetAndResetScript.getScriptAsString().getBytes(StandardCharsets.UTF_8);
        this.pendingTtlMillis = String.valueOf(
                properties.trackingTtl().plus(properties.extension()).plus(properties.grace()).toMillis());
    }

    public void increase(Long postId) {
        increaseBy(postId, 1L);
    }

    /**
     * 반영 실패가 확실할 때 증가량을 되돌린다.
     */
    public void restore(Long postId, long delta) {
        increaseBy(postId, delta);
    }

    public long getAndReset(Long postId) {
        Long value = redisTemplate.execute(viewCountGetAndResetScript, List.of(key(postId)));
        if (value == null) {
            return 0L;
        }
        return value;
    }

    /**
     * get-and-reset을 파이프라인으로 일괄 실행한다. 반환 순서는 입력 순서와 같다.
     */
    public List<Long> getAndResetAll(List<Long> postIds) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long postId : postIds) {
                connection.scriptingCommands()
                        .eval(getAndResetScriptBytes, ReturnType.INTEGER, 1, rawKey(postId));
            }
            return null;
        });

        // 위치로 짝짓기 때문에 개수가 어긋나면 남의 글에 증가량이 반영된다 — 계약 강제
        if (results.size() != postIds.size()) {
            throw new IllegalStateException(
                    "파이프라인 결과 개수 불일치: 요청 %d, 응답 %d".formatted(postIds.size(), results.size()));
        }

        List<Long> values = new ArrayList<>(results.size());
        for (Object result : results) {
            values.add(result == null ? 0L : (Long) result);
        }
        return values;
    }

    public void delete(Long postId) {
        redisTemplate.delete(key(postId));
    }

    private void increaseBy(Long postId, long delta) {
        redisTemplate.execute(viewCountIncreaseScript, List.of(key(postId)), pendingTtlMillis, String.valueOf(delta));
    }

    private String key(Long postId) {
        return KEY_PREFIX + postId;
    }

    private byte[] rawKey(Long postId) {
        return key(postId).getBytes(StandardCharsets.UTF_8);
    }
}
