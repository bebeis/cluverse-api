package cluverse.group.service.response;

import cluverse.group.domain.GroupInterest;

public record GroupInterestResponse(
        Long interestId,
        String name
) {
    public static GroupInterestResponse of(GroupInterest interest, String name) {
        return new GroupInterestResponse(interest.getInterestId(), name);
    }
}
