package cluverse.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import cluverse.member.domain.QMember;
import cluverse.member.domain.QMemberAuth;
import cluverse.member.domain.QSocialAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<Member> findByEmail(String email) {
        QMember member = QMember.member;
        QMemberAuth memberAuth = QMemberAuth.memberAuth;

        return Optional.ofNullable(
                queryFactory.selectFrom(member)
                        .join(member.memberAuth, memberAuth).fetchJoin()
                        .where(memberAuth.email.eq(email))
                        .fetchOne()
        );
    }

    public Optional<Member> findBySocialAccount(OAuthProvider provider, String providerUserId) {
        QMember member = QMember.member;
        QSocialAccount socialAccount = QSocialAccount.socialAccount;

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
