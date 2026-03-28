package cluverse.member.service;

import cluverse.member.service.request.MemberUniversityUpdateRequest;
import cluverse.member.service.response.MemberProfileResponse;
import cluverse.university.service.implement.UniversityReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberUniversityService {

    private final MemberService memberService;
    private final UniversityReader universityReader;

    public MemberProfileResponse updateUniversity(Long memberId, MemberUniversityUpdateRequest request) {
        universityReader.readOrThrow(request.universityId());
        return memberService.updateUniversity(memberId, request.universityId());
    }
}
