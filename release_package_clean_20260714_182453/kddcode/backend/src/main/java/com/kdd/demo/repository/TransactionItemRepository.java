package com.kdd.demo.repository;

import com.kdd.demo.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {
    List<TransactionItem> findAllByOrderByTransactionIdAscItemNameAsc();
}
