package cluverse.common.config;

import cluverse.meta.properties.ViewSurgeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(ViewSurgeProperties.class)
public class RedisConfig {

    @Bean
    public RedisScript<Long> viewCountGetAndResetScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/view_count_get_and_reset.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
