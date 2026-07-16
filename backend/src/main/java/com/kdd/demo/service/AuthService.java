package com.kdd.demo.service;

import com.kdd.demo.entity.UserAccount;
import com.kdd.demo.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {
    /*
     * PBKDF2 参数：迭代次数越高，暴力破解成本越高；盐值保证相同密码也会生成不同哈希；
     * HASH_BITS 控制最终派生密钥长度。
     */
    private static final int HASH_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final UserAccountRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 注入用户仓库，所有认证相关的数据库读写都通过仓库完成。
     */
    public AuthService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 注册用户：规范化用户名、校验账号密码、检查重复用户名，
     * 最后对密码加盐哈希后保存到数据库。
     */
    public Map<String, Object> register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validateCredentials(normalizedUsername, password);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(hashPassword(password));
        UserAccount saved = userRepository.save(user);
        return userResponse(saved);
    }

    /**
     * 登录用户：先按用户名查找账号，再校验密码哈希。
     * 成功后更新最后登录时间，方便后续扩展审计或展示登录记录。
     */
    public Map<String, Object> login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        UserAccount user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        if (!matchesPassword(password == null ? "" : password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        UserAccount saved = userRepository.save(user);
        return userResponse(saved);
    }

    /**
     * 用户名只做首尾空格清理，不改变大小写，避免改变用户输入的语义。
     */
    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    /**
     * 基础账号密码校验，保证进入数据库和加密逻辑的数据是可用的。
     */
    private void validateCredentials(String username, String password) {
        if (username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("Username must be 50 characters or fewer");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    /**
     * 构造返回给前端的用户信息。
     * 出于安全考虑，这里不返回 passwordHash、createdAt 等内部字段。
     */
    private Map<String, Object> userResponse(UserAccount user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        return result;
    }

    /**
     * 使用随机盐和 PBKDF2 生成密码哈希。
     * 存储格式为：算法名$迭代次数$Base64盐$Base64哈希，便于之后调整迭代次数。
     */
    private String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, HASH_ITERATIONS);
        return "pbkdf2$" + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 校验用户输入密码是否匹配数据库中的哈希。
     * 解析失败或格式不符合预期时直接返回 false，不把内部异常暴露给前端。
     */
    private boolean matchesPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            return constantTimeEquals(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 调用 JDK 自带的 PBKDF2WithHmacSHA256 派生密码哈希。
     */
    private byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Password hashing failed", ex);
        }
    }

    /**
     * 常量时间比较，避免因为比较提前退出而泄露哈希匹配进度。
     */
    private boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < expected.length; index++) {
            result |= expected[index] ^ actual[index];
        }
        return result == 0;
    }
}
