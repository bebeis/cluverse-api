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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostListCacheRepository {

    private static final String KEY_PREFIX = "post:list:latest:";
    private static final int POST_ID_WIDTH = 19;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> readScript;
    private final RedisScript<Long> replaceScript;
    private final RedisScript<Long> invalidateScript;
    private final RedisScript<Long> unlockScript;
    private final Clock clock;

    public PostListCacheRepository(
            StringRedisTemplate redisTemplate,
            @Qualifier("readLatestPostIdsScript") RedisScript<List> readScript,
            @Qualifier("replaceLatestPostIdsScript") RedisScript<Long> replaceScript,
            @Qualifier("invalidateLatestPostIdsScript") RedisScript<Long> invalidateScript,
            @Qualifier("unlockScript") RedisScript<Long> unlockScript,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.readScript = readScript;
        this.replaceScript = replaceScript;
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
        for (LatestPostCacheEntry entry : entries) {
            arguments.add(member(entry.postId()));
            arguments.add(String.valueOf(entry.createdAt()
                    .atZone(clock.getZone())
                    .toInstant()
                    .toEpochMilli()));
        }

        Long replaced = redisTemplate.execute(
                replaceScript,
                List.of(keys.ids(), keys.ready(), keys.version()),
                arguments.toArray()
        );
        return replaced != null && replaced == 1L;
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

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    private record CacheKeys(String ids, String ready, String version, String lock) {
    }
}
