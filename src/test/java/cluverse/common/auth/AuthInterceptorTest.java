package cluverse.common.auth;

import cluverse.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class AuthInterceptorTest {

    private final AuthInterceptor authInterceptor = new AuthInterceptor(
            List.of(
                    "/api/v1/posts",
                    "/api/v1/posts/*",
                    "/api/v1/universities",
                    "/api/v1/universities/*",
                    "/api/v1/groups",
                    "/api/v1/groups/",
                    "/api/v1/groups/*",
                    "/api/v1/boards",
                    "/api/v1/boards/*",
                    "/api/v1/boards/*/home",
                    "/api/v1/majors",
                    "/api/v1/interests",
                    "/api/v1/terms",
                    "/api/v1/members/nickname/availability"
            ),
            List.of(
                    "/api/v1/groups/me"
            )
    );

    @Test
    void 비회원도_게시글_목록을_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/posts");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_게시글_상세를_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/posts/1");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원은_게시글을_작성할_수_없다() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/posts");

        assertThatThrownBy(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 비회원도_약관을_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/terms");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_관심사를_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/interests");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_전공을_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/majors");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_닉네임_중복_확인을_할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/nickname/availability");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_보드_홈을_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/boards/1/home");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_그룹_목록을_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_그룹_상세를_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups/1");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원도_슬래시가_붙은_그룹_목록을_조회할_수_있다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups/");

        assertThatCode(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void 비회원은_내_그룹_목록을_조회할_수_없다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups/me");

        assertThatThrownBy(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 비회원은_대학교를_생성할_수_없다() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/universities");

        assertThatThrownBy(() -> authInterceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .isInstanceOf(UnauthorizedException.class);
    }
}
