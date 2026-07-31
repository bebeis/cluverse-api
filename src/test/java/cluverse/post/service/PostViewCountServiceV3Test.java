package cluverse.post.service;

import cluverse.meta.service.implement.PostMetaWriter;
import cluverse.post.service.implement.PostAccessReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostViewCountServiceV3Test {

    @Mock
    private PostAccessReader postAccessReader;

    @Mock
    private PostMetaWriter postMetaWriter;

    @InjectMocks
    private PostViewCountServiceV3 postViewCountService;

    @Test
    void V3_조회수_증가시_원자적_UPDATE_방식에_위임한다() {
        // when
        postViewCountService.increaseViewCount(10L);

        // then
        verify(postAccessReader).validateActivePost(10L);
        verify(postMetaWriter).increaseViewCount(10L);
    }
}
