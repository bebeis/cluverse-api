package cluverse.member.service.response;

public record MemberProfileSummaryResponse(
        Long universityId,
        String universityName,
        String universityBadgeImageUrl
) {
    public static MemberProfileSummaryResponse empty() {
        return new MemberProfileSummaryResponse(null, null, null);
    }
}
