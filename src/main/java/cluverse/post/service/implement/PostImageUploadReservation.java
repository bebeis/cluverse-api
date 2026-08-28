package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostImageUploadReservation {

    private final PostImageUploadWriter writer;

    public PostImageUploadReservationResult reserve(
            Long memberId,
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        var existing = writer.read(requestId, version);
        if (existing.isPresent()) {
            existing.get().validateOwner(memberId);
            return new PostImageUploadReservationResult.Existing(existing.get());
        }
        try {
            return new PostImageUploadReservationResult.Created(
                    writer.reserve(memberId, requestId, version, assets));
        } catch (DataIntegrityViolationException exception) {
            // 선행 조회를 함께 통과한 요청은 DB unique constraint로 한 건만 생성한다.
            PostImageUpload raced = writer.read(requestId, version).orElseThrow(() -> exception);
            raced.validateOwner(memberId);
            return new PostImageUploadReservationResult.Existing(raced);
        }
    }

    public PostImageUploadReservationResult reserve(
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        return reserve(null, requestId, version, assets);
    }
}
