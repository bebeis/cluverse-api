package cluverse.post.service.response;

import java.util.List;

public record PostPageResponse(
        List<PostSummaryResponse> posts,
        Integer page,
        int size,
        boolean hasNext,
        boolean dateBased
) {
    public PostPageResponse {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
