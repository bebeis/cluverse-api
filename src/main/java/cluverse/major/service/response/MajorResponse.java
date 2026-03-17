package cluverse.major.service.response;

import cluverse.major.domain.Major;

public record MajorResponse(
        Long majorId,
        Long boardId,
        String name,
        Long parentId,
        int depth,
        int displayOrder
) {
    public static MajorResponse from(Major major) {
        return new MajorResponse(
                major.getId(),
                major.getBoardId(),
                major.getName(),
                major.getParentId(),
                major.getDepth(),
                major.getDisplayOrder()
        );
    }
}
