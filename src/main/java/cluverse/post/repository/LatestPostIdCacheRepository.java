package cluverse.post.repository;

import cluverse.post.domain.PostCategory;
import cluverse.post.repository.dto.CachedLatestPostIds;
import cluverse.post.repository.dto.LatestPostCacheEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class LatestPostIdCacheRepository {

    private static final String KEY_PREFIX = "post:list:latest:";
    private static final int POST_ID_WIDTH = 19;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> readScript;
    private final RedisScript<Long> replaceScript;
    private final RedisScript<Long> addIfReadyScript;
    private final RedisScript<Long> invalidateScript;
    private final RedisScript<Long> unlockScript;
    private final Clock clock;

    public LatestPostIdCacheRepository(
            StringRedisTemplate redisTemplate,
            @Qualifier("readLatestPostIdsScript") RedisScript<List> readScript,
            @Qualifier("replaceLatestPostIdsScript") RedisScript<Long> replaceScript,
            @Qualifier("addLatestPostIdIfReadyScript") RedisScript<Long> addIfReadyScript,
            @Qualifier("invalidateLatestPostIdsScript") RedisScript<Long> invalidateScript,
            @Qualifier("unlockScript") RedisScript<Long> unlockScript,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.readScript = readScript;
        this.replaceScript = replaceScript;
        this.addIfReadyScript = addIfReadyScript;
        this.invalidateScript = invalidateScript;
        this.unlockScript = unlockScript;
        this.clock = clock;
    }

    public Optional<CachedLatestPostIds> read(
            Long boardId,
            PostCategory category,
            long offset,
            int limit
    ) {
        CacheKeys keys = keys(boardId, category);
        // ready 키와 ID 범위를 한 Lua에서 읽어, 워밍되지 않은 캐시와 정상적인 빈 목록을 구분한다.
        List<?> values = redisTemplate.execute(
                readScript,
                List.of(keys.ids(), keys.ready()),
                String.valueOf(offset),
                String.valueOf(offset + limit - 1)
        );
        if (values == null || values.isEmpty() || asLong(values.getFirst()) < 0) {
            return Optional.empty();
        }

        List<Long> postIds = values.stream()
                .skip(1)
                .map(value -> Long.valueOf(value.toString()))
                .toList();
        return Optional.of(new CachedLatestPostIds(postIds, asLong(values.getFirst())));
    }

    public boolean tryAcquireWarmupLock(
            Long boardId,
            PostCategory category,
            String owner,
            Duration lease
    ) {
        // owner token + lease를 저장해 워밍 서버가 중단돼도 다음 요청이 락을 회수할 수 있게 한다.
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                keys(boardId, category).lock(), owner, lease);
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseWarmupLock(Long boardId, PostCategory category, String owner) {
        redisTemplate.execute(unlockScript, List.of(keys(boardId, category).lock()), owner);
    }

    public long readVersion(Long boardId, PostCategory category) {
        String version = redisTemplate.opsForValue().get(keys(boardId, category).version());
        return version == null ? 0L : Long.parseLong(version);
    }

    public boolean replaceIfVersion(
            Long boardId,
            PostCategory category,
            long expectedVersion,
            List<LatestPostCacheEntry> entries,
            Duration ttl
    ) {
        CacheKeys keys = keys(boardId, category);
        List<String> arguments = new ArrayList<>(2 + entries.size() * 2);
        arguments.add(String.valueOf(expectedVersion));
        arguments.add(String.valueOf(ttl.toSeconds()));
        // score가 같은 게시글은 19자리 postId member의 역순으로 정렬되어 DB의 post_id DESC와 일치한다.
        for (LatestPostCacheEntry entry : entries) {
            arguments.add(member(entry.postId()));
            arguments.add(String.valueOf(score(entry.createdAt())));
        }

        Long replaced = redisTemplate.execute(
                replaceScript,
                List.of(keys.ids(), keys.ready(), keys.version()),
                arguments.toArray()
        );
        return replaced != null && replaced == 1L;
    }

    public boolean addIfReady(
            Long boardId,
            PostCategory category,
            Long postId,
            LocalDateTime createdAt,
            int maxEntries,
            Duration ttl
    ) {
        CacheKeys keys = keys(boardId, category);
        Long updated = redisTemplate.execute(
                addIfReadyScript,
                List.of(keys.ids(), keys.ready(), keys.version()),
                member(postId),
                String.valueOf(score(createdAt)),
                String.valueOf(maxEntries),
                String.valueOf(ttl.toSeconds())
        );
        return updated != null && updated == 1L;
    }

    public void invalidateBoard(Long boardId) {
        invalidate(boardId, null);
        for (PostCategory category : PostCategory.values()) {
            invalidate(boardId, category);
        }
    }

    private void invalidate(Long boardId, PostCategory category) {
        CacheKeys keys = keys(boardId, category);
        redisTemplate.execute(
                invalidateScript,
                List.of(keys.ids(), keys.ready(), keys.version())
        );
    }

    private CacheKeys keys(Long boardId, PostCategory category) {
        String filter = category == null ? "all" : category.name().toLowerCase();
        // 중괄호 안을 Redis Cluster hash tag로 사용해 Lua가 접근하는 네 키를 같은 슬롯에 배치한다.
        String prefix = KEY_PREFIX + "{" + boardId + ":" + filter + "}";
        return new CacheKeys(
                prefix + ":ids",
                prefix + ":ready",
                prefix + ":version",
                prefix + ":lock"
        );
    }

    private String member(Long postId) {
        return String.format("%0" + POST_ID_WIDTH + "d", postId);
    }

    private long score(LocalDateTime createdAt) {
        return createdAt.atZone(clock.getZone()).toInstant().toEpochMilli();
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    private record CacheKeys(String ids, String ready, String version, String lock) {
    }
}
