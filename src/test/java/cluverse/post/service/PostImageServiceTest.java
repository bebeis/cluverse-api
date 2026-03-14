package cluverse.post.service;

import cluverse.post.client.PostImageStorageClient;
import cluverse.post.client.PresignedUploadResult;
import cluverse.post.service.request.PostImagePresignedUrlRequest;
import cluverse.post.service.response.PostImagePresignedUrlResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostImageServiceTest {

    @Mock
    private PostImageStorageClient postImageStorageClient;

    @Test
    void 게시글_이미지_presigned_url을_생성한다() throws Exception {
        PostImageService postImageService = new PostImageService(postImageStorageClient);
        when(postImageStorageClient.createPresignedUpload(any(), any())).thenReturn(
                new PresignedUploadResult(
                        "https://upload.example.com/presigned",
                        "http://localhost:4566/cluverse-images/posts/1/2026/03/13/uuid.png"
                )
        );

        PostImagePresignedUrlResponse response = postImageService.createPresignedUrl(
                1L,
                new PostImagePresignedUrlRequest("banner.png", "image/png")
        );

        assertThat(response.fileKey()).startsWith("posts/1/");
        assertThat(response.fileKey()).endsWith(".png");
        assertThat(response.uploadUrl()).isEqualTo("https://upload.example.com/presigned");
        assertThat(response.imageUrl()).isEqualTo("http://localhost:4566/cluverse-images/posts/1/2026/03/13/uuid.png");
        assertThat(response.expiresAt()).isNotNull();
    }
}
