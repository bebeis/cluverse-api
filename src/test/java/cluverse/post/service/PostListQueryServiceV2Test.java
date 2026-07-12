package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostOffsetSearchRequest;
import cluverse.post.service.request.PostSortType;
import cluverse.post.service.response.PostPageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostListQueryServiceV2Test {

    @Mock
    private PostReader postReader;

    @Mock
    private BoardReader boardReader;

    @InjectMocks
    private PostListQueryServiceV2 postListQueryServiceV2;

    @Test
    void 다음_페이지_여부는_슬라이스에서_마지막_페이지는_전체_카운트에서_계산한다() {
        // given: 총 250건, 1페이지 조회 — 슬라이스 hasNext=true
        PostOffsetSearchRequest request = new PostOffsetSearchRequest(3L, null, PostSortType.LATEST, 1, 20);
        when(postReader.readPostPage(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), true));
        when(postReader.countPosts(request)).thenReturn(250L);

        // when
        PostPageResponse response = postListQueryServiceV2.getPosts(99L, request);

        // then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.lastPage()).isEqualTo(10);
        assertThat(response.hasNextBlock()).isTrue();
        verify(boardReader).validateReadable(99L, 3L);
    }

    @Test
    void 블록_안에서_끝나면_실제_마지막_페이지를_내려준다() {
        // given: 총 45건, 3페이지(마지막) 조회 — 슬라이스 hasNext=false
        PostOffsetSearchRequest request = new PostOffsetSearchRequest(3L, null, PostSortType.LATEST, 3, 20);
        when(postReader.readPostPage(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));
        when(postReader.countPosts(request)).thenReturn(45L);

        // when
        PostPageResponse response = postListQueryServiceV2.getPosts(99L, request);

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.lastPage()).isEqualTo(3);
        assertThat(response.hasNextBlock()).isFalse();
    }
}
