package cluverse.place.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.place.repository.dto.PlaceContentQueryResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceContentCursorCodecTest {

    private final PlaceContentCursorCodec codec = new PlaceContentCursorCodec();

    @Test
    void 콘텐츠의_정렬_키를_왕복한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 20, 30, 0);
        PlaceContentQueryResult content = new PlaceContentQueryResult(
                "POST", 10L, "제목", "내용", 1L, "작성자", createdAt);

        PlaceContentCursorCodec.Cursor cursor = codec.decode(codec.encode(content));

        assertThat(cursor.createdAt()).isEqualTo(createdAt);
        assertThat(cursor.contentType()).isEqualTo("POST");
        assertThat(cursor.contentId()).isEqualTo(10L);
    }

    @Test
    void 알_수_없는_콘텐츠_타입은_거부한다() {
        String invalid = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "2026-08-01T20:30:00|UNKNOWN|10".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decode(invalid)).isInstanceOf(BadRequestException.class);
    }
}
