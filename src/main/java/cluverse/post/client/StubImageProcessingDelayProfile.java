package cluverse.post.client;

import java.time.Duration;

final class StubImageProcessingDelayProfile {

    private final Duration average;

    StubImageProcessingDelayProfile(Duration average) {
        if (average == null || average.isNegative() || average.isZero()) {
            throw new IllegalArgumentException("stub 평균 지연은 0보다 커야 합니다.");
        }
        this.average = average;
    }

    Duration delayFor(PostImageProcessCommand command) {
        int hash = 31 * command.requestId().hashCode() + command.displayOrder();
        return delayForBucket(Math.floorMod(hash, 100));
    }

    Duration delayForBucket(int bucket) {
        if (bucket < 0 || bucket >= 100) {
            throw new IllegalArgumentException("지연 bucket은 0~99 사이여야 합니다.");
        }
        if (bucket < 75) {
            return average.minusMillis(Math.min(20, average.toMillis() - 1));
        }
        if (bucket < 95) {
            return average.plusMillis(40);
        }
        return average.plusMillis(120);
    }
}
