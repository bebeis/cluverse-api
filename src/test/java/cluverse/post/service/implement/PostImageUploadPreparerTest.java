package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.properties.PostImageUploadProperties;
import cluverse.post.service.request.ImageUploadFailurePoint;
import cluverse.post.service.request.PostImageUploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostImageUploadPreparerTest {

    @Mock
    private PostImageUploadProperties properties;
    @Mock
    private PostImageUploadTemporaryFileCleaner temporaryFileCleaner;
    @Mock
    private MultipartFile multipartFile;

    private PostImageUploadPreparer preparer;

    @BeforeEach
    void setUp() {
        when(properties.maxFileSize()).thenReturn(DataSize.ofMegabytes(10));
        preparer = new PostImageUploadPreparer(properties, temporaryFileCleaner);
    }

    @Test
    void servlet_임시_파일을_애플리케이션_소유_파일로_이동한다() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
        }));
        PostImageUploadRequest request = new PostImageUploadRequest(
                UUID.randomUUID(),
                List.of(multipartFile),
                ImageUploadFailurePoint.NONE
        );

        PreparedPostImageUpload prepared = preparer.prepare(ImageUploadVersion.V3, request);
        Path preparedPath = prepared.images().getFirst().path();
        prepared.close();

        verify(multipartFile).transferTo(any(File.class));
        verify(temporaryFileCleaner).delete(preparedPath);
    }
}
