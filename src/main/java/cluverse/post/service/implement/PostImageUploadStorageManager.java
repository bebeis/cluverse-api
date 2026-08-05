package cluverse.post.service.implement;

import cluverse.post.client.PostImageObjectStorageClient;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostImageUploadStorageManager {

    private final PostImageObjectStorageClient storageClient;

    public boolean deleteStaging(PostImageUpload upload) {
        try {
            storageClient.delete(upload.getAssets().stream().map(PostImageAsset::getStagingKey).toList());
            return true;
        } catch (RuntimeException exception) {
            log.warn("이미지 staging 정리에 실패했습니다. uploadId={}", upload.getId(), exception);
            return false;
        }
    }

    public boolean compensate(PostImageUpload upload) {
        try {
            storageClient.delete(allKeys(upload));
            return true;
        } catch (RuntimeException exception) {
            log.warn("이미지 업로드 보상에 실패했습니다. uploadId={}", upload.getId(), exception);
            return false;
        }
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
