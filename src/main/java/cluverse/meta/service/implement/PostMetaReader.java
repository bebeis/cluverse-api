package cluverse.meta.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.meta.exception.MetaExceptionMessage;
import cluverse.meta.repository.PostViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostMetaReader {

    private final PostViewCountRepository postViewCountRepository;

    @Transactional(readOnly = true)
    public long readViewCount(Long postId) {
        return postViewCountRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException(MetaExceptionMessage.POST_VIEW_COUNT_NOT_FOUND.getMessage()))
                .getViewCount();
    }
}
