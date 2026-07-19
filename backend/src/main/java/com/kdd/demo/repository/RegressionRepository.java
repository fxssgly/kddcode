package com.kdd.demo.repository;

/**
 * 文件作用：RegressionSample 的数据库访问接口。
 * 项目位置：Repository 层，由 Spring Data JPA 自动实现。
 * 交互关系：DatasetService 调用 findAllByOrderByIdAsc，把 regression_data 表按 id 顺序读给前端和回归算法。
 */
import com.kdd.demo.entity.RegressionSample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegressionRepository extends JpaRepository<RegressionSample, Integer> {
    // Spring Data 会根据方法名生成“按 id 升序查询全部样本”的 SQL。
    List<RegressionSample> findAllByOrderByIdAsc();
}
