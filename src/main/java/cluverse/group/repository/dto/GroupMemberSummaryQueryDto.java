package cluverse.group.repository.dto;

public record GroupMemberSummaryQueryDto(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
}
