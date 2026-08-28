package cluverse.meta.repository;

import cluverse.meta.properties.ViewCountProperties;
import cluverse.meta.repository.dto.ResidentViewCount;
import cluverse.meta.repository.dto.TotalViewCountResult;
import cluverse.meta.repository.dto.TotalViewCountStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TotalViewCountRepository {

    private static final String COUNTER_KEY_PREFIX = "view:v4:counter:";
    private static final String DUPLICATE_KEY_PREFIX = "view:v4:dedupe:";
    private static final String INITIALIZATION_KEY_PREFIX = "view:v4:init:";
    private static final String COUNT_FIELD = "count";
    private static final String LAST_COUNTED_AT_FIELD = "last_counted_at";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> countTotalScript;
    private final RedisScript<Long> unlockScript;
    private final RedisScript<Long> deleteInactiveCounterScript;
    private final RedisScript<Long> ensureTotalAtLeastScript;
    private final ViewCountProperties properties;
    private final Clock clock;

    public TotalViewCountRepository(
            StringRedisTemplate redisTemplate,
            @Qualifier("countTotalScript") RedisScript<List> countTotalScript,
            @Qualifier("unlockScript") RedisScript<Long> unlockScript,
            @Qualifier("deleteInactiveCounterScript") RedisScript<Long> deleteInactiveCounterScript,
            @Qualifier("ensureTotalAtLeastScript") RedisScript<Long> ensureTotalAtLeastScript,
            ViewCountProperties properties,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.countTotalScript = countTotalScript;
        this.unlockScript = unlockScript;
        this.deleteInactiveCounterScript = deleteInactiveCounterScript;
        this.ensureTotalAtLeastScript = ensureTotalAtLeastScript;
        this.properties = properties;
        this.clock = clock;
    }

    public TotalViewCountResult count(Long postId, String cookieId) {
        List<?> values = redisTemplate.execute(
                countTotalScript,
                List.of(counterKey(postId), duplicateKey(postId, cookieId)),
                String.valueOf(properties.duplicateTtl().toSeconds()),
                String.valueOf(clock.millis())
        );
        if (values == null || values.size() != 2) {
            throw new IllegalStateException("Redis 전체 조회수 결과가 올바르지 않습니다.");
        }
        return new TotalViewCountResult(
                TotalViewCountStatus.from(asLong(values.get(0))),
                asLong(values.get(1))
        );
    }

    public Long read(Long postId) {
        Object count = redisTemplate.opsForHash().get(counterKey(postId), COUNT_FIELD);
        return count == null ? null : Long.valueOf(count.toString());
    }

    public boolean tryAcquireInitialization(Long postId, String ownerToken) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                initializationKey(postId),
                ownerToken,
                properties.initializationLockLease()
        );
        return Boolean.TRUE.equals(acquired);
    }

    public void initializeIfAbsent(Long postId, long viewCount) {
        String key = counterKey(postId);
        redisTemplate.opsForHash().putIfAbsent(key, COUNT_FIELD, String.valueOf(viewCount));
        redisTemplate.opsForHash().putIfAbsent(key, LAST_COUNTED_AT_FIELD, "0");
    }

    public void releaseInitialization(Long postId, String ownerToken) {
        redisTemplate.execute(unlockScript, List.of(initializationKey(postId)), ownerToken);
    }

    public long increaseBy(Long postId, long delta) {
        return redisTemplate.opsForHash().increment(counterKey(postId), COUNT_FIELD, delta);
    }

    public long ensureAtLeast(Long postId, long base) {
        Long count = redisTemplate.execute(
                ensureTotalAtLeastScript,
                List.of(counterKey(postId)),
                String.valueOf(base)
        );
        if (count == null) {
            throw new IllegalStateException("Redis 전체 조회수 기준값 복구에 실패했습니다.");
        }
        return count;
    }

    public List<ResidentViewCount> findAll() {
        return scan(false);
    }

    public List<ResidentViewCount> findInactive() {
        return scan(true);
    }

    public boolean deleteIfUnchanged(ResidentViewCount counter) {
        Long deleted = redisTemplate.execute(
                deleteInactiveCounterScript,
                List.of(counterKey(counter.postId())),
                String.valueOf(counter.viewCount()),
                String.valueOf(counter.lastCountedAtMillis())
        );
        return deleted != null && deleted == 1L;
    }

    private List<ResidentViewCount> scan(boolean inactiveOnly) {
        // SCAN count는 반환 개수의 보장이 아닌 힌트다. findAll은 전체를 모으고 비활성 조회만 batchSize로 제한한다.
        long inactiveBefore = clock.millis() - properties.inactiveAfter().toMillis();
        List<ResidentViewCount> counters = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(COUNTER_KEY_PREFIX + "*")
                .count(properties.scanCount())
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext() && (!inactiveOnly || counters.size() < properties.batchSize())) {
                String key = cursor.next();
                Map<Object, Object> values = redisTemplate.opsForHash().entries(key);
                Long count = value(values, COUNT_FIELD);
                Long lastCountedAt = value(values, LAST_COUNTED_AT_FIELD);
                if (count == null || lastCountedAt == null) {
                    continue;
                }
                if (!inactiveOnly || lastCountedAt <= inactiveBefore) {
                    counters.add(new ResidentViewCount(parsePostId(key), count, lastCountedAt));
                }
            }
        }
        return counters;
    }

    private Long value(Map<Object, Object> values, String field) {
        Object value = values.get(field);
        return value == null ? null : Long.valueOf(value.toString());
    }

    private String counterKey(Long postId) {
        // 조회 Lua가 함께 다루는 카운터와 중복 방지 키를 Redis Cluster의 같은 슬롯에 둔다.
        return COUNTER_KEY_PREFIX + "{" + postId + "}";
    }

    private String duplicateKey(Long postId, String cookieId) {
        return DUPLICATE_KEY_PREFIX + "{" + postId + "}:" + cookieId;
    }

    private String initializationKey(Long postId) {
        return INITIALIZATION_KEY_PREFIX + "{" + postId + "}";
    }

    private Long parsePostId(String key) {
        int start = key.lastIndexOf('{');
        int end = key.lastIndexOf('}');
        return Long.valueOf(key.substring(start + 1, end));
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }
}
