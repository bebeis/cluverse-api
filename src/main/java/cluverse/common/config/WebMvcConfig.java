package cluverse.common.config;

import cluverse.auth.properties.OAuth2Properties;
import cluverse.common.auth.AuthInterceptor;
import cluverse.common.auth.LoginMemberArgumentResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(OAuth2Properties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private static final List<String> PUBLIC_GET_PATH_PATTERNS = List.of(
            "/api/v1/posts",
            "/api/v1/posts/*",
            // 게시글 목록 조회 버전별 성능 비교 엔드포인트
            "/api/v2/posts",
            "/api/v3/posts",
            "/api/v4/posts",
            "/api/v1/comments",
            "/api/v1/universities",
            "/api/v1/universities/*",
            "/api/v1/groups",
            "/api/v1/groups/",
            "/api/v1/groups/*",
            "/api/v1/boards",
            "/api/v1/boards/*",
            "/api/v1/boards/*/home",
            "/api/v1/majors",
            "/api/v1/interests",
            "/api/v1/terms",
            "/api/v1/members/nickname/availability"
    );

    private static final List<String> PROTECTED_GET_PATH_PATTERNS = List.of(
            "/api/v1/groups/me"
    );

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://cluverse-web.vercel.app",
                        "http://localhost:3000"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(PUBLIC_GET_PATH_PATTERNS, PROTECTED_GET_PATH_PATTERNS))
                .addPathPatterns("/api/v1/**", "/api/v2/**", "/api/v3/**", "/api/v4/**")
                .excludePathPatterns(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/oauth/**",
                        "/actuator/**",
                        // 조회수 증가 버전별 성능 비교 엔드포인트 — 부하테스트용 비로그인 허용
                        "/api/v1/posts/*/view-count",
                        "/api/v2/posts/*/view-count",
                        "/api/v3/posts/*/view-count"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
