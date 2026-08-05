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
            UUID requestId,
            ImageUploadVersion version,
            List<PostImageAsset> assets
    ) {
        var existing = writer.read(requestId, version);
        if (existing.isPresent()) {
            return new PostImageUploadReservationResult(existing.get(), false);
        }
        try {
            return new PostImageUploadReservationResult(writer.reserve(requestId, version, assets), true);
        } catch (DataIntegrityViolationException exception) {
            PostImageUpload raced = writer.read(requestId, version).orElseThrow(() -> exception);
            return new PostImageUploadReservationResult(raced, false);
        }
    }
}
