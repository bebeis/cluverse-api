package cluverse.member.repository;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberMajor;
import cluverse.member.domain.OAuthProvider;
import cluverse.post.domain.PostStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberAuth.memberAuth;
import static cluverse.member.domain.QMemberMajor.memberMajor;
import static cluverse.member.domain.QSocialAccount.socialAccount;
import static cluverse.post.domain.QPost.post;

@Repository
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public boolean existsByEmail(String email) {
        return queryFactory.selectFrom(memberAuth)
                .where(memberAuth.email.eq(email))
                .fetchFirst() != null;
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return Optional.ofNullable(
                queryFactory.selectFrom(member)
                        .join(member.memberAuth, memberAuth).fetchJoin()
                        .where(memberAuth.email.eq(email))
                        .fetchOne()
        );
    }

    @Override
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

    @Override
    public List<MemberMajor> findMajorsByMemberId(Long memberId) {
        return queryFactory.selectFrom(memberMajor)
                .where(memberMajor.member.id.eq(memberId))
                .fetch();
    }

    @Override
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
}
