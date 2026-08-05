package cluverse.post.service;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.domain.PostImageUpload;
import cluverse.post.exception.PostImageUploadTimeoutException;
import cluverse.post.service.implement.PostImageUploadExperimentAuthorizer;
import cluverse.post.service.implement.PostImageUploadMetricsRecorder;
import cluverse.post.service.implement.PostImageUploadPreparer;
import cluverse.post.service.implement.PostImageUploadProcessor;
import cluverse.post.service.implement.PostImageUploadReservation;
import cluverse.post.service.implement.PostImageUploadReservationResult;
import cluverse.post.service.implement.PostImageUploadStorageManager;
import cluverse.post.service.implement.PostImageUploadTemporaryFileCleaner;
import cluverse.post.service.implement.PostImageUploadWriter;
import cluverse.post.service.implement.PreparedPostImageUpload;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageUploadResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostImageUploadServiceTest {

    @Test
    void URL이_선택한_버전의_프로세서만_실행한다() {
        PostImageUploadExperimentAuthorizer authorizer = mock(PostImageUploadExperimentAuthorizer.class);
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = mock(PostImageUploadMetricsRecorder.class);
        PostImageUploadProcessor v1 = processor(ImageUploadVersion.V1);
        PostImageUploadProcessor v3 = processor(ImageUploadVersion.V3);
        PostImageUploadRequest request = new PostImageUploadRequest(
                UUID.randomUUID(),
                List.of(new MockMultipartFile("images", "sample.jpg", "image/jpeg", new byte[]{1})),
                null
        );
        PostImageAsset plan = PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1);
        PreparedPostImageUpload prepared = new PreparedPostImageUpload(
                List.of(), List.of(plan), mock(PostImageUploadTemporaryFileCleaner.class));
        PostImageUpload reserved = PostImageUpload.reserve(
                request.requestId(), ImageUploadVersion.V3, List.of(plan));
        PostImageUpload completed = PostImageUpload.completed(
                request.requestId(), ImageUploadVersion.V3,
                List.of(PostImageAsset.completedOriginal(0, "content/a.jpg", "image/jpeg", 1))
        );
        when(preparer.prepare(ImageUploadVersion.V3, request)).thenReturn(prepared);
        when(writer.read(request.requestId(), ImageUploadVersion.V3)).thenReturn(Optional.empty());
        when(reservation.reserve(request.requestId(), ImageUploadVersion.V3, prepared.assets()))
                .thenReturn(new PostImageUploadReservationResult(reserved, true));
        when(v3.process(prepared.images(), request.failurePoint())).thenReturn(List.of());
        when(writer.complete(isNull(), any())).thenReturn(completed);
        when(storageManager.deleteStaging(completed)).thenReturn(true);
        PostImageUploadService service = new PostImageUploadService(
                authorizer, preparer, reservation, writer, storageManager, metricsRecorder, List.of(v1, v3));

        PostImageUploadResponse response = service.upload(ImageUploadVersion.V3, "secret", request);

        assertThat(response.version()).isEqualTo("v3");
        verify(authorizer).authorize("secret");
        verify(v3).process(prepared.images(), request.failurePoint());
        verify(v1, never()).process(any(), any());
    }

    @Test
    void 진행_중인_requestId의_거절도_실패_메트릭에_포함한다() {
        PostImageUploadExperimentAuthorizer authorizer = mock(PostImageUploadExperimentAuthorizer.class);
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = mock(PostImageUploadMetricsRecorder.class);
        PostImageUploadProcessor processor = processor(ImageUploadVersion.V1);
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = new PostImageUploadRequest(
                requestId,
                List.of(new MockMultipartFile("images", "sample.jpg", "image/jpeg", new byte[]{1})),
                null
        );
        PostImageUpload pending = PostImageUpload.reserve(
                requestId,
                ImageUploadVersion.V1,
                List.of(PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1))
        );
        when(writer.read(requestId, ImageUploadVersion.V1)).thenReturn(Optional.of(pending));
        PostImageUploadService service = new PostImageUploadService(
                authorizer,
                preparer,
                reservation,
                writer,
                storageManager,
                metricsRecorder,
                List.of(processor)
        );

        assertThatThrownBy(() -> service.upload(ImageUploadVersion.V1, "secret", request))
                .isInstanceOf(cluverse.common.exception.BadRequestException.class);

        verify(metricsRecorder).request(eq(ImageUploadVersion.V1), eq("failure"), anyLong());
        verify(preparer, never()).prepare(any(), any());
    }

    @Test
    void 외부_처리_timeout은_늦은_Lambda와_경쟁하지_않도록_즉시_보상하지_않는다() {
        PostImageUploadExperimentAuthorizer authorizer = mock(PostImageUploadExperimentAuthorizer.class);
        PostImageUploadPreparer preparer = mock(PostImageUploadPreparer.class);
        PostImageUploadReservation reservation = mock(PostImageUploadReservation.class);
        PostImageUploadWriter writer = mock(PostImageUploadWriter.class);
        PostImageUploadStorageManager storageManager = mock(PostImageUploadStorageManager.class);
        PostImageUploadMetricsRecorder metricsRecorder = mock(PostImageUploadMetricsRecorder.class);
        PostImageUploadProcessor processor = processor(ImageUploadVersion.V3);
        UUID requestId = UUID.randomUUID();
        PostImageUploadRequest request = new PostImageUploadRequest(
                requestId,
                List.of(new MockMultipartFile("images", "sample.jpg", "image/jpeg", new byte[]{1})),
                null
        );
        PostImageAsset plan = PostImageAsset.plan(0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", 1);
        PreparedPostImageUpload prepared = new PreparedPostImageUpload(
                List.of(), List.of(plan), mock(PostImageUploadTemporaryFileCleaner.class));
        PostImageUpload reserved = PostImageUpload.reserve(requestId, ImageUploadVersion.V3, List.of(plan));
        when(writer.read(requestId, ImageUploadVersion.V3)).thenReturn(Optional.empty());
        when(preparer.prepare(ImageUploadVersion.V3, request)).thenReturn(prepared);
        when(reservation.reserve(requestId, ImageUploadVersion.V3, prepared.assets()))
                .thenReturn(new PostImageUploadReservationResult(reserved, true));
        when(processor.process(prepared.images(), request.failurePoint()))
                .thenThrow(new PostImageUploadTimeoutException("timeout", new IllegalStateException()));
        PostImageUploadService service = new PostImageUploadService(
                authorizer, preparer, reservation, writer, storageManager, metricsRecorder, List.of(processor));

        assertThatThrownBy(() -> service.upload(ImageUploadVersion.V3, "secret", request))
                .isInstanceOf(PostImageUploadTimeoutException.class);

        verify(storageManager, never()).compensate(any());
        verify(writer, never()).fail(any(), any());
    }

    private PostImageUploadProcessor processor(ImageUploadVersion version) {
        PostImageUploadProcessor processor = mock(PostImageUploadProcessor.class);
        when(processor.version()).thenReturn(version);
        return processor;
    }
}
