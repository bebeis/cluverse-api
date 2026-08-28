package cluverse.post.repository;

import cluverse.post.domain.PostCategory;
import cluverse.post.repository.dto.LatestPostCacheEntry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LatestPostIdCacheRepositoryTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void epoch_millisecond_score는_Double로_해석해도_현재_시간대에서_정확하다() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisScript<List> readScript = mock(RedisScript.class);
        RedisScript<Long> replaceScript = mock(RedisScript.class);
        RedisScript<Long> addIfReadyScript = mock(RedisScript.class);
        RedisScript<Long> invalidateScript = mock(RedisScript.class);
        RedisScript<Long> unlockScript = mock(RedisScript.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), SEOUL);
        LatestPostIdCacheRepository repository = new LatestPostIdCacheRepository(
                redisTemplate, readScript, replaceScript, addIfReadyScript,
                invalidateScript, unlockScript, clock);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 29, 9, 0, 0);
        when(redisTemplate.execute(eq(replaceScript), any(), any(Object[].class))).thenReturn(1L);

        repository.replaceIfVersion(
                3L,
                null,
                0L,
                List.of(new LatestPostCacheEntry(Long.MAX_VALUE, createdAt)),
                Duration.ofMinutes(3)
        );

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(eq(replaceScript), any(), arguments.capture());
        Object[] values = arguments.getValue();
        long epochMilli = createdAt.atZone(SEOUL).toInstant().toEpochMilli();
        double redisScore = Double.parseDouble(values[3].toString());

        assertThat((long) redisScore).isEqualTo(epochMilli);
        assertThat(values[2]).isEqualTo("9223372036854775807");
    }

    @Test
    void 준비된_캐시에_추가할_ID와_정렬값_크기_TTL을_Lua에_전달한다() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisScript<List> readScript = mock(RedisScript.class);
        RedisScript<Long> replaceScript = mock(RedisScript.class);
        RedisScript<Long> addIfReadyScript = mock(RedisScript.class);
        RedisScript<Long> invalidateScript = mock(RedisScript.class);
        RedisScript<Long> unlockScript = mock(RedisScript.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), SEOUL);
        LatestPostIdCacheRepository repository = new LatestPostIdCacheRepository(
                redisTemplate, readScript, replaceScript, addIfReadyScript,
                invalidateScript, unlockScript, clock);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 29, 9, 0);
        when(redisTemplate.execute(eq(addIfReadyScript), any(), any(Object[].class))).thenReturn(1L);

        boolean updated = repository.addIfReady(
                3L,
                PostCategory.INFORMATION,
                10L,
                createdAt,
                201,
                Duration.ofMinutes(3)
        );

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(eq(addIfReadyScript), any(), arguments.capture());
        assertThat(updated).isTrue();
        assertThat(arguments.getValue()).containsExactly(
                "0000000000000000010",
                String.valueOf(createdAt.atZone(SEOUL).toInstant().toEpochMilli()),
                "201",
                "180"
        );
    }
}
