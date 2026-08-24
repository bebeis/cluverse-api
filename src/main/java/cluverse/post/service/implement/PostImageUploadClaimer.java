package cluverse.post.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostImage;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.repository.PostImageUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostImageUploadClaimer {

    private static final int MAX_IMAGES = 10;
    private static final ImageUploadVersion CURRENT_VERSION = ImageUploadVersion.V3;

    private final PostImageUploadRepository repository;

    public void claimForCreate(
            Long memberId,
            Post post,
            List<String> legacyImageUrls,
            List<UUID> uploadRequestIds
    ) {
        List<Post.PostImageKey> imageKeys = claimUploads(memberId, post.getId(), uploadRequestIds);
        validateImageCount(legacyImageUrls.size() + imageKeys.size());
        post.replaceImages(legacyImageUrls, imageKeys);
    }

    public void claimForUpdate(
            Long memberId,
            Post post,
            List<String> legacyImageUrls,
            List<String> retainedContentKeys,
            List<UUID> uploadRequestIds
    ) {
        Map<String, PostImage> currentByContentKey = post.getImages().stream()
                .filter(image -> image.getContentKey() != null)
                .collect(Collectors.toMap(PostImage::getContentKey, Function.identity()));
        List<Post.PostImageKey> imageKeys = new ArrayList<>();
        for (String contentKey : new LinkedHashSet<>(retainedContentKeys)) {
            PostImage retained = currentByContentKey.get(contentKey);
            if (retained == null) {
                throw new BadRequestException("게시글에 연결되지 않은 이미지는 유지할 수 없습니다.");
            }
            imageKeys.add(new Post.PostImageKey(retained.getContentKey(), retained.getThumbnailKey()));
        }
        releaseUnused(post, Set.copyOf(retainedContentKeys));
        imageKeys.addAll(claimUploads(memberId, post.getId(), uploadRequestIds));
        validateImageCount(legacyImageUrls.size() + imageKeys.size());
        post.replaceImages(legacyImageUrls, imageKeys);
    }

    public void releaseAll(Post post) {
        repository.findByClaimedPostId(post.getId())
                .forEach(upload -> upload.release(post.getId()));
        post.replaceImages(List.of(), List.of());
    }

    private void releaseUnused(Post post, Set<String> retainedContentKeys) {
        repository.findByClaimedPostId(post.getId()).stream()
                .filter(upload -> upload.getAssets().stream()
                        .map(asset -> asset.getContentKey())
                        .noneMatch(retainedContentKeys::contains))
                .forEach(upload -> upload.release(post.getId()));
    }

    private List<Post.PostImageKey> claimUploads(
            Long memberId,
            Long postId,
            List<UUID> uploadRequestIds
    ) {
        if (new LinkedHashSet<>(uploadRequestIds).size() != uploadRequestIds.size()) {
            throw new BadRequestException("같은 이미지 업로드를 중복 연결할 수 없습니다.");
        }
        List<Post.PostImageKey> imageKeys = new ArrayList<>();
        for (UUID requestId : uploadRequestIds) {
            PostImageUpload upload = repository.findByRequestIdAndVersionForUpdate(requestId, CURRENT_VERSION)
                    .orElseThrow(() -> new BadRequestException("완료된 이미지 업로드를 찾을 수 없습니다."));
            try {
                upload.claim(memberId, postId);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new BadRequestException(exception.getMessage());
            }
            upload.getAssets().forEach(asset -> imageKeys.add(
                    new Post.PostImageKey(asset.getContentKey(), asset.getThumbnailKey())));
        }
        return imageKeys;
    }

    private void validateImageCount(int imageCount) {
        if (imageCount > MAX_IMAGES) {
            throw new BadRequestException("이미지는 최대 10개까지 첨부할 수 있습니다.");
        }
    }
}
