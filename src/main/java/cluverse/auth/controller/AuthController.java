package cluverse.auth.controller;

import cluverse.auth.service.AuthService;
import cluverse.auth.service.request.LoginRequest;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.LoginMember;
import cluverse.common.auth.LoginSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginSessionManager loginSessionManager;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoginMember> register(@RequestBody @Valid MemberRegisterRequest request,
                                             HttpServletRequest httpRequest) {
        LoginMember loginMember = authService.register(request, httpRequest.getRemoteAddr());
        loginSessionManager.createSession(httpRequest, loginMember);
        return ApiResponse.created(loginMember);
    }

    @PostMapping("/login")
    public ApiResponse<LoginMember> login(@RequestBody @Valid LoginRequest request,
                                          HttpServletRequest httpRequest) {
        LoginMember loginMember = authService.loginWithEmail(
                request.email(), request.password(), httpRequest.getRemoteAddr());
        loginSessionManager.createSession(httpRequest, loginMember);
        return ApiResponse.ok(loginMember);
    }

    @PostMapping("/oauth/token")
    public ApiResponse<LoginMember> exchangeOAuthToken(@RequestParam String token,
                                                       HttpServletRequest httpRequest) {
        LoginMember loginMember = authService.exchangeOAuthToken(token);
        loginSessionManager.createSession(httpRequest, loginMember);
        return ApiResponse.ok(loginMember);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        loginSessionManager.invalidateSession(request);
        return ApiResponse.ok();
    }
}
