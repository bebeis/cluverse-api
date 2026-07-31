package cluverse.meta.repository;

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
 */
@Repository
public class PendingViewCountRepository {

    private static final String KEY_PREFIX = "view:pending:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> viewCountGetAndResetScript;
    private final byte[] getAndResetScriptBytes;

    public PendingViewCountRepository(StringRedisTemplate redisTemplate, RedisScript<Long> viewCountGetAndResetScript) {
        this.redisTemplate = redisTemplate;
        this.viewCountGetAndResetScript = viewCountGetAndResetScript;
        this.getAndResetScriptBytes = viewCountGetAndResetScript.getScriptAsString().getBytes(StandardCharsets.UTF_8);
    }

    public void increase(Long postId) {
        redisTemplate.opsForValue().increment(key(postId));
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

        List<Long> values = new ArrayList<>(results.size());
        for (Object result : results) {
            values.add(result == null ? 0L : (Long) result);
        }
        return values;
    }

    public void restore(Long postId, long delta) {
        redisTemplate.opsForValue().increment(key(postId), delta);
    }

    public void delete(Long postId) {
        redisTemplate.delete(key(postId));
    }

    private String key(Long postId) {
        return KEY_PREFIX + postId;
    }

    private byte[] rawKey(Long postId) {
        return key(postId).getBytes(StandardCharsets.UTF_8);
    }
}
