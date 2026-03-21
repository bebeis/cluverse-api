package cluverse.member.service;

import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.request.MemberPostPageRequest;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostQueryRepository;
import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.repository.dto.PostSummaryQueryDto;
import cluverse.post.service.response.PostPageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberPostServiceTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private PostQueryRepository postQueryRepository;

    @InjectMocks
    private MemberPostQueryService memberPostQueryService;

    @Test
    void 내_게시글_목록을_조회한다() {
        // given
        Member member = Member.create("luna", 10L);
        ReflectionTestUtils.setField(member, "id", 1L);
        MemberPostPageRequest request = new MemberPostPageRequest(1, 20);

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(postQueryRepository.findPostPageByAuthor(1L, 1L, 1, 20)).thenReturn(new PostPageQueryResult(
                List.of(new PostSummaryQueryDto(
                        10L,
                        3L,
                        PostCategory.INFORMATION,
                        "스프링 스터디 모집합니다",
                        "주 1회 온라인으로 진행할 예정입니다.",
                        List.of("spring", "backend"),
                        "https://cdn.example.com/posts/10-thumb.png",
                        false,
                        false,
                        true,
                        true,
                        120L,
                        15L,
                        4L,
                        8L,
                        1L,
                        "luna",
                        "https://cdn.example.com/profile.png",
                        LocalDateTime.of(2026, 3, 13, 10, 0)
                )),
                true
        ));

        // when
        PostPageResponse response = memberPostQueryService.getMyPosts(1L, request);

        // then
        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().getFirst().postId()).isEqualTo(10L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.hasNext()).isTrue();
        verify(memberReader).readOrThrow(1L);
    }
}
