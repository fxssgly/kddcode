package com.kdd.demo.entity;

/**
 * 文件作用：描述 transaction_items 表中的一个商品项。
 * 项目位置：Entity 层，表示“一笔交易中的一个商品”，多行会组合成一笔完整交易。
 * 交互关系：TransactionItemRepository 读取这些行，DatasetService 按 transactionId 分组成关联规则算法需要的篮子数据。
 */
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
