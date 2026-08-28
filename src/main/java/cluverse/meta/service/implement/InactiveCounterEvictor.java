package cluverse.meta.service.implement;

import cluverse.meta.repository.TotalViewCountRepository;
import cluverse.meta.repository.dto.ResidentViewCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InactiveCounterEvictor {

    private final TotalViewCountRepository totalViewCountRepository;
    private final PostMetaWriter postMetaWriter;

    public int evict() {
        List<ResidentViewCount> counters = totalViewCountRepository.findInactive();
        if (counters.isEmpty()) {
            return 0;
        }
        // 삭제 전에 전체값을 마지막으로 체크포인트하고, 그사이 조회가 없었던 카운터만 제거한다.
        postMetaWriter.checkpointViewCounts(counters.stream().map(ResidentViewCount::toSnapshot).toList());
        int evicted = 0;
        for (ResidentViewCount counter : counters) {
            if (totalViewCountRepository.deleteIfUnchanged(counter)) {
                evicted++;
            }
        }
        return evicted;
    }
}
