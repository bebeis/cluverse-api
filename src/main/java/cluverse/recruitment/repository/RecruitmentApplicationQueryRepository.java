package cluverse.recruitment.repository;

import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import cluverse.recruitment.repository.dto.ApplicationChatMessageQueryDto;
import cluverse.recruitment.repository.dto.RecruitmentApplicationSummaryQueryDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberProfile.memberProfile;
import static cluverse.recruitment.domain.QApplicationChatMessage.applicationChatMessage;
import static cluverse.recruitment.domain.QRecruitment.recruitment;
import static cluverse.recruitment.domain.QRecruitmentApplication.recruitmentApplication;

@Repository
@RequiredArgsConstructor
public class RecruitmentApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<RecruitmentApplicationSummaryQueryDto> findMyApplicationSummaries(Long applicantId,
                                                                                  RecruitmentApplicationStatus status,
                                                                                  int page,
                                                                                  int size) {
        return queryFactory
                .select(
                        recruitmentApplication.id,
                        recruitmentApplication.recruitmentId,
                        recruitment.groupId,
                        recruitment.title,
                        recruitmentApplication.applicantId,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        recruitmentApplication.position,
                        recruitmentApplication.portfolioUrl,
                        recruitmentApplication.status,
                        recruitmentApplication.createdAt,
                        recruitmentApplication.reviewedAt
                )
                .from(recruitmentApplication)
                .join(recruitment).on(recruitment.id.eq(recruitmentApplication.recruitmentId))
                .join(member).on(member.id.eq(recruitmentApplication.applicantId))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .where(
                        recruitmentApplication.applicantId.eq(applicantId),
                        statusEq(status)
                )
                .orderBy(recruitmentApplication.createdAt.desc(), recruitmentApplication.id.desc())
                .offset(offset(page, size))
                .limit(size + 1L)
                .fetch()
                .stream()
                .map(tuple -> new RecruitmentApplicationSummaryQueryDto(
                        tuple.get(recruitmentApplication.id),
                        tuple.get(recruitmentApplication.recruitmentId),
                        tuple.get(recruitment.groupId),
                        tuple.get(recruitment.title),
                        tuple.get(recruitmentApplication.applicantId),
                        tuple.get(member.nickname),
                        tuple.get(memberProfile.profileImageUrl),
                        tuple.get(recruitmentApplication.position),
                        tuple.get(recruitmentApplication.portfolioUrl),
                        tuple.get(recruitmentApplication.status),
                        tuple.get(recruitmentApplication.createdAt),
                        tuple.get(recruitmentApplication.reviewedAt)
                ))
                .toList();
    }

    public List<RecruitmentApplicationSummaryQueryDto> findRecruitmentApplicationSummaries(Long recruitmentId,
                                                                                           RecruitmentApplicationStatus status,
                                                                                           int page,
                                                                                           int size) {
        return queryFactory
                .select(
                        recruitmentApplication.id,
                        recruitmentApplication.recruitmentId,
                        recruitment.groupId,
                        recruitment.title,
                        recruitmentApplication.applicantId,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        recruitmentApplication.position,
                        recruitmentApplication.portfolioUrl,
                        recruitmentApplication.status,
                        recruitmentApplication.createdAt,
                        recruitmentApplication.reviewedAt
                )
                .from(recruitmentApplication)
                .join(recruitment).on(recruitment.id.eq(recruitmentApplication.recruitmentId))
                .join(member).on(member.id.eq(recruitmentApplication.applicantId))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .where(
                        recruitmentApplication.recruitmentId.eq(recruitmentId),
                        statusEq(status)
                )
                .orderBy(recruitmentApplication.createdAt.desc(), recruitmentApplication.id.desc())
                .offset(offset(page, size))
                .limit(size + 1L)
                .fetch()
                .stream()
                .map(tuple -> new RecruitmentApplicationSummaryQueryDto(
                        tuple.get(recruitmentApplication.id),
                        tuple.get(recruitmentApplication.recruitmentId),
                        tuple.get(recruitment.groupId),
                        tuple.get(recruitment.title),
                        tuple.get(recruitmentApplication.applicantId),
                        tuple.get(member.nickname),
                        tuple.get(memberProfile.profileImageUrl),
                        tuple.get(recruitmentApplication.position),
                        tuple.get(recruitmentApplication.portfolioUrl),
                        tuple.get(recruitmentApplication.status),
                        tuple.get(recruitmentApplication.createdAt),
                        tuple.get(recruitmentApplication.reviewedAt)
                ))
                .toList();
    }

    public List<ApplicationChatMessageQueryDto> findApplicationMessages(Long applicationId,
                                                                        Long beforeMessageId,
                                                                        int limit) {
        return queryFactory
                .select(
                        applicationChatMessage.id,
                        applicationChatMessage.application.id,
                        applicationChatMessage.senderId,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        applicationChatMessage.content,
                        applicationChatMessage.isRead,
                        applicationChatMessage.createdAt
                )
                .from(applicationChatMessage)
                .join(member).on(member.id.eq(applicationChatMessage.senderId))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .where(
                        applicationChatMessage.application.id.eq(applicationId),
                        beforeMessageIdLt(beforeMessageId)
                )
                .orderBy(applicationChatMessage.createdAt.asc(), applicationChatMessage.id.asc())
                .limit(limit + 1L)
                .fetch()
                .stream()
                .map(tuple -> new ApplicationChatMessageQueryDto(
                        tuple.get(applicationChatMessage.id),
                        tuple.get(applicationChatMessage.application.id),
                        tuple.get(applicationChatMessage.senderId),
                        tuple.get(member.nickname),
                        tuple.get(memberProfile.profileImageUrl),
                        tuple.get(applicationChatMessage.content),
                        Boolean.TRUE.equals(tuple.get(applicationChatMessage.isRead)),
                        tuple.get(applicationChatMessage.createdAt)
                ))
                .toList();
    }

    public ApplicationChatMessageQueryDto findApplicationMessage(Long applicationId, Long messageId) {
        return queryFactory
                .select(
                        applicationChatMessage.id,
                        applicationChatMessage.application.id,
                        applicationChatMessage.senderId,
                        member.nickname,
                        memberProfile.profileImageUrl,
                        applicationChatMessage.content,
                        applicationChatMessage.isRead,
                        applicationChatMessage.createdAt
                )
                .from(applicationChatMessage)
                .join(member).on(member.id.eq(applicationChatMessage.senderId))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .where(
                        applicationChatMessage.application.id.eq(applicationId),
                        applicationChatMessage.id.eq(messageId)
                )
                .fetch()
                .stream()
                .findFirst()
                .map(tuple -> new ApplicationChatMessageQueryDto(
                        tuple.get(applicationChatMessage.id),
                        tuple.get(applicationChatMessage.application.id),
                        tuple.get(applicationChatMessage.senderId),
                        tuple.get(member.nickname),
                        tuple.get(memberProfile.profileImageUrl),
                        tuple.get(applicationChatMessage.content),
                        Boolean.TRUE.equals(tuple.get(applicationChatMessage.isRead)),
                        tuple.get(applicationChatMessage.createdAt)
                ))
                .orElse(null);
    }

    private BooleanExpression statusEq(RecruitmentApplicationStatus status) {
        return status == null ? null : recruitmentApplication.status.eq(status);
    }

    private BooleanExpression beforeMessageIdLt(Long beforeMessageId) {
        return beforeMessageId == null ? null : applicationChatMessage.id.lt(beforeMessageId);
    }

    private long offset(int page, int size) {
        return (long) (page - 1) * size;
    }
}
