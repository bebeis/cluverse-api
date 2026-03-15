package cluverse.group.service.response;

import java.util.List;

public record GroupPageResponse(
        List<GroupSummaryResponse> groups,
        int page,
        int size,
        boolean hasNext
) {
    public GroupPageResponse {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }
}
