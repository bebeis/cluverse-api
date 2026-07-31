package cluverse.meta.service.implement;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;

/**
 * Write-back 반영 실패 분류 (조회수 증가 API V4).
 * 커밋 여부가 모호한 실패에서 복구(restore)하면 같은 증가량이 두 번 반영될 수 있다 —
 * 롤백이 확실한 실패에만 복구하고, 모호하면 한 주기치 유실을 감수한다.
 */
final class WriteBackFailurePolicy {

    private WriteBackFailurePolicy() {
    }

    static boolean isRollbackCertain(DataAccessException exception) {
        return !(exception instanceof QueryTimeoutException
                || exception instanceof RecoverableDataAccessException
                || exception instanceof DataAccessResourceFailureException
                || exception instanceof TransientDataAccessResourceException);
    }
}
