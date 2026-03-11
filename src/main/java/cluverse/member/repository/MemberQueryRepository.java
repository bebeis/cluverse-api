package cluverse.member.repository;

import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static cluverse.member.domain.QMember.member;
import static cluverse.member.domain.QMemberAuth.memberAuth;
import static cluverse.member.domain.QSocialAccount.socialAccount;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

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
}
