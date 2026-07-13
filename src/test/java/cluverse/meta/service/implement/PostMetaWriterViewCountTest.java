package cluverse.meta.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.meta.domain.PostViewCount;
import cluverse.meta.domain.PostViewCountOptimistic;
import cluverse.meta.repository.PostViewCountOptimisticRepository;
import cluverse.meta.repository.PostViewCountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PostMetaWriter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostMetaWriterViewCountTest {

    @Autowired
    private PostMetaWriter postMetaWriter;

    @Autowired
    private PostViewCountOptimisticRepository postViewCountOptimisticRepository;

    @Autowired
    private PostViewCountRepository postViewCountRepository;

    @Test
    void 낙관적_락_조회수_레코드가_없으면_생성후_증가시킨다() {
        // when
        postMetaWriter.increaseViewCountOptimistic(20L);

        // then
        assertThat(postViewCountOptimisticRepository.findById(20L))
                .get()
                .extracting(PostViewCountOptimistic::getViewCount)
                .isEqualTo(1);
    }

    @Test
    void 낙관적_락_조회수_레코드가_있으면_증가시킨다() {
        // given
        postViewCountOptimisticRepository.save(PostViewCountOptimistic.create(10L));

        // when
        postMetaWriter.increaseViewCountOptimistic(10L);

        // then
        assertThat(postViewCountOptimisticRepository.findById(10L))
                .get()
                .extracting(PostViewCountOptimistic::getViewCount)
                .isEqualTo(1);
    }

    @Test
    void 비관적_락_조회수_레코드가_있으면_증가시킨다() {
        // given
        postViewCountRepository.save(PostViewCount.of(30L, 0));

        // when
        postMetaWriter.increaseViewCountPessimistic(30L);

        // then
        assertThat(postViewCountRepository.findById(30L))
                .get()
                .extracting(PostViewCount::getViewCount)
                .isEqualTo(1);
    }

    @Test
    void 비관적_락_조회수_레코드가_없으면_예외가_발생한다() {
        // when // then
        assertThatThrownBy(() -> postMetaWriter.increaseViewCountPessimistic(40L))
                .isInstanceOf(BadRequestException.class);
    }
}
