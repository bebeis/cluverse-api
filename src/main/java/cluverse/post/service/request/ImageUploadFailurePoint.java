package cluverse.post.service.request;

public enum ImageUploadFailurePoint {
    NONE,
    AFTER_FIRST_OBJECT,
    BEFORE_DATABASE_COMPLETE,
    REMOTE_TIMEOUT
}
