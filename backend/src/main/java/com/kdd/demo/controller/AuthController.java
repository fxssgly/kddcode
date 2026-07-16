package com.kdd.demo.controller;

/**
 * 文件作用：提供注册和登录接口。
 * 项目位置：Controller 层，是前端 LoginView.vue 和后端认证服务之间的入口。
 * 交互关系：收到账号密码后调用 AuthService；AuthService 负责校验、加密、访问 UserAccountRepository。
 */
import com.kdd.demo.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://127.0.0.1:8080",
        "http://localhost:8080",
        "http://127.0.0.1:5173",
        "http://localhost:5173"
})
public class AuthController {
    private final AuthService authService;

    /**
     * 注入认证服务。控制器只负责接收 HTTP 请求和组织响应，
     * 用户校验、密码加密和数据库保存都交给 AuthService。
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册新用户。
     *
     * 请求体需要包含 username 和 password。成功后只返回用户 id 和用户名，
     * 不会把密码或密码哈希暴露给前端。
     */
    @PostMapping("/api/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("user", authService.register(body.get("username"), body.get("password")));
        return result;
    }

    /**
     * 用户登录。
     *
     * 登录成功后返回前端需要保存的最小用户信息；当前项目使用本地存储维护登录态，
     * 没有引入 JWT 或服务端 Session。
     */
    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("user", authService.login(body.get("username"), body.get("password")));
        return result;
    }

    /**
     * 统一处理注册/登录中的业务参数错误。
     *
     * AuthService 通过 IllegalArgumentException 表达“用户名已存在、密码错误”等
     * 可预期问题，这里把它转换成 400 响应和统一 JSON 结构。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("message", ex.getMessage());
        return result;
    }
}
