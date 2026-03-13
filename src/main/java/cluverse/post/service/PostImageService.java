package cluverse.post.service;

import cluverse.post.service.request.PostImagePresignedUrlRequest;
import cluverse.post.service.response.PostImagePresignedUrlResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostImageService {

    @Transactional(readOnly = true)
    public PostImagePresignedUrlResponse createPresignedUrl(
            Long memberId,
            PostImagePresignedUrlRequest request
    ) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("게시글 이미지 서비스는 아직 구현되지 않았습니다.");
    }
}
