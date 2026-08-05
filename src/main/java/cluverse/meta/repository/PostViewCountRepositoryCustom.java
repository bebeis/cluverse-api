package cluverse.meta.repository;

import cluverse.meta.repository.dto.ViewCountDelta;
import cluverse.meta.repository.dto.ViewCountSnapshot;

import java.util.List;

public interface PostViewCountRepositoryCustom {

    void increaseByDeltas(List<ViewCountDelta> deltas);

    void checkpointViewCounts(List<ViewCountSnapshot> snapshots);
}
