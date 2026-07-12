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
class PostListQueryServiceV1Test {

    @Mock
    private PostReader postReader;

    @Mock
    private BoardReader boardReader;

    @InjectMocks
    private PostListQueryServiceV1 postListQueryServiceV1;

    @Test
    void 전체_카운트로_마지막_페이지와_다음_페이지_여부를_계산한다() {
        // given: 총 45건, 1페이지(20건씩) 조회
        PostOffsetSearchRequest request = new PostOffsetSearchRequest(3L, null, PostSortType.LATEST, 1, 20);
        when(postReader.readPostPageWithOffset(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));
        when(postReader.countPosts(request)).thenReturn(45L);

        // when
        PostPageResponse response = postListQueryServiceV1.getPosts(99L, request);

        // then: 마지막 페이지 3, 다음 페이지 있음, 다음 블록 없음
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.lastPage()).isEqualTo(3);
        assertThat(response.hasNextBlock()).isFalse();
        verify(boardReader).validateReadable(99L, 3L);
    }

    @Test
    void 다음_블록이_있으면_블록_끝_페이지를_마지막_페이지로_내려준다() {
        // given: 총 250건(13페이지), 1페이지 조회 → 블록(1~10) 너머 존재
        PostOffsetSearchRequest request = new PostOffsetSearchRequest(3L, null, PostSortType.LATEST, 1, 20);
        when(postReader.readPostPageWithOffset(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));
        when(postReader.countPosts(request)).thenReturn(250L);

        // when
        PostPageResponse response = postListQueryServiceV1.getPosts(99L, request);

        // then
        assertThat(response.lastPage()).isEqualTo(10);
        assertThat(response.hasNextBlock()).isTrue();
    }

    @Test
    void 마지막_페이지_조회는_다음_페이지가_없다() {
        // given: 총 45건, 3페이지(마지막) 조회
        PostOffsetSearchRequest request = new PostOffsetSearchRequest(3L, null, PostSortType.LATEST, 3, 20);
        when(postReader.readPostPageWithOffset(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));
        when(postReader.countPosts(request)).thenReturn(45L);

        // when
        PostPageResponse response = postListQueryServiceV1.getPosts(99L, request);

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.lastPage()).isEqualTo(3);
    }

    @Test
    void 게시글이_없어도_마지막_페이지는_1이다() {
        // given
        PostOffsetSearchRequest request = new PostOffsetSearchRequest(3L, null, PostSortType.LATEST, 1, 20);
        when(postReader.readPostPageWithOffset(99L, request))
                .thenReturn(new PostPageQueryResult(List.of(), false));
        when(postReader.countPosts(request)).thenReturn(0L);

        // when
        PostPageResponse response = postListQueryServiceV1.getPosts(99L, request);

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.lastPage()).isEqualTo(1);
        assertThat(response.hasNextBlock()).isFalse();
    }
}
