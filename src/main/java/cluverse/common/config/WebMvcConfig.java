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
            "/api/v1/comments"
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
        registry.addInterceptor(new AuthInterceptor(PUBLIC_GET_PATH_PATTERNS))
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/oauth/token",
                        "/api/v1/universities",
                        "/api/v1/universities/*",
                        "/oauth2/**",
                        "/actuator/**"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
