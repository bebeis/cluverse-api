package cluverse.place.service.request;

public record PlaceVerificationRequest(
        String query,
        String sourceFingerprint,
        boolean recommended
) {
}
