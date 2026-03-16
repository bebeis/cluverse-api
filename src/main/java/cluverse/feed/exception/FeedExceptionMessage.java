package cluverse.feed.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedExceptionMessage {

    INVALID_CURSOR("유효하지 않은 피드 커서입니다.");

    private final String message;
}
