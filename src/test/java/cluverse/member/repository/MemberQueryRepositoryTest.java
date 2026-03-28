package cluverse.member.repository;

import cluverse.common.config.QuerydslConfig;
import cluverse.member.domain.Block;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberAuth;
import cluverse.member.domain.MemberProfile;
import cluverse.member.domain.OAuthProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({MemberQueryRepository.class, MemberRepositoryImpl.class, QuerydslConfig.class})
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
        Optional<Member> result = memberRepository.findByEmail("test@example.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("testuser");
        assertThat(result.get().getMemberAuth().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void 이메일로_회원_조회_없는_경우() {
        // when
        Optional<Member> result = memberRepository.findByEmail("none@example.com");

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
        Optional<Member> result = memberRepository.findBySocialAccount(OAuthProvider.KAKAO, "kakao-provider-id");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("kakaouser");
    }

    @Test
    void 소셜_계정으로_회원_조회_없는_경우() {
        // when
        Optional<Member> result = memberRepository.findBySocialAccount(OAuthProvider.GOOGLE, "nonexistent-id");

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
        Optional<Member> result = memberRepository.findBySocialAccount(OAuthProvider.KAKAO, "google-provider-id");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 차단한_회원_목록을_조회할_수_있다() {
        // given
        insertUniversity(1L, "클루대", "cluverse.ac.kr", "https://cdn.example.com/badge.png");

        Member blocker = Member.create("blocker", 1L);
        Member blocked = Member.create("blocked", 1L);
        MemberProfile profile = MemberProfile.create(blocked);
        profile.update(
                "소개",
                2024,
                "https://cdn.example.com/profile.png",
                null,
                null,
                null,
                null,
                null,
                true,
                java.util.List.of()
        );
        blocked.initProfile(profile);

        memberRepository.save(blocker);
        memberRepository.save(blocked);
        em.persist(Block.of(blocker.getId(), blocked.getId()));
        em.flush();
        em.clear();

        // when
        java.util.List<MemberQueryRepository.BlockedMemberDTO> result =
                memberQueryRepository.findBlockedMembersByBlockerId(blocker.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().memberId()).isEqualTo(blocked.getId());
        assertThat(result.getFirst().nickname()).isEqualTo("blocked");
        assertThat(result.getFirst().universityName()).isEqualTo("클루대");
        assertThat(result.getFirst().universityBadgeImageUrl()).isEqualTo("https://cdn.example.com/badge.png");
        assertThat(result.getFirst().profileImageUrl()).isEqualTo("https://cdn.example.com/profile.png");
    }

    private void insertUniversity(Long universityId, String name, String emailDomain, String badgeImageUrl) {
        em.createNativeQuery("""
                        INSERT INTO university (university_id, name, email_domain, badge_image_url, is_active, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """)
                .setParameter(1, universityId)
                .setParameter(2, name)
                .setParameter(3, emailDomain)
                .setParameter(4, badgeImageUrl)
                .setParameter(5, true)
                .executeUpdate();
    }
}
