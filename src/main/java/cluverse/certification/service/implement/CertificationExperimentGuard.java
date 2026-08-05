package cluverse.certification.service.implement;

import cluverse.certification.properties.CertificationProperties;
import cluverse.certification.properties.CertificationProviderMode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class CertificationExperimentGuard implements InitializingBean {

    private static final String DATA_GO_KR_HOST = "apis.data.go.kr";

    private final CertificationProperties properties;

    public CertificationExperimentGuard(CertificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.experimentEndpointsEnabled()) {
            return;
        }
        if (properties.providerMode() != CertificationProviderMode.STUB) {
            throw new IllegalStateException("자격시험 실험 API는 STUB provider에서만 활성화할 수 있습니다.");
        }
        String host = URI.create(properties.providerBaseUrl()).getHost();
        if (host == null || host.equalsIgnoreCase(DATA_GO_KR_HOST)) {
            throw new IllegalStateException("자격시험 실험 API가 실제 공공데이터포털 host를 가리키고 있습니다.");
        }
    }
}
