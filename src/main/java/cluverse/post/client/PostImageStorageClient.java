package cluverse.post.client;

public interface PostImageStorageClient {

    PresignedUploadResult createPresignedUpload(String fileKey, String contentType);
}
