package cluverse.member.service.response;

public record MemberInterestResponse(
        Long interestId,
        String interestName,
        String category
) {
    public static MemberInterestResponse from(Long interestId, String interestName, String category) {
        return new MemberInterestResponse(interestId, interestName, category);
    }
}
