package cluverse.certification.client;

import cluverse.certification.domain.CertificationSchedule;
import cluverse.certification.exception.CertificationExceptionMessage;
import cluverse.certification.properties.CertificationProperties;
import cluverse.common.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class DataGoKrCertificationScheduleClient implements CertificationScheduleClient {

    private static final int ANNUAL_SCHEDULE_PAGE_SIZE = 50;
    private static final int MAX_PAGES = 20;

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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
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
            List<CertificationSchedule> schedules = new java.util.ArrayList<>();
            for (int pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
                DataGoKrCertificationResponse response = readPage(year, pageNo);
                validateResponse(response);
                List<CertificationSchedule> page = mapper.map(response);
                schedules.addAll(page);
                Integer totalCount = response.resolvedBody() == null
                        ? null : response.resolvedBody().totalCount();
                if (page.isEmpty()
                        || page.size() < ANNUAL_SCHEDULE_PAGE_SIZE
                        || totalCount != null && schedules.size() >= totalCount) {
                    break;
                }
            }
            return List.copyOf(schedules);
        } catch (RestClientException exception) {
            log.warn("공공 자격시험 API 호출에 실패했습니다. exceptionType={}, causeType={}",
                    exception.getClass().getSimpleName(), rootCauseType(exception));
            throw unavailable(exception);
        }
    }

    private DataGoKrCertificationResponse readPage(int year, int pageNo) {
        return restClient.get()
                .uri(scheduleUri(year, pageNo))
                .retrieve()
                .body(DataGoKrCertificationResponse.class);
    }

    private void validateServiceKey() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw unavailable(new IllegalStateException("DATA_GO_KR_SERVICE_KEY가 설정되지 않았습니다."));
        }
    }

    private URI scheduleUri(int year, int pageNo) {
        return URI.create(properties.providerBaseUrl()
                + "/B490007/qualExamSchd/getQualExamSchdList"
                + "?serviceKey=" + encodedServiceKey()
                + "&numOfRows=" + ANNUAL_SCHEDULE_PAGE_SIZE
                + "&pageNo=" + pageNo
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
            if (response != null && response.resolvedHeader() != null) {
                log.warn("공공 자격시험 API가 오류 코드를 반환했습니다. resultCode={}",
                        response.resolvedHeader().resultCode());
            }
            throw unavailable(new IllegalStateException("공공 API가 정상 응답 코드를 반환하지 않았습니다."));
        }
    }

    private String rootCauseType(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }

    private ExternalServiceException unavailable(Throwable cause) {
        return new ExternalServiceException(CertificationExceptionMessage.SCHEDULE_UNAVAILABLE.getMessage(), cause);
    }
}
