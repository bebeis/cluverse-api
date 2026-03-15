package cluverse.recruitment.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.group.domain.Group;
import cluverse.group.exception.GroupExceptionMessage;
import cluverse.group.repository.GroupRepository;
import cluverse.member.domain.Member;
import cluverse.member.repository.MemberRepository;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.repository.RecruitmentApplicationRepository;
import cluverse.recruitment.repository.RecruitmentRepository;
import cluverse.recruitment.service.request.RecruitmentApplicationSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationReader {

    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    public RecruitmentApplication readOrThrow(Long applicationId) {
        return recruitmentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_NOT_FOUND.getMessage()
                ));
    }

    public Recruitment readRecruitmentOrThrow(Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_NOT_FOUND.getMessage()));
        validateActive(recruitment);
        return recruitment;
    }

    public Group readGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
    }

    public List<RecruitmentApplication> readMyApplications(Long memberId, RecruitmentApplicationSearchRequest request) {
        return recruitmentApplicationRepository.findAllByApplicantIdOrderByCreatedAtDesc(memberId).stream()
                .filter(application -> request.status() == null || application.getStatus() == request.status())
                .toList();
    }

    public List<RecruitmentApplication> readApplications(Long recruitmentId, RecruitmentApplicationSearchRequest request) {
        return recruitmentApplicationRepository.findAllByRecruitmentIdOrderByCreatedAtDesc(recruitmentId).stream()
                .filter(application -> request.status() == null || application.getStatus() == request.status())
                .toList();
    }

    public boolean existsByRecruitmentAndApplicant(Long recruitmentId, Long applicantId) {
        return recruitmentApplicationRepository.findByRecruitmentIdAndApplicantId(recruitmentId, applicantId).isPresent();
    }

    public Map<Long, Recruitment> readRecruitmentMap(Collection<Long> recruitmentIds) {
        return recruitmentRepository.findAllById(recruitmentIds).stream()
                .collect(Collectors.toMap(Recruitment::getId, Function.identity()));
    }

    public Map<Long, Member> readMemberMap(Collection<Long> memberIds) {
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    private void validateActive(Recruitment recruitment) {
        if (recruitment.isDeleted()) {
            throw new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_NOT_FOUND.getMessage());
        }
    }
}
