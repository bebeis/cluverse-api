package cluverse.certification.client;

import cluverse.certification.domain.CertificationSchedule;
import cluverse.certification.exception.CertificationExceptionMessage;
import cluverse.certification.properties.CertificationProperties;
import cluverse.common.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class DataGoKrCertificationScheduleClient implements CertificationScheduleClient {

    private static final int ANNUAL_SCHEDULE_PAGE_SIZE = 1_000;

    private final CertificationProperties properties;
    private final CertificationScheduleMapper mapper;
    private final RestClient restClient;

    @Autowired
    public DataGoKrCertificationScheduleClient(
            CertificationProperties properties,
            CertificationScheduleMapper mapper
    ) {
        this(properties, mapper, createRestClient(properties));
    }

    DataGoKrCertificationScheduleClient(
            CertificationProperties properties,
            CertificationScheduleMapper mapper,
            RestClient restClient
    ) {
        this.properties = properties;
        this.mapper = mapper;
        this.restClient = restClient;
    }

    private static RestClient createRestClient(CertificationProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.providerBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<CertificationSchedule> readSchedules(int year) {
        validateServiceKey();
        try {
            DataGoKrCertificationResponse response = restClient.get()
                    .uri(scheduleUri(year))
                    .retrieve()
                    .body(DataGoKrCertificationResponse.class);
            validateResponse(response);
            return mapper.map(response);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private void validateServiceKey() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw unavailable(new IllegalStateException("DATA_GO_KR_SERVICE_KEY가 설정되지 않았습니다."));
        }
    }

    private URI scheduleUri(int year) {
        return URI.create(properties.providerBaseUrl()
                + "/B490007/qualExamSchd/getQualExamSchdList"
                + "?serviceKey=" + encodedServiceKey()
                + "&numOfRows=" + ANNUAL_SCHEDULE_PAGE_SIZE
                + "&pageNo=1"
                + "&dataFormat=json"
                + "&implYy=" + year);
    }

    private String encodedServiceKey() {
        String serviceKey = properties.serviceKey();
        return serviceKey.contains("%")
                ? serviceKey
                : URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
    }

    private void validateResponse(DataGoKrCertificationResponse response) {
        if (response == null
                || response.resolvedHeader() == null
                || !"00".equals(response.resolvedHeader().resultCode())) {
            throw unavailable(new IllegalStateException("공공 API가 정상 응답 코드를 반환하지 않았습니다."));
        }
    }

    private ExternalServiceException unavailable(Throwable cause) {
        return new ExternalServiceException(CertificationExceptionMessage.SCHEDULE_UNAVAILABLE.getMessage(), cause);
    }
}
