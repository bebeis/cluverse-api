package cluverse.place.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubPlaceSearchClientTest {

    private final StubPlaceSearchClient client = new StubPlaceSearchClient();

    @Test
    void 검색할_때마다_고정된_mock_후보와_호출_수를_반환한다() {
        client.reset(0);

        var first = client.search("연세대 카페");
        var second = client.search("다른 검색어");

        assertThat(first).hasSize(2).isEqualTo(second);
        assertThat(first.getFirst().name()).isEqualTo("클루버스 카페");
        assertThat(client.searchCalls()).isEqualTo(2);
    }

    @Test
    void reset은_지연과_호출_수를_함께_초기화한다() {
        client.reset(0);
        client.search("연세대 카페");

        client.reset(300);

        assertThat(client.delayMillis()).isEqualTo(300);
        assertThat(client.searchCalls()).isZero();
    }
}
