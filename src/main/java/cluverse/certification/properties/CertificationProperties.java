package cluverse.certification.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "certification")
public record CertificationProperties(
        @DefaultValue("https://apis.data.go.kr") String providerBaseUrl,
        @DefaultValue("") @NotBlank String serviceKey,
        @DefaultValue("2s") @NotNull Duration connectTimeout,
        @DefaultValue("10s") @NotNull Duration readTimeout,
        @DefaultValue("12h") @NotNull Duration cacheTtl
) {
}
