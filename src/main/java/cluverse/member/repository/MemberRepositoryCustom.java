package cluverse.member.repository;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberMajor;
import cluverse.member.domain.OAuthProvider;

import java.util.List;
import java.util.Optional;

public interface MemberRepositoryCustom {

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    Optional<Member> findBySocialAccount(OAuthProvider provider, String providerUserId);

    List<MemberMajor> findMajorsByMemberId(Long memberId);

    long countActivePostsByMemberId(Long memberId);
}
