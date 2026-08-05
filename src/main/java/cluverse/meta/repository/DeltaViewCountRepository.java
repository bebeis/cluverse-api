package cluverse.meta.repository;

import cluverse.meta.properties.ViewCountProperties;
import cluverse.meta.repository.dto.DeltaViewCountResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DeltaViewCountRepository {

    private static final String DELTA_KEY_PREFIX = "view:%s:delta:";
    private static final String DUPLICATE_KEY_PREFIX = "view:%s:dedupe:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> countDeltaScript;
    private final RedisScript<Long> getAndDeleteScript;
    private final ViewCountProperties properties;

    public DeltaViewCountRepository(
            StringRedisTemplate redisTemplate,
            @Qualifier("countDeltaScript") RedisScript<List> countDeltaScript,
            @Qualifier("getAndDeleteScript") RedisScript<Long> getAndDeleteScript,
            ViewCountProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.countDeltaScript = countDeltaScript;
        this.getAndDeleteScript = getAndDeleteScript;
        this.properties = properties;
    }

    public DeltaViewCountResult count(DeltaViewCountVersion version, Long postId, String cookieId) {
        List<?> values = redisTemplate.execute(
                countDeltaScript,
                List.of(duplicateKey(version, postId, cookieId), deltaKey(version, postId)),
                String.valueOf(properties.duplicateTtl().toSeconds())
        );
        if (values == null || values.size() != 2) {
            throw new IllegalStateException("Redis 증분 조회수 결과가 올바르지 않습니다.");
        }
        return new DeltaViewCountResult(asLong(values.get(0)) == 1L, asLong(values.get(1)));
    }

    public long take(DeltaViewCountVersion version, Long postId) {
        Long delta = redisTemplate.execute(getAndDeleteScript, List.of(deltaKey(version, postId)));
        return delta == null ? 0L : delta;
    }

    public void restore(DeltaViewCountVersion version, Long postId, long delta) {
        redisTemplate.opsForValue().increment(deltaKey(version, postId), delta);
    }

    public List<Long> findPostIds(DeltaViewCountVersion version) {
        List<Long> postIds = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(deltaKeyPrefix(version) + "*")
                .count(properties.scanCount())
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                postIds.add(parsePostId(cursor.next()));
            }
        }
        return postIds;
    }

    private String deltaKey(DeltaViewCountVersion version, Long postId) {
        return deltaKeyPrefix(version) + "{" + postId + "}";
    }

    private String deltaKeyPrefix(DeltaViewCountVersion version) {
        return DELTA_KEY_PREFIX.formatted(version.keySegment());
    }

    private String duplicateKey(DeltaViewCountVersion version, Long postId, String cookieId) {
        return DUPLICATE_KEY_PREFIX.formatted(version.keySegment()) + "{" + postId + "}:" + cookieId;
    }

    private Long parsePostId(String key) {
        int start = key.lastIndexOf('{');
        int end = key.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("조회수 증분 키 형식이 올바르지 않습니다: " + key);
        }
        return Long.valueOf(key.substring(start + 1, end));
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }
}
