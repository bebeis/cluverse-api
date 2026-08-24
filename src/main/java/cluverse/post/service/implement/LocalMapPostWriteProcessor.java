package cluverse.post.service.implement;

import cluverse.board.service.implement.BoardReader;
import cluverse.member.service.implement.MemberReader;
import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.place.domain.SelectedPlace;
import cluverse.post.domain.Post;
import cluverse.post.service.request.PostCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalMapPostWriteProcessor {

    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final PostWriter postWriter;
    private final PostMetaWriter postMetaWriter;
    private final PostPlaceVerificationWriter verificationWriter;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long create(Long memberId, String requestId, PostCreateRequest request,
                       List<SelectedPlace> selectedPlaces, String clientIp) {
        boardReader.validateWritable(memberId, memberReader.isVerified(memberId), request.boardId());
        Post post = postWriter.create(memberId, request, clientIp, requestId);
        postMetaWriter.createViewCount(post.getId());
        if (!selectedPlaces.isEmpty()) {
            verificationWriter.start(post.getId());
            eventPublisher.publishEvent(
                    new PostPlaceVerificationRequested(memberId, post.getId(), selectedPlaces));
        }
        return post.getId();
    }
}
