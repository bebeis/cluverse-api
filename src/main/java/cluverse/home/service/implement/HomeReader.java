package cluverse.home.service.implement;

import cluverse.home.repository.HomeQueryRepository;
import cluverse.home.repository.dto.HomeBoardQueryResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
public class HomeReader {

    private final HomeQueryRepository homeQueryRepository;

    public HomeReader(HomeQueryRepository homeQueryRepository) {
        this.homeQueryRepository = homeQueryRepository;
    }

    public FavoriteBoardPageView readFavoriteBoards(Long memberId, Long cursorBoardId, int size) {
        List<HomeBoardQueryResult> rows = homeQueryRepository.findFavoriteBoards(
                memberId, cursorBoardId, size + 1
        );
        boolean hasNext = rows.size() > size;
        List<FavoriteBoardView> boards = rows.stream()
                .limit(size)
                .map(FavoriteBoardView::from)
                .toList();
        Long nextCursor = hasNext ? boards.getLast().boardId() : null;
        return new FavoriteBoardPageView(boards, nextCursor, hasNext);
    }

    public List<RecentCommentedPostView> readRecentCommentedPosts(Long memberId, int size) {
        return homeQueryRepository.findRecentCommentedPosts(memberId, size).stream()
                .map(RecentCommentedPostView::from)
                .toList();
    }
}
