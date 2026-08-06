package cluverse.place.service.implement;

import cluverse.place.properties.LocalMapProperties;
import cluverse.place.properties.PlaceProviderMode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class LocalMapExperimentGuard implements InitializingBean {

    private final LocalMapProperties properties;

    public LocalMapExperimentGuard(LocalMapProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.experimentEndpointsEnabled()) {
            return;
        }
        if (properties.providerMode() != PlaceProviderMode.STUB) {
            throw new IllegalStateException("로컬맵 실험 API는 STUB provider에서만 활성화할 수 있습니다.");
        }
    }
}
