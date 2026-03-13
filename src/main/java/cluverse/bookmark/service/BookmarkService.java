package cluverse.bookmark.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookmarkService {

    public void bookmarkPost(Long memberId, Long postId) {
        throw unsupported();
    }

    public void removeBookmark(Long memberId, Long postId) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("북마크 서비스는 아직 구현되지 않았습니다.");
    }
}
