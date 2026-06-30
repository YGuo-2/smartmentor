package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.dto.*;
import com.tricia.smartmentor.service.AuthService;
import com.tricia.smartmentor.service.MailService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MailService mailService;

    public AuthController(AuthService authService, MailService mailService) {
        this.authService = authService;
        this.mailService = mailService;
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/captcha/email")
    public Result<?> sendCaptcha(@Valid @RequestBody CaptchaRequest request) {
        mailService.sendCaptcha(request.getEmail());
        return Result.success("验证码已发送，请查收邮件");
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return Result.success("注册成功", response);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        CurrentUserResponse response = authService.getCurrentUser(principal);
        return Result.success(response);
    }
}
