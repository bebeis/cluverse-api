package cluverse.post.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.repository.PostImageUploadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostImageUploadClaimerTest {

    @Mock
    private PostImageUploadRepository repository;

    @Test
    void 완료된_내_업로드를_게시글에_key로_연결한다() {
        UUID requestId = UUID.randomUUID();
        PostImageUpload upload = completedUpload(1L, requestId);
        Post post = post();
        when(repository.findByRequestIdAndVersionForUpdate(requestId, ImageUploadVersion.V3))
                .thenReturn(Optional.of(upload));

        new PostImageUploadClaimer(repository).claimForCreate(
                1L, post, List.of(), List.of(requestId));

        assertThat(post.getImages()).singleElement().satisfies(image -> {
            assertThat(image.getImageUrl()).isNull();
            assertThat(image.getContentKey()).isEqualTo("content/a.jpg");
            assertThat(image.getThumbnailKey()).isEqualTo("thumbnail/a.jpg");
        });
        assertThat(upload.getClaimedPostId()).isEqualTo(10L);
    }

    @Test
    void 다른_회원의_업로드는_연결할_수_없다() {
        UUID requestId = UUID.randomUUID();
        when(repository.findByRequestIdAndVersionForUpdate(requestId, ImageUploadVersion.V3))
                .thenReturn(Optional.of(completedUpload(2L, requestId)));

        assertThatThrownBy(() -> new PostImageUploadClaimer(repository).claimForCreate(
                1L, post(), List.of(), List.of(requestId)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 수정에서_제거한_업로드는_claim을_해제해_정리_대상이_된다() {
        UUID requestId = UUID.randomUUID();
        PostImageUpload upload = completedUpload(1L, requestId);
        Post post = post();
        when(repository.findByRequestIdAndVersionForUpdate(requestId, ImageUploadVersion.V3))
                .thenReturn(Optional.of(upload));
        when(repository.findByClaimedPostId(10L)).thenReturn(List.of(upload));
        PostImageUploadClaimer claimer = new PostImageUploadClaimer(repository);
        claimer.claimForCreate(1L, post, List.of(), List.of(requestId));

        claimer.claimForUpdate(1L, post, List.of(), List.of(), List.of());

        assertThat(upload.getClaimedPostId()).isNull();
        assertThat(post.getImages()).isEmpty();
    }

    private PostImageUpload completedUpload(Long memberId, UUID requestId) {
        return PostImageUpload.completed(
                memberId,
                requestId,
                ImageUploadVersion.V3,
                List.of(PostImageAsset.plan(
                        0, null, "content/a.jpg", "thumbnail/a.jpg", 100))
        );
    }

    private Post post() {
        Post post = Post.createByMember(
                List.of(), List.of(), 1L, 1L, "제목", "본문",
                PostCategory.GENERAL, false, false, true, "127.0.0.1");
        ReflectionTestUtils.setField(post, "id", 10L);
        return post;
    }
}
