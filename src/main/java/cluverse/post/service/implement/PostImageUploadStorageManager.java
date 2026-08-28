package cluverse.post.service.implement;

import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostImageUploadStorageManager {

    private final PostImageObjectStorageClient storageClient;

    public void deleteStaging(PostImageUpload upload) {
        storageClient.delete(upload.getAssets().stream().map(PostImageAsset::getStagingKey).toList());
    }

    public void deleteAll(PostImageUpload upload) {
        storageClient.delete(allKeys(upload));
    }

    public String createImageUrl(String objectKey) {
        return storageClient.createImageUrl(objectKey);
    }

    private List<String> allKeys(PostImageUpload upload) {
        List<String> keys = new ArrayList<>();
        for (PostImageAsset asset : upload.getAssets()) {
            keys.add(asset.getStagingKey());
            keys.add(asset.getContentKey());
            keys.add(asset.getThumbnailKey());
        }
        return keys;
    }
}
