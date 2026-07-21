package com.kdd.demo.repository;

/**
 * 文件作用：IrisSample 的数据库访问接口。
 * 项目位置：Repository 层，由 Spring Data JPA 自动实现，不需要手写 SQL。
 * 交互关系：DatasetService 在启用 MySQL 时调用 findAll，把数据库中的 Iris 样本读出来。
 */
import com.kdd.demo.entity.IrisSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Iris 样本的 Spring Data 仓库。
 * 启用 MySQL 模式时，DatasetService 会使用 JpaRepository 提供的基础
 * CRUD 和 findAll 方法读取数据。
 */
public interface IrisRepository extends JpaRepository<IrisSample, Integer> {
    @Query(value = "SELECT id, SepL, SepW, PetL, PetW, Species FROM iris2", nativeQuery = true)
    List<IrisSample> findAllFromIris2();
}
