package cluverse.place.client;

import cluverse.place.domain.PlaceProvider;
import cluverse.place.domain.PlaceSourceCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(prefix = "local-map", name = "provider-mode", havingValue = "STUB")
public class StubPlaceSearchClient implements PlaceSearchClient {

    private static final List<PlaceSourceCandidate> CANDIDATES = List.of(
            new PlaceSourceCandidate(
                    PlaceProvider.NAVER,
                    "클루버스 카페",
                    "음식점>카페,디저트",
                    "서울특별시 서대문구 신촌동 1",
                    "서울특별시 서대문구 연세로 1",
                    new BigDecimal("37.5510000"),
                    new BigDecimal("126.9410000"),
                    "https://example.test/places/cafe"
            ),
            new PlaceSourceCandidate(
                    PlaceProvider.NAVER,
                    "클루버스 식당",
                    "음식점>한식",
                    "서울특별시 서대문구 신촌동 2",
                    "서울특별시 서대문구 연세로 2",
                    new BigDecimal("37.5511000"),
                    new BigDecimal("126.9415000"),
                    "https://example.test/places/food"
            )
    );

    private final AtomicLong delayMillis = new AtomicLong(300);
    private final AtomicLong searchCalls = new AtomicLong();

    @Override
    public List<PlaceSourceCandidate> search(String query) {
        searchCalls.incrementAndGet();
        sleep(delayMillis.get());
        return CANDIDATES;
    }

    public void reset(long delayMillis) {
        if (delayMillis < 0 || delayMillis > 10_000) {
            throw new IllegalArgumentException("mock 지연은 0~10000ms여야 합니다.");
        }
        this.delayMillis.set(delayMillis);
        searchCalls.set(0);
    }

    public long delayMillis() {
        return delayMillis.get();
    }

    public long searchCalls() {
        return searchCalls.get();
    }

    private void sleep(long delayMillis) {
        try {
            TimeUnit.MILLISECONDS.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("provider mock 대기가 중단됐습니다.", e);
        }
    }
}
