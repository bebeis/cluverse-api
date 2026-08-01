package cluverse.place.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "local-map")
@Validated
public record LocalMapProperties(
        @DefaultValue("NAVER") @NotNull PlaceProviderMode providerMode,
        @DefaultValue("https://openapi.naver.com") String providerBaseUrl,
        @DefaultValue("") String naverClientId,
        @DefaultValue("") String naverClientSecret,
        @DefaultValue("") String selectionTokenSecret,
        @DefaultValue("15m") @NotNull Duration selectionTokenTtl,
        @DefaultValue("500ms") @NotNull Duration connectTimeout,
        @DefaultValue("2s") @NotNull Duration readTimeout,
        @DefaultValue("false") boolean experimentEndpointsEnabled,
        @DefaultValue("") String benchmarkToken
) {
}
