package cluverse.member.service;

import cluverse.member.service.request.MemberProfileImagePresignedUrlRequest;
import cluverse.member.service.response.MemberProfileImagePresignedUrlResponse;
import cluverse.post.client.PostImageStorageClient;
import cluverse.post.client.PresignedUploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberProfileImageServiceTest {

    @Mock
    private PostImageStorageClient postImageStorageClient;

    @InjectMocks
    private MemberProfileImageService memberProfileImageService;

    @Test
    void 프로필_이미지_presigned_url을_발급한다() {
        // given
        MemberProfileImagePresignedUrlRequest request = new MemberProfileImagePresignedUrlRequest("profile.png", "image/png");
        when(postImageStorageClient.createPresignedUpload(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("image/png")))
                .thenReturn(new PresignedUploadResult(
                        "https://upload.example.com/profile.png",
                        "https://cdn.example.com/profile.png"
                ));

        // when
        MemberProfileImagePresignedUrlResponse response = memberProfileImageService.createPresignedUrl(1L, request);

        // then
        assertThat(response.fileKey()).startsWith("members/1/profile/");
        assertThat(response.uploadUrl()).isEqualTo("https://upload.example.com/profile.png");
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/profile.png");
    }
}
