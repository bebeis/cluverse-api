package cluverse.post.domain;

public enum ImageUploadVersion {
    V1,
    V2,
    V3;

    public String value() {
        return name().toLowerCase();
    }
}
