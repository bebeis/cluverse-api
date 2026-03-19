package cluverse.meta.service.implement;

import cluverse.meta.domain.PostBookmarkCount;
import cluverse.meta.domain.PostCommentCount;
import cluverse.meta.domain.PostViewCount;
import cluverse.meta.repository.PostBookmarkCountRepository;
import cluverse.meta.repository.PostCommentCountRepository;
import cluverse.meta.repository.PostLikeCountRepository;
import cluverse.meta.repository.PostViewCountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostMetaWriterTest {

    @Mock
    private PostLikeCountRepository postLikeCountRepository;

    @Mock
    private PostBookmarkCountRepository postBookmarkCountRepository;

    @Mock
    private PostCommentCountRepository postCommentCountRepository;

    @Mock
    private PostViewCountRepository postViewCountRepository;

    @Mock
    private PostViewCountV2Writer postViewCountV2Writer;

    @InjectMocks
    private PostMetaWriter postMetaWriter;

    @Test
    void 게시글_생성시_조회수_레코드를_생성한다() {
        postMetaWriter.createViewCount(10L);

        verify(postViewCountRepository).save(argThat(postViewCount ->
                postViewCount.getPostId().equals(10L) && postViewCount.getViewCount() == 0
        ));
    }

    @Test
    void 게시글_조회수는_분리된_테이블에서_증가시킨다() {
        postMetaWriter.increaseViewCount(10L);

        verify(postViewCountRepository).increaseCount(10L);
    }

    @Test
    void 게시글_조회수_V2는_낙관적_락_작성기에_위임한다() {
        postMetaWriter.increaseViewCountV2(10L);

        verify(postViewCountV2Writer).increaseCount(10L);
    }

    @Test
    void 게시글_좋아요_수는_upsert로_증가시킨다() {
        postMetaWriter.increaseLikeCount(10L);

        verify(postLikeCountRepository).increaseCount(10L);
    }

    @Test
    void 게시글_북마크_수가_0이되면_row를_삭제한다() {
        when(postBookmarkCountRepository.findByPostIdForUpdate(10L))
                .thenReturn(Optional.of(PostBookmarkCount.of(10L, 1)));
        when(postBookmarkCountRepository.decreaseCount(10L)).thenReturn(1);

        postMetaWriter.decreaseBookmarkCount(10L);

        verify(postBookmarkCountRepository).deleteIfZero(10L);
    }

    @Test
    void 게시글_댓글_수가_0이되면_row를_삭제한다() {
        when(postCommentCountRepository.findByPostIdForUpdate(10L))
                .thenReturn(Optional.of(PostCommentCount.of(10L, 1)));
        when(postCommentCountRepository.decreaseCount(10L)).thenReturn(1);

        postMetaWriter.decreaseCommentCount(10L);

        verify(postCommentCountRepository).deleteIfZero(10L);
    }
}
