package com.kdd.demo.repository;

import com.kdd.demo.entity.IrisSample;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Iris 样本的 Spring Data 仓库。
 * 启用 MySQL 模式时，DatasetService 会使用 JpaRepository 提供的基础
 * CRUD 和 findAll 方法读取数据。
 */
public interface IrisRepository extends JpaRepository<IrisSample, Integer> {
}
