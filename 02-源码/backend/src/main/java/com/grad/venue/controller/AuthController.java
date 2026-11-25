package com.grad.venue.controller;

import com.grad.venue.common.R;
import com.grad.venue.common.UserContext;
import com.grad.venue.entity.SysUser;
import com.grad.venue.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 登录、个人信息、改密 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return R.fail(400, "请输入账号和密码");
        }
        return R.ok(authService.login(username.trim(), password));
    }

    @GetMapping("/me")
    public R<SysUser> me() {
        return R.ok(authService.profile(UserContext.id()));
    }

    @PutMapping("/profile")
    public R<Void> profile(@RequestBody Map<String, String> body) {
        authService.updateProfile(UserContext.id(), body.get("realName"), body.get("phone"), body.get("avatar"));
        return R.ok();
    }

    @PutMapping("/password")
    public R<Void> password(@RequestBody Map<String, String> body) {
        authService.changePassword(UserContext.id(), body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @GetMapping("/menus")
    public R<List<com.grad.venue.entity.SysMenu>> menus() {
        return R.ok(authService.menus(UserContext.role()));
    }
}
