package cluverse.meta.repository;

import cluverse.meta.repository.dto.ViewCountDelta;

import java.util.List;
import java.util.OptionalLong;

public interface PostViewCountRepositoryCustom {

    OptionalLong increaseAndGet(Long postId);

    void increaseByDeltas(List<ViewCountDelta> deltas);
}
