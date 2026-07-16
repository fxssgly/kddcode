package com.kdd.demo.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "transaction_items")
public class TransactionItem {
    /**
     * 事务篮子中单个商品项的数据库行映射。
     *
     * 多行数据可以拥有相同的 transactionId；DatasetService 会按事务编号
     * 分组还原成 List<List<String>>，供关联规则挖掘使用。
     */
    @Id
    private Long id;

    @Column(name = "transaction_id")
    private Integer transactionId;

    @Column(name = "item_name")
    private String itemName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
