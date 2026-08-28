package cluverse.post.service.aspect;

import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.LatestPostIdCacheHandler;
import cluverse.post.service.request.PostPageSearchRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class LatestPostIdsCacheAspect {

    private final LatestPostIdCacheHandler cacheHandler;

    @Around("@annotation(cluverse.post.service.annotation.LatestPostIdsCacheable)"
            + " && args(memberId, request, countLimit)")
    public PostPageQueryResult read(
            ProceedingJoinPoint joinPoint,
            Long memberId,
            PostPageSearchRequest request,
            long countLimit
    ) {
        return cacheHandler.fetch(memberId, request, countLimit, () -> proceed(joinPoint));
    }

    private PostPageQueryResult proceed(ProceedingJoinPoint joinPoint) {
        try {
            return (PostPageQueryResult) joinPoint.proceed();
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new IllegalStateException("게시글 목록 원본 조회 실행에 실패했습니다.", failure);
        }
    }
}
