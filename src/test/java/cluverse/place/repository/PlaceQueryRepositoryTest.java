package cluverse.place.repository;

import cluverse.place.domain.PlaceCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PlaceQueryRepositoryTest {

    @Autowired
    private PlaceQueryRepository placeQueryRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 공개된_현지_추천만_지도에_집계하고_익명_작성자는_가린다() {
        jdbcTemplate.update("""
                INSERT INTO member (
                    member_id, nickname, university_id, status, verification_status, role, created_at, updated_at
                ) VALUES (100, '익명작성자', 1, 'ACTIVE', 'APPROVED', 'MEMBER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO place (
                    place_id, provider, source_fingerprint, name, category, latitude, longitude,
                    synchronized_at, created_at, updated_at
                ) VALUES (
                    200, 'NAVER', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    '학교 앞 카페', 'CAFE', 37.55, 126.94, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO post (
                    post_id, board_id, member_id, status, title, content, category, is_anonymous,
                    is_pinned, is_external_visible, created_at, updated_at
                ) VALUES (
                    300, 1, 100, 'ACTIVE', '추천', '좋아요', 'INFORMATION', TRUE,
                    FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO post_place (
                    post_place_id, post_id, place_id, display_order, author_university_id,
                    university_campus_id, recommended, created_at, updated_at
                ) VALUES (400, 300, 200, 0, 1, 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        var markers = placeQueryRepository.findMarkers(1L, 10L, PlaceCategory.CAFE);
        var contents = placeQueryRepository.findContents(200L, null, null, null, 10);

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().recommendationCount()).isEqualTo(1);
        assertThat(contents).hasSize(1);
        assertThat(contents.getFirst().authorId()).isNull();
        assertThat(contents.getFirst().authorNickname()).isNull();
    }
}
