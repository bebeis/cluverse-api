package cluverse.member.repository;

import cluverse.common.config.QuerydslConfig;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberAuth;
import cluverse.member.domain.OAuthProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({MemberQueryRepository.class, QuerydslConfig.class})
class MemberQueryRepositoryTest {

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    @Test
    void 이메일로_회원_조회_성공() {
        // given
        Member member = Member.create("testuser", 1L);
        memberRepository.save(member);

        MemberAuth memberAuth = MemberAuth.ofEmail(member, "test@example.com", "hashedpw");
        em.persist(memberAuth);
        em.flush();
        em.clear();

        // when
        Optional<Member> result = memberQueryRepository.findByEmail("test@example.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("testuser");
        assertThat(result.get().getMemberAuth().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void 이메일로_회원_조회_없는_경우() {
        // when
        Optional<Member> result = memberQueryRepository.findByEmail("none@example.com");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 소셜_계정으로_회원_조회_성공() {
        // given
        Member member = Member.create("kakaouser", 1L);
        member.initMemberAuthBySocial("kakao@example.com");
        member.addSocialAccount(OAuthProvider.KAKAO, "kakao-provider-id");
        memberRepository.save(member);
        em.flush();
        em.clear();

        // when
        Optional<Member> result = memberQueryRepository.findBySocialAccount(OAuthProvider.KAKAO, "kakao-provider-id");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("kakaouser");
    }

    @Test
    void 소셜_계정으로_회원_조회_없는_경우() {
        // when
        Optional<Member> result = memberQueryRepository.findBySocialAccount(OAuthProvider.GOOGLE, "nonexistent-id");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 다른_provider로_조회시_미조회() {
        // given
        Member member = Member.create("googleuser", 1L);
        member.initMemberAuthBySocial("google@example.com");
        member.addSocialAccount(OAuthProvider.GOOGLE, "google-provider-id");
        memberRepository.save(member);
        em.flush();
        em.clear();

        // when
        Optional<Member> result = memberQueryRepository.findBySocialAccount(OAuthProvider.KAKAO, "google-provider-id");

        // then
        assertThat(result).isEmpty();
    }
}
