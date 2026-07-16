package com.kdd.demo.repository;

/**
 * 文件作用：TransactionItem 的数据库访问接口。
 * 项目位置：Repository 层，负责从 transaction_items 表读取事务商品项。
 * 交互关系：DatasetService 调用按 transactionId 和 itemName 排序的方法，让后续分组结果稳定。
 */
import com.kdd.demo.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 事务篮子数据的仓库。
 * 自定义查询方法会按固定顺序返回记录，保证按 transaction_id 分组时结果稳定。
 */
public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {
    List<TransactionItem> findAllByOrderByTransactionIdAscItemNameAsc();
}
