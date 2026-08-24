package cluverse.post;

import cluverse.post.client.PostImageObjectStorageClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class PostImageStorageTestConfig {

    @Bean
    PostImageObjectStorageClient postImageObjectStorageClient() {
        PostImageObjectStorageClient storage = mock(PostImageObjectStorageClient.class);
        when(storage.createImageUrl(anyString()))
                .thenAnswer(invocation -> "https://images.example.com/" + invocation.getArgument(0));
        return storage;
    }
}
