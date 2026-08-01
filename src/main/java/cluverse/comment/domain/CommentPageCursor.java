package cluverse.comment.domain;

import cluverse.comment.exception.CommentExceptionMessage;
import cluverse.common.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public record CommentPageCursor(
        String path,
        LocalDateTime asOf,
        long snapshotMaxCommentId
) {

    private static final String DELIMITER = "\n";
    private static final int MAX_PATH_LENGTH = 255;

    public CommentPageCursor {
        path = path == null ? "" : path;
        validate(path, asOf, snapshotMaxCommentId);
    }

    public static CommentPageCursor first(LocalDateTime asOf, long snapshotMaxCommentId) {
        return new CommentPageCursor("", asOf, snapshotMaxCommentId);
    }

    public static CommentPageCursor decode(String encodedCursor) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] values = decoded.split(DELIMITER, -1);
            if (values.length != 3) {
                throw invalidCursor();
            }
            return new CommentPageCursor(
                    values[2],
                    LocalDateTime.parse(values[0]),
                    Long.parseLong(values[1])
            );
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    public String encode() {
        String rawCursor = asOf + DELIMITER + snapshotMaxCommentId + DELIMITER + path;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    public boolean hasPath() {
        return !path.isBlank();
    }

    private static void validate(String path, LocalDateTime asOf, long snapshotMaxCommentId) {
        if (asOf == null || snapshotMaxCommentId < 0 || path.length() > MAX_PATH_LENGTH || path.contains(DELIMITER)) {
            throw invalidCursor();
        }
    }

    private static BadRequestException invalidCursor() {
        return new BadRequestException(CommentExceptionMessage.COMMENT_CURSOR_INVALID.getMessage());
    }
}
