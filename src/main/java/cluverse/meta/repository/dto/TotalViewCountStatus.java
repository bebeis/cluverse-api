package cluverse.meta.repository.dto;

public enum TotalViewCountStatus {
    REINITIALIZE(-1),
    DUPLICATE(0),
    COUNTED(1);

    private final long code;

    TotalViewCountStatus(long code) {
        this.code = code;
    }

    public static TotalViewCountStatus from(long code) {
        for (TotalViewCountStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 전체 조회수 집계 결과입니다: " + code);
    }
}
