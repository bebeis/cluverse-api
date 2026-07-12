package cluverse.reaction.service.implement;

import cluverse.feed.repository.FeedQueryRepository;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.reaction.service.request.BookmarkedPostSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReactionReader {

    private final FeedQueryRepository feedQueryRepository;

    public FeedPageQueryResult readBookmarkedFeed(Long memberId, BookmarkedPostSortType sortType, int page, int size) {
        return feedQueryRepository.findBookmarkedFeedPage(
                memberId,
                feedQueryRepository.findBlockedMemberIds(memberId),
                feedQueryRepository.findMyGroupBoardIds(memberId),
                sortType,
                page,
                size
        );
    }
}
