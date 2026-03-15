package cluverse.recruitment.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.group.domain.Group;
import cluverse.group.exception.GroupExceptionMessage;
import cluverse.group.repository.GroupRepository;
import cluverse.member.domain.Member;
import cluverse.member.repository.MemberRepository;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.repository.RecruitmentRepository;
import cluverse.recruitment.service.request.RecruitmentSearchRequest;
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
public class RecruitmentReader {

    private final RecruitmentRepository recruitmentRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    public Recruitment readOrThrow(Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_NOT_FOUND.getMessage()));
        validateActive(recruitment);
        return recruitment;
    }

    public Group readGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException(GroupExceptionMessage.GROUP_NOT_FOUND.getMessage()));
    }

    public List<Recruitment> readRecruitments(RecruitmentSearchRequest request) {
        return recruitmentRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(recruitment -> request.groupId() == null || recruitment.getGroupId().equals(request.groupId()))
                .filter(recruitment -> request.status() == null || recruitment.getStatus() == request.status())
                .filter(recruitment -> !Boolean.TRUE.equals(request.recruitingOnly()) || recruitment.isOpen())
                .toList();
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
