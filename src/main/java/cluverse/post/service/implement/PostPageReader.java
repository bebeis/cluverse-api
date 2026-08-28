package cluverse.post.service.implement;

import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.annotation.LatestPostIdsCacheable;
import cluverse.post.service.request.PostPageSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostPageReader {

    private final PostReader postReader;

    @LatestPostIdsCacheable
    public PostPageQueryResult readPage(
            Long memberId,
            PostPageSearchRequest request,
            long countLimit
    ) {
        return postReader.readOffsetPage(memberId, request);
    }
}
