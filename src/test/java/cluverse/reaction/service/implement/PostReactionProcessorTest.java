package cluverse.reaction.service.implement;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PostReactionProcessorTest {

    @Mock
    private PostReactionWriter postReactionWriter;

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private PostMetaWriter postMetaWriter;

    @InjectMocks
    private PostReactionProcessor postReactionProcessor;

    @Test
    void 게시글_좋아요를_추가하고_좋아요_수를_증가시킨다() {
        // when
        postReactionProcessor.likePost(1L, 10L);

        // then
        InOrder inOrder = inOrder(postAccessReader, postReactionWriter, postMetaWriter);
        inOrder.verify(postAccessReader).validateReadablePost(1L, 10L);
        inOrder.verify(postReactionWriter).likePost(1L, 10L);
        inOrder.verify(postMetaWriter).increaseLikeCount(10L);
        verifyNoMoreInteractions(postAccessReader, postReactionWriter, postMetaWriter);
    }

    @Test
    void 게시글을_북마크하고_북마크_수를_증가시킨다() {
        // when
        postReactionProcessor.bookmarkPost(1L, 10L);

        // then
        InOrder inOrder = inOrder(postAccessReader, postReactionWriter, postMetaWriter);
        inOrder.verify(postAccessReader).validateReadablePost(1L, 10L);
        inOrder.verify(postReactionWriter).bookmarkPost(1L, 10L);
        inOrder.verify(postMetaWriter).increaseBookmarkCount(10L);
        verifyNoMoreInteractions(postAccessReader, postReactionWriter, postMetaWriter);
    }

    @Test
    void 게시글_북마크를_취소하고_북마크_수를_감소시킨다() {
        // when
        postReactionProcessor.removeBookmark(1L, 10L);

        // then
        InOrder inOrder = inOrder(postAccessReader, postReactionWriter, postMetaWriter);
        inOrder.verify(postAccessReader).validateReadablePost(1L, 10L);
        inOrder.verify(postReactionWriter).removeBookmark(1L, 10L);
        inOrder.verify(postMetaWriter).decreaseBookmarkCount(10L);
        verifyNoMoreInteractions(postAccessReader, postReactionWriter, postMetaWriter);
    }
}
