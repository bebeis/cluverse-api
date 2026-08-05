package cluverse.meta.service;

import cluverse.meta.service.implement.PostMetaWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostMetaServiceTest {

    @Mock
    private PostMetaWriter postMetaWriter;

    @InjectMocks
    private PostMetaService postMetaService;

    @Test
    void 게시글_조회수_레코드_생성을_작성기에_위임한다() {
        postMetaService.createViewCount(10L);

        verify(postMetaWriter).createViewCount(10L);
    }

    @Test
    void 게시글_조회수_증가를_작성기에_위임한다() {
        postMetaService.increaseViewCount(10L);

        verify(postMetaWriter).increaseViewCount(10L);
    }

    @Test
    void 게시글_좋아요_수_증가를_작성기에_위임한다() {
        postMetaService.increaseLikeCount(10L);

        verify(postMetaWriter).increaseLikeCount(10L);
    }

    @Test
    void 게시글_댓글_수_증가를_작성기에_위임한다() {
        postMetaService.increaseCommentCount(10L);

        verify(postMetaWriter).increaseCommentCount(10L);
    }

    @Test
    void 게시글_북마크_수_감소를_작성기에_위임한다() {
        postMetaService.decreaseBookmarkCount(10L);

        verify(postMetaWriter).decreaseBookmarkCount(10L);
    }
}
