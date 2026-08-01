package cluverse.popularity.service.implement;

import cluverse.popularity.properties.PopularityProperties;
import cluverse.popularity.repository.PopularityQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularityPolicyRefreshProcessor {

    private final PopularityQueryRepository popularityQueryRepository;
    private final PopularityPolicyBoardRefreshProcessor popularityPolicyBoardRefreshProcessor;
    private final PopularityProperties properties;
    private final Clock clock;

    public int refresh() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        LocalDateTime sampleEnd = now.minus(properties.promotionWindow());
        LocalDateTime sampleStart = sampleEnd.minus(properties.policySampleWindow());
        List<Long> boardIds = popularityQueryRepository.findPolicyBoardIds(sampleStart, sampleEnd);
        int refreshed = 0;
        for (Long boardId : boardIds) {
            try {
                popularityPolicyBoardRefreshProcessor.refreshBoard(
                        boardId,
                        sampleStart,
                        sampleEnd,
                        now
                );
                refreshed++;
            } catch (RuntimeException exception) {
                log.warn("게시판 인기글 정책 갱신 실패: boardId={}", boardId, exception);
            }
        }
        return refreshed;
    }
}
