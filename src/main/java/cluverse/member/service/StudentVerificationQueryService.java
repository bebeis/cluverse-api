package cluverse.member.service;

import cluverse.member.service.response.StudentVerificationStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentVerificationQueryService {

    public StudentVerificationStatusResponse getVerificationStatus(Long memberId) {
        throw new UnsupportedOperationException("학생 인증 상태 조회는 아직 구현되지 않았습니다.");
    }
}
