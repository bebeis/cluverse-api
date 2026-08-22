package cluverse.post.client;

import java.nio.file.Path;
import java.util.Collection;

public interface PostImageObjectStorageClient {

    void upload(String objectKey, String contentType, Path source);

    void copy(String sourceKey, String targetKey, String contentType);

    void delete(Collection<String> objectKeys);

    long size(String objectKey);
}
