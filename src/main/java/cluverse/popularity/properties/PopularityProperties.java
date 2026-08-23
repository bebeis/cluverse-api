package cluverse.popularity.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "popularity")
@Validated
public record PopularityProperties(
        @DefaultValue("100") @PositiveOrZero long defaultPromotionScore,
        @DefaultValue("3") @PositiveOrZero int scoreLikeWeight,
        @DefaultValue("2") @PositiveOrZero int scoreCommentWeight,
        @DefaultValue("48h") @NotNull Duration promotionWindow,
        @DefaultValue("7d") @NotNull Duration policySampleWindow,
        @DefaultValue("0.98") @DecimalMin(value = "0", inclusive = false) @DecimalMax("1")
        double policyPercentile,
        @DefaultValue("100") @Positive int policyMinSampleSize,
        @DefaultValue("0.30") @DecimalMin("0") @DecimalMax("1") double policySmoothingRatio,
        @DefaultValue("30s") @NotNull Duration finalizationInterval,
        @DefaultValue("500") @Positive int finalizationBatchSize
) {
}
