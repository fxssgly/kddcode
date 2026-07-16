package com.kdd.demo.repository;

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
