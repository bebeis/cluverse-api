package cluverse.post.service.response;

import java.util.List;

public record PostPageResponse(
        List<PostSummaryResponse> posts,
        int page,
        int size,
        boolean hasNext
) {
    public PostPageResponse {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
