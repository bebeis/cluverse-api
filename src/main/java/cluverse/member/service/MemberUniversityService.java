package cluverse.member.service;

import cluverse.member.service.request.MemberUniversityUpdateRequest;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.university.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberUniversityService {

    private final MemberService memberService;
    private final UniversityService universityService;

    public MemberProfileResponse updateUniversity(Long memberId, MemberUniversityUpdateRequest request) {
        universityService.getUniversity(request.universityId());
        return memberService.updateUniversity(memberId, request.universityId());
    }
}
