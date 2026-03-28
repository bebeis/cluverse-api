package cluverse.feed.service;

import cluverse.common.exception.UnauthorizedException;
import cluverse.feed.service.implement.FeedReader;
import cluverse.feed.service.request.FollowingFeedSearchRequest;
import cluverse.feed.service.request.HomeFeedSearchRequest;
import cluverse.feed.service.response.FeedPageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private FeedReader feedReader;

    @InjectMocks
    private FeedQueryService feedQueryService;

    @Test
    void 홈_피드를_조회한다() {
        // given
        HomeFeedSearchRequest request = new HomeFeedSearchRequest(null, null, 20);
        FeedPageResponse response = FeedPageResponse.empty(20);
        when(feedReader.readHomeFeed(1L, request)).thenReturn(response);

        // when
        FeedPageResponse result = feedQueryService.getHomeFeed(1L, request);

        // then
        assertThat(result).isEqualTo(response);
        verify(feedReader).readHomeFeed(1L, request);
    }

    @Test
    void 비로그인_사용자는_팔로잉_피드를_조회할_수_없다() {
        // given
        FollowingFeedSearchRequest request = new FollowingFeedSearchRequest(null, null, 20);

        // when, then
        assertThatThrownBy(() -> feedQueryService.getFollowingFeed(null, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("인증이 필요합니다.");
        verifyNoInteractions(feedReader);
    }
}
