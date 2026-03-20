package cluverse.group.repository;

import cluverse.group.repository.dto.GroupMemberSummaryQueryDto;
import cluverse.recruitment.domain.RecruitmentStatus;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cluverse.group.domain.QGroup.group;
import static cluverse.interest.domain.QInterest.interest;
import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberProfile.memberProfile;
import static cluverse.recruitment.domain.QRecruitment.recruitment;

@Repository
@RequiredArgsConstructor
public class GroupQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Map<Long, Long> countOpenRecruitments(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }

        return queryFactory
                .select(recruitment.groupId, recruitment.count())
                .from(recruitment)
                .where(
                        recruitment.groupId.in(groupIds),
                        recruitment.status.eq(RecruitmentStatus.OPEN),
                        recruitment.deletedAt.isNull()
                )
                .groupBy(recruitment.groupId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(recruitment.groupId),
                        tuple -> tuple.get(recruitment.count())
                ));
    }

    public Map<Long, GroupMemberSummaryQueryDto> readMemberSummaryMap(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        return queryFactory
                .select(member.id, member.nickname, memberProfile.profileImageUrl)
                .from(member)
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .where(member.id.in(memberIds))
                .fetch()
                .stream()
                .map(this::toMemberSummaryQueryDto)
                .collect(Collectors.toMap(GroupMemberSummaryQueryDto::memberId, summary -> summary));
    }

    public Map<Long, String> readInterestNameMap(Collection<Long> interestIds) {
        if (interestIds == null || interestIds.isEmpty()) {
            return Map.of();
        }

        return queryFactory
                .select(interest.id, interest.name)
                .from(interest)
                .where(interest.id.in(interestIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(interest.id),
                        tuple -> tuple.get(interest.name)
                ));
    }

    private GroupMemberSummaryQueryDto toMemberSummaryQueryDto(Tuple tuple) {
        return new GroupMemberSummaryQueryDto(
                tuple.get(member.id),
                tuple.get(member.nickname),
                tuple.get(memberProfile.profileImageUrl)
        );
    }
}
