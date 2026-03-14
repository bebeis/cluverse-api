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

    private final AuthInterceptor authInterceptor = new AuthInterceptor(List.of(
            "/api/v1/posts",
            "/api/v1/posts/*"
    ));

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
}
