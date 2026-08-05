package cluverse.certification.service;

import cluverse.certification.properties.CertificationProperties;
import cluverse.certification.properties.CertificationProviderMode;
import cluverse.certification.service.implement.CertificationScheduleReader;
import cluverse.certification.service.response.CertificationBenchmarkReadinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class CertificationBenchmarkService {

    private static final String DATA_GO_KR_HOST = "apis.data.go.kr";

    private final CertificationProperties properties;
    private final CertificationScheduleReader scheduleReader;

    public CertificationBenchmarkReadinessResponse readReadiness() {
        String host = URI.create(properties.providerBaseUrl()).getHost();
        boolean stubProvider = properties.providerMode() == CertificationProviderMode.STUB
                && host != null
                && !host.equalsIgnoreCase(DATA_GO_KR_HOST);
        return new CertificationBenchmarkReadinessResponse(
                properties.providerMode(),
                properties.experimentEndpointsEnabled(),
                stubProvider,
                properties.cacheTtl().toMillis()
        );
    }

    public void evictCache() {
        scheduleReader.evictAll();
    }
}
