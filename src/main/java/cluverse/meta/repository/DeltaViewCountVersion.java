package cluverse.meta.repository;

public enum DeltaViewCountVersion {
    TIME_BASED("v2"),
    THRESHOLD("v3");

    private final String keySegment;

    DeltaViewCountVersion(String keySegment) {
        this.keySegment = keySegment;
    }

    public String keySegment() {
        return keySegment;
    }
}
