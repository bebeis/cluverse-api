package cluverse.place.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.place.exception.PlaceExceptionMessage;
import cluverse.place.repository.dto.PlaceContentQueryResult;
import cluverse.place.service.response.PlaceContentType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Component
public class PlaceContentCursorCodec {

    private static final int MAX_CURSOR_LENGTH = 512;

    public String encode(PlaceContentQueryResult content) {
        String value = content.createdAt() + "|" + content.contentType() + "|" + content.contentId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return Cursor.firstPage();
        }
        if (cursor.length() > MAX_CURSOR_LENGTH) {
            throw invalidCursor();
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] values = decoded.split("\\|", -1);
            if (values.length != 3) {
                throw invalidCursor();
            }
            PlaceContentType contentType = PlaceContentType.valueOf(values[1]);
            return new Cursor(LocalDateTime.parse(values[0]), contentType.name(), Long.parseLong(values[2]));
        } catch (RuntimeException e) {
            throw invalidCursor();
        }
    }

    private BadRequestException invalidCursor() {
        return new BadRequestException(PlaceExceptionMessage.INVALID_PLACE_CONTENT_CURSOR.getMessage());
    }

    public record Cursor(LocalDateTime createdAt, String contentType, Long contentId) {
        private static Cursor firstPage() {
            return new Cursor(null, null, null);
        }
    }
}
