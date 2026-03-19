package cluverse.meta.service;

import cluverse.meta.service.implement.PostMetaWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostMetaService {

    private final PostMetaWriter postMetaWriter;

    public void createViewCount(Long postId) {
        postMetaWriter.createViewCount(postId);
    }

    public void increaseViewCount(Long postId) {
        postMetaWriter.increaseViewCount(postId);
    }

    public void increaseViewCountV2(Long postId) {
        postMetaWriter.increaseViewCountV2(postId);
    }

    public void increaseLikeCount(Long postId) {
        postMetaWriter.increaseLikeCount(postId);
    }

    public void increaseBookmarkCount(Long postId) {
        postMetaWriter.increaseBookmarkCount(postId);
    }

    public void decreaseBookmarkCount(Long postId) {
        postMetaWriter.decreaseBookmarkCount(postId);
    }

    public void increaseCommentCount(Long postId) {
        postMetaWriter.increaseCommentCount(postId);
    }

    public void decreaseCommentCount(Long postId) {
        postMetaWriter.decreaseCommentCount(postId);
    }
}
