package cluverse.member.repository;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberInterest;
import cluverse.member.domain.MemberMajor;
import cluverse.member.domain.OAuthProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static cluverse.member.domain.QBlock.block;
import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberAuth.memberAuth;
import static cluverse.member.domain.QMemberInterest.memberInterest;
import static cluverse.member.domain.QMemberMajor.memberMajor;
import static cluverse.member.domain.QMemberProfile.memberProfile;
import static cluverse.member.domain.QSocialAccount.socialAccount;
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

    public List<MemberInterest> findInterestsByMemberId(Long memberId) {
        return queryFactory.selectFrom(memberInterest)
                .where(memberInterest.member.id.eq(memberId))
                .fetch();
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
