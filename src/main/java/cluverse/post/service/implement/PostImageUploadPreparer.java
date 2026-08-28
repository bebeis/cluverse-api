package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageProcessingPlan;
import cluverse.post.service.request.PostImageUploadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostImageUploadPreparer {

    private static final String POLICY_VERSION = "p1";

    private final PostImageFileInspector fileInspector;
    private final PostImageTemporaryFileStore temporaryFiles;

    public PreparedPostImageUpload prepare(ImageUploadVersion version, PostImageUploadRequest request) {
        List<PreparedPostImage> preparedImages = new ArrayList<>();
        List<PostImageAsset> assets = new ArrayList<>();
        try {
            for (int index = 0; index < request.images().size(); index++) {
                MultipartFile file = request.images().get(index);
                PostImageFileInspector.PostImageSource source = fileInspector.inspect(file);
                String prefix = "image-uploads/%s/%s/%d".formatted(
                        version.value(), request.requestId(), index);
                String stagingKey = prefix + "/staging/source";
                String contentKey = prefix + "/" + POLICY_VERSION + "/content.jpg";
                String thumbnailKey = prefix + "/" + POLICY_VERSION + "/thumbnail.jpg";
                PostImageProcessingPlan plan = new PostImageProcessingPlan(
                        request.requestId(), index, stagingKey, contentKey, thumbnailKey, POLICY_VERSION);
                TemporaryPostImageFile temporaryFile = temporaryFiles.copy(file);
                preparedImages.add(new PreparedPostImage(
                        temporaryFile, source.contentType(), source.bytes(), plan));
                assets.add(PostImageAsset.plan(
                        index, stagingKey, contentKey, thumbnailKey, source.bytes()));
            }
            return new PreparedPostImageUpload(
                    List.copyOf(preparedImages),
                    List.copyOf(assets)
            );
        } catch (RuntimeException exception) {
            deletePreparedFiles(preparedImages);
            throw exception;
        }
    }

    private void deletePreparedFiles(List<PreparedPostImage> preparedImages) {
        for (PreparedPostImage preparedImage : preparedImages) {
            preparedImage.source().close();
        }
    }
}
