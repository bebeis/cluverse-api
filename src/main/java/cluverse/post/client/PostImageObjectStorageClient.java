package cluverse.post.client;

import java.nio.file.Path;
import java.util.Collection;

public interface PostImageObjectStorageClient {

    void upload(String objectKey, String contentType, Path source);

    void delete(Collection<String> objectKeys);

    long size(String objectKey);
}
