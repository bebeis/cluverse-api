package cluverse.post.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record PostPageResponse(
        List<PostSummaryResponse> posts,
        Integer page,
        int size,
        boolean hasNext,
        Integer lastPage,
        Boolean hasNextBlock,
        boolean dateBased,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean hasPrev,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PostCursorResponse prevCursor,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PostCursorResponse nextCursor,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean cursorRequired
) {
    public PostPageResponse {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }

    public PostPageResponse(List<PostSummaryResponse> posts, Integer page, int size, boolean hasNext,
                            boolean dateBased) {
        this(posts, page, size, hasNext, null, null, dateBased, null, null, null, null);
    }

    public PostPageResponse(
            List<PostSummaryResponse> posts,
            Integer page,
            int size,
            boolean hasNext,
            Integer lastPage,
            Boolean hasNextBlock,
            boolean dateBased
    ) {
        this(posts, page, size, hasNext, lastPage, hasNextBlock, dateBased, null, null, null, null);
    }
}
