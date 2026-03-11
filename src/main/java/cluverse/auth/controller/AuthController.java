package cluverse.auth.controller;

import cluverse.auth.controller.request.LoginRequest;
import cluverse.auth.service.AuthService;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginMember> login(@RequestBody @Valid LoginRequest request,
                                          HttpServletRequest httpRequest) {
        LoginMember loginMember = authService.loginWithEmail(
                request.email(), request.password(), httpRequest.getRemoteAddr(), httpRequest);
        return ApiResponse.ok(loginMember);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }
}
