package cluverse.member.service.implement;

import cluverse.member.domain.Member;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.FollowRepository;
import cluverse.member.service.request.AddInterestRequest;
import cluverse.member.service.response.MemberInterestResponse;
import cluverse.interest.repository.InterestRepository;
import cluverse.major.repository.MajorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberWriterTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private InterestRepository interestRepository;

    @InjectMocks
    private MemberWriter memberWriter;

    @Test
    void 관심사를_추가할_수_있다() {
        Member member = Member.create("luna", 10L);
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberReader.readOrThrow(1L)).thenReturn(member);
        when(interestRepository.existsById(300L)).thenReturn(true);

        MemberInterestResponse response = memberWriter.addInterest(1L, new AddInterestRequest(300L));

        assertThat(response).isEqualTo(new MemberInterestResponse(300L));
        assertThat(member.getInterests()).containsExactly(300L);
    }
}
