package cluverse.post;

import cluverse.post.domain.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class PostDataInitializer {
    static final int BULK_INSERT_SIZE = 2000;
    static final int EXECUTE_COUNT = 5000;

    @PersistenceContext
    EntityManager em;

    @Autowired
    TransactionTemplate transactionTemplate;
    CountDownLatch latch = new CountDownLatch(EXECUTE_COUNT);

    @Test
    void initialize() throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(20);
        for (int i = 0; i < EXECUTE_COUNT; i++) {
            es.submit(() -> {
                insert();
                latch.countDown();
                System.out.println("latch.getCount() = " + latch.getCount());
            });
        }
        latch.await();
        es.shutdown();
    }

    private void insert() {
        transactionTemplate.executeWithoutResult(status -> {
            for (int i = 0; i < BULK_INSERT_SIZE; i++) {
                Post postByMember = Post.createByMember(
                        generateTags(),
                        1L,
                        1L,
                        "title" + i,
                        "content" + i,
                        false,
                        "127.0.0.1"
                );
                em.persist(postByMember);
                if (i % 100 == 0) {
                    em.flush();
                    em.clear();
                }
            }
        });
    }

    private static final List<String> TAG_POOL = List.of(
            "Spring", "Java", "Kotlin", "JPA", "QueryDSL",
            "MySQL", "Redis", "Docker", "AWS", "CI/CD",
            "React", "TypeScript", "Python", "CS", "Algorithm",
            "Backend", "Frontend", "DevOps", "Architecture", "Career"
    );

    private List<String> generateTags() {
        Random random = new Random();
        int count = random.nextInt(4); // 0~3
        List<String> shuffled = new ArrayList<>(TAG_POOL);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, count);
    }
}
