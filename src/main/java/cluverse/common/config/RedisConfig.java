package cluverse.common.config;

import cluverse.meta.properties.ViewSurgeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(ViewSurgeProperties.class)
public class RedisConfig {

    @Bean
    public RedisScript<Long> viewCountGetAndResetScript() {
        return longScript("redis/view_count_get_and_reset.lua");
    }

    @Bean
    public RedisScript<Long> viewCountIncreaseScript() {
        return longScript("redis/view_count_increase.lua");
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
}
