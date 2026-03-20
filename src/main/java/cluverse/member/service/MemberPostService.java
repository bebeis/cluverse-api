package cluverse.member.service;

import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.request.MemberPostPageRequest;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.response.PostPageResponse;
import cluverse.post.service.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPostService {

    private final MemberReader memberReader;
    private final PostQueryRepository postQueryRepository;

    public PostPageResponse getMyPosts(Long memberId, MemberPostPageRequest request) {
        memberReader.readOrThrow(memberId);

        PostPageQueryResult queryResult = postQueryRepository.findPostPageByAuthor(memberId, memberId, request.pageOrDefault(),
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
