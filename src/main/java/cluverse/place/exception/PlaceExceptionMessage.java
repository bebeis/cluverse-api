package cluverse.place.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceExceptionMessage {
    INVALID_SELECTION_TOKEN("장소 선택 정보가 올바르지 않습니다. 장소를 다시 검색해주세요."),
    EXPIRED_SELECTION_TOKEN("장소 선택 정보가 만료되었습니다. 장소를 다시 검색해주세요."),
    PLACE_NOT_FOUND("존재하지 않는 장소입니다."),
    INVALID_PLACE_CONTENT_CURSOR("장소 콘텐츠 커서가 올바르지 않습니다."),
    SELECTED_PLACE_NOT_FOUND("선택한 장소를 검색 결과에서 확인할 수 없습니다."),
    PLACE_SEARCH_UNAVAILABLE("장소 검색 서비스에 일시적으로 연결할 수 없습니다."),
    TOO_MANY_POST_PLACES("게시글에는 장소를 최대 5개까지 첨부할 수 있습니다."),
    DUPLICATED_PLACE_ATTACHMENT("같은 장소를 중복으로 첨부할 수 없습니다.");

    private final String message;
}
