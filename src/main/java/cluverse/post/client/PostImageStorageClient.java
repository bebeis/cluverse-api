package cluverse.post.client;

import java.nio.file.Path;
import java.util.Collection;

public interface PostImageStorageClient {

    PresignedUploadResult createPresignedUpload(String fileKey, String contentType);

    void upload(String fileKey, String contentType, Path source);

    void delete(Collection<String> fileKeys);

    String createImageUrl(String fileKey);
}
