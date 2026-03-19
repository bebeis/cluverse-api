package cluverse.meta.service.implement;

import cluverse.meta.domain.PostViewCountV2;
import cluverse.meta.repository.PostViewCountV2Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PostViewCountV2Writer.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostViewCountV2WriterTest {

    @Autowired
    private PostViewCountV2Writer postViewCountV2Writer;

    @Autowired
    private PostViewCountV2Repository postViewCountV2Repository;

    @Test
    void V2_조회수_레코드가_없으면_생성후_증가시킨다() {
        // when
        postViewCountV2Writer.increaseCount(20L);

        // then
        assertThat(postViewCountV2Repository.findById(20L))
                .get()
                .extracting(PostViewCountV2::getViewCount)
                .isEqualTo(1);
    }

    @Test
    void V2_조회수_레코드가_있으면_낙관적_락_방식으로_증가시킨다() {
        // given
        postViewCountV2Repository.save(PostViewCountV2.create(10L));

        // when
        postViewCountV2Writer.increaseCount(10L);

        // then
        assertThat(postViewCountV2Repository.findById(10L))
                .get()
                .extracting(PostViewCountV2::getViewCount)
                .isEqualTo(1);
    }
}
