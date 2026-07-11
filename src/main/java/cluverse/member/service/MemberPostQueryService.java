package cluverse.member.service;

import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.request.MemberPostPageRequest;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPostQueryService {

    private final MemberReader memberReader;
    private final PostReader postReader;

    public PostPageResponse getMyPosts(Long memberId, MemberPostPageRequest request) {
        memberReader.readOrThrow(memberId);

        PostPageQueryResult queryResult = postReader.readPostPageByAuthor(memberId, memberId, request.pageOrDefault(),
                request.sizeOrDefault());
        List<PostSummaryResponse> responses = queryResult.posts().stream()
                .map(PostSummaryResponse::from)
                .toList();

        return new PostPageResponse(
                responses,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                queryResult.hasNext(),
                false
        );
    }
}
