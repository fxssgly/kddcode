package com.kdd.demo.repository;

import com.kdd.demo.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户账号仓库。
 * Spring Data 会根据方法名自动生成按用户名查询和判断用户名是否存在的 SQL。
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    /**
     * 按用户名查找用户。登录时用 Optional 表达“用户可能不存在”的情况。
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * 注册前检查用户名是否已被占用，避免触发数据库唯一索引异常。
     */
    boolean existsByUsername(String username);
}
