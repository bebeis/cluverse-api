package cluverse.post.service.implement;

import cluverse.board.service.implement.BoardReader;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.domain.Post;
import cluverse.post.service.request.PostCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PostCreationProcessor {

    private final PostWriter postWriter;
    private final PostMetaWriter postMetaWriter;
    private final BoardReader boardReader;
    private final MemberReader memberReader;

    public Long create(Long memberId, PostCreateRequest request, String clientIp) {
        boardReader.validateWritable(memberId, memberReader.isVerified(memberId), request.boardId());
        Post post = postWriter.create(memberId, request, clientIp);
        postMetaWriter.createViewCount(post.getId());
        return post.getId();
    }
}
