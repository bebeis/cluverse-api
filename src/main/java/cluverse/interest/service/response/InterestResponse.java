package cluverse.interest.service.response;

import cluverse.interest.domain.Interest;

public record InterestResponse(
        Long interestId,
        Long boardId,
        String name,
        String category,
        Long parentId,
        int displayOrder
) {
    public static InterestResponse from(Interest interest) {
        return new InterestResponse(
                interest.getId(),
                interest.getBoardId(),
                interest.getName(),
                interest.getCategory(),
                interest.getParentId(),
                interest.getDisplayOrder()
        );
    }
}
