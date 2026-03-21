package cluverse.meta.service.implement;

import cluverse.meta.domain.PostBookmarkCount;
import cluverse.meta.domain.PostCommentCount;
import cluverse.meta.domain.PostViewCount;
import cluverse.meta.domain.PostViewCountV2;
import cluverse.meta.repository.PostBookmarkCountRepository;
import cluverse.meta.repository.PostCommentCountRepository;
import cluverse.meta.repository.PostLikeCountRepository;
import cluverse.meta.repository.PostViewCountRepository;
import cluverse.meta.repository.PostViewCountV2Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
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
    private PostViewCountV2Repository postViewCountV2Repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

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
    void 게시글_조회수_V2는_새_트랜잭션에서_증가시킨다() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(postViewCountV2Repository.findById(10L)).thenReturn(Optional.of(PostViewCountV2.create(10L)));

        postMetaWriter.increaseViewCountV2(10L);

        verify(postViewCountV2Repository).findById(10L);
        verify(postViewCountV2Repository).flush();
        verify(transactionManager).commit(transactionStatus);
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
