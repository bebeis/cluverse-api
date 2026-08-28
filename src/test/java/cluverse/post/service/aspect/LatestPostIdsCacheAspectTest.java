package cluverse.post.service.aspect;

import cluverse.post.repository.dto.PostPageQueryResult;
import cluverse.post.service.implement.LatestPostIdCacheHandler;
import cluverse.post.service.implement.PostPageReader;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.request.PostPageSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LatestPostIdsCacheAspectTest {

    @Test
    void 어노테이션이_붙은_목록_조회는_캐시_Handler를_거친다() {
        PostReader postReader = mock(PostReader.class);
        LatestPostIdCacheHandler cacheHandler = mock(LatestPostIdCacheHandler.class);
        PostPageReader target = new PostPageReader(postReader);
        PostPageReader proxy = proxy(target, cacheHandler);
        PostPageSearchRequest request = new PostPageSearchRequest(3L, null, null, 1, 20);
        PostPageQueryResult databaseResult = new PostPageQueryResult(List.of(), false);
        when(postReader.readOffsetPage(7L, request)).thenReturn(databaseResult);
        when(cacheHandler.fetch(eq(7L), eq(request), eq(201L), any()))
                .thenAnswer(invocation -> origin(invocation.getArgument(3)).get());

        PostPageQueryResult result = proxy.readPage(7L, request, 201L);

        assertThat(result).isSameAs(databaseResult);
        verify(cacheHandler).fetch(eq(7L), eq(request), eq(201L), any());
        verify(postReader).readOffsetPage(7L, request);
    }

    private PostPageReader proxy(PostPageReader target, LatestPostIdCacheHandler cacheHandler) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new LatestPostIdsCacheAspect(cacheHandler));
        return factory.getProxy();
    }

    @SuppressWarnings("unchecked")
    private Supplier<PostPageQueryResult> origin(Object argument) {
        return (Supplier<PostPageQueryResult>) argument;
    }
}
