package cluverse.certification.service.response;

import cluverse.certification.properties.CertificationProviderMode;

public record CertificationBenchmarkReadinessResponse(
        CertificationProviderMode providerMode,
        boolean experimentEndpointsEnabled,
        boolean stubProvider,
        long cacheTtlMillis
) {
}
