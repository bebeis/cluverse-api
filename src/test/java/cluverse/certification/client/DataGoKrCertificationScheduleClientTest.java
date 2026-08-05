package cluverse.certification.client;

import cluverse.certification.properties.CertificationProperties;
import cluverse.certification.properties.CertificationProviderMode;
import cluverse.common.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DataGoKrCertificationScheduleClientTest {

    @Test
    void 지원하지_않는_응답_형식도_외부_서비스_예외로_변환한다() {
        // given
        CertificationProperties properties = new CertificationProperties(
                CertificationProviderMode.STUB,
                "https://apis.data.go.kr",
                "test-key",
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofHours(12),
                false,
                ""
        );
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.providerBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/B490007/qualExamSchd/getQualExamSchdList"))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));
        DataGoKrCertificationScheduleClient client = new DataGoKrCertificationScheduleClient(
                properties,
                new CertificationScheduleMapper(),
                restClientBuilder.build()
        );

        // when & then
        assertThatThrownBy(() -> client.readSchedules(2026))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("자격시험 일정을 일시적으로 불러올 수 없습니다.");
        server.verify();
    }
}
