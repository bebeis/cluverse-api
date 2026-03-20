package cluverse.member.repository;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberMajor;
import cluverse.member.domain.OAuthProvider;
import cluverse.post.domain.PostStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static cluverse.member.domain.QBlock.block;
import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberAuth.memberAuth;
import static cluverse.member.domain.QMemberMajor.memberMajor;
import static cluverse.member.domain.QMemberProfile.memberProfile;
import static cluverse.member.domain.QSocialAccount.socialAccount;
import static cluverse.major.domain.QMajor.major;
import static cluverse.post.domain.QPost.post;
import static cluverse.interest.domain.QInterest.interest;
import static cluverse.university.domain.QUniversity.university;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    public boolean existsByEmail(String email) {
        return queryFactory.selectFrom(memberAuth)
                .where(memberAuth.email.eq(email))
                .fetchFirst() != null;
    }

    public Optional<Member> findByEmail(String email) {
        return Optional.ofNullable(
                queryFactory.selectFrom(member)
                        .join(member.memberAuth, memberAuth).fetchJoin()
                        .where(memberAuth.email.eq(email))
                        .fetchOne()
        );
    }

    public Optional<Member> findBySocialAccount(OAuthProvider provider, String providerUserId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(member)
                        .join(member.socialAccounts, socialAccount).fetchJoin()
                        .where(
                                socialAccount.provider.eq(provider),
                                socialAccount.providerUserId.eq(providerUserId)
                        )
                        .fetchOne()
        );
    }

    public List<MemberMajor> findMajorsByMemberId(Long memberId) {
        return queryFactory.selectFrom(memberMajor)
                .where(memberMajor.member.id.eq(memberId))
                .fetch();
    }

    public List<MemberMajorDetailDto> findMajorDetailsByMemberId(Long memberId) {
        cluverse.major.domain.QMajor parentMajor = new cluverse.major.domain.QMajor("parentMajor");

        return queryFactory
                .select(
                        memberMajor.id,
                        memberMajor.majorId,
                        memberMajor.majorType,
                        major.name,
                        parentMajor.name
                )
                .from(memberMajor)
                .join(major).on(major.id.eq(memberMajor.majorId))
                .leftJoin(parentMajor).on(parentMajor.id.eq(major.parentId))
                .where(memberMajor.member.id.eq(memberId))
                .fetch()
                .stream()
                .map(tuple -> new MemberMajorDetailDto(
                        tuple.get(memberMajor.id),
                        tuple.get(memberMajor.majorId),
                        tuple.get(memberMajor.majorType),
                        tuple.get(major.name),
                        tuple.get(parentMajor.name)
                ))
                .toList();
    }

    public MemberMajorDetailDto findMajorDetailByMemberMajorId(Long memberMajorId) {
        cluverse.major.domain.QMajor parentMajor = new cluverse.major.domain.QMajor("parentMajor");

        return queryFactory
                .select(
                        memberMajor.id,
                        memberMajor.majorId,
                        memberMajor.majorType,
                        major.name,
                        parentMajor.name
                )
                .from(memberMajor)
                .join(major).on(major.id.eq(memberMajor.majorId))
                .leftJoin(parentMajor).on(parentMajor.id.eq(major.parentId))
                .where(memberMajor.id.eq(memberMajorId))
                .fetch()
                .stream()
                .findFirst()
                .map(tuple -> new MemberMajorDetailDto(
                        tuple.get(memberMajor.id),
                        tuple.get(memberMajor.majorId),
                        tuple.get(memberMajor.majorType),
                        tuple.get(major.name),
                        tuple.get(parentMajor.name)
                ))
                .orElse(null);
    }

    public List<BlockedMemberDTO> findBlockedMembersByBlockerId(Long blockerId) {
        return queryFactory
                .select(
                        block.blockedId,
                        member.nickname,
                        member.universityId,
                        university.name,
                        university.badgeImageUrl,
                        memberProfile.profileImageUrl,
                        block.createdAt
                )
                .from(block)
                .join(member).on(block.blockedId.eq(member.id))
                .leftJoin(memberProfile).on(memberProfile.memberId.eq(member.id))
                .leftJoin(university).on(university.id.eq(member.universityId))
                .where(block.blockerId.eq(blockerId))
                .orderBy(block.createdAt.desc())
                .fetch()
                .stream()
                .map(tuple -> new BlockedMemberDTO(
                        tuple.get(block.blockedId),
                        tuple.get(member.nickname),
                        tuple.get(member.universityId),
                        tuple.get(university.name),
                        tuple.get(university.badgeImageUrl),
                        tuple.get(memberProfile.profileImageUrl),
                        tuple.get(block.createdAt)
                ))
                .toList();
    }

    public List<MemberInterestDetailDto> findInterestDetailsByInterestIds(List<Long> interestIds) {
        if (interestIds == null || interestIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(
                        interest.id,
                        interest.name,
                        interest.category
                )
                .from(interest)
                .where(interest.id.in(interestIds))
                .fetch()
                .stream()
                .map(tuple -> new MemberInterestDetailDto(
                        tuple.get(interest.id),
                        tuple.get(interest.name),
                        tuple.get(interest.category)
                ))
                .toList();
    }

    public MemberInterestDetailDto findInterestDetailByInterestId(Long interestId) {
        return queryFactory
                .select(
                        interest.id,
                        interest.name,
                        interest.category
                )
                .from(interest)
                .where(interest.id.eq(interestId))
                .fetch()
                .stream()
                .findFirst()
                .map(tuple -> new MemberInterestDetailDto(
                        tuple.get(interest.id),
                        tuple.get(interest.name),
                        tuple.get(interest.category)
                ))
                .orElse(null);
    }

    public long countActivePostsByMemberId(Long memberId) {
        Long count = queryFactory.select(post.count())
                .from(post)
                .where(
                        post.memberId.eq(memberId),
                        post.status.eq(PostStatus.ACTIVE)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    public record MemberMajorDetailDto(
            Long memberMajorId,
            Long majorId,
            cluverse.member.domain.MajorType majorType,
            String majorName,
            String collegeName
    ) {
    }

    public record MemberInterestDetailDto(
            Long interestId,
            String interestName,
            String category
    ) {
    }

    public record BlockedMemberDTO(
            Long memberId,
            String nickname,
            Long universityId,
            String universityName,
            String universityBadgeImageUrl,
            String profileImageUrl,
            LocalDateTime blockedAt
    ) {
    }
}
