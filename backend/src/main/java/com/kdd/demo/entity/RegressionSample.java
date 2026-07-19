package com.kdd.demo.entity;

/**
 * 文件作用：描述 regression_data 表中的一条回归样本。
 * 项目位置：Entity 层，供 Spring Data JPA 把数据库行映射成 Java 对象。
 * 交互关系：RegressionRepository 读取本实体，DatasetService 转成前端和 Python 使用的 x/y/type 字段。
 */
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "regression_data")
public class RegressionSample {
    // id 是样本编号；x/y 是回归自变量和因变量；type 用来标记普通样本或噪声点。
    @Id
    private Integer id;

    @Column(name = "x")
    private Double x;

    @Column(name = "y")
    private Double y;

    @Column(name = "type")
    private String type;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
