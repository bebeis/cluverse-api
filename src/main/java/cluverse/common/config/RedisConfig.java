package cluverse.common.config;

import cluverse.meta.properties.ViewCountProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

@Configuration
@EnableConfigurationProperties(ViewCountProperties.class)
public class RedisConfig {

    @Bean
    public RedisScript<List> countDeltaScript() {
        return listScript("redis/count_delta.lua");
    }

    @Bean
    public RedisScript<Long> getAndDeleteScript() {
        return longScript("redis/get_and_delete.lua");
    }

    @Bean
    public RedisScript<List> countTotalScript() {
        return listScript("redis/count_total.lua");
    }

    @Bean
    public RedisScript<Long> unlockScript() {
        return longScript("redis/unlock.lua");
    }

    @Bean
    public RedisScript<Long> deleteInactiveCounterScript() {
        return longScript("redis/delete_inactive_counter.lua");
    }

    @Bean
    public RedisScript<Long> ensureTotalAtLeastScript() {
        return longScript("redis/ensure_total_at_least.lua");
    }

    /**
     * 인스턴스마다 JVM 기본 타임존이 다르면 zone 없는 DATETIME 비교가 어긋난다 — 명시 고정.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    private RedisScript<Long> longScript(String classPath) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(classPath));
        script.setResultType(Long.class);
        return script;
    }

    @SuppressWarnings("rawtypes")
    private RedisScript<List> listScript(String classPath) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(classPath));
        script.setResultType(List.class);
        return script;
    }
}
