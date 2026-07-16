package com.kdd.demo.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "iris")
public class IrisSample {
    /**
     * 数据库中一条 Iris 花样本记录的实体映射。
     *
     * 字段名对应 MySQL 初始化脚本或数据源中的列名。
     * 服务层返回 JSON 前，会把实体转换成前端更容易使用的 snake_case 字段。
     */
    @Id
    private Integer id;

    @Column(name = "SepL")
    private Double sepalLength;

    @Column(name = "SepW")
    private Double sepalWidth;

    @Column(name = "PetL")
    private Double petalLength;

    @Column(name = "PetW")
    private Double petalWidth;

    @Column(name = "Species")
    private String species;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getSepalLength() {
        return sepalLength;
    }

    public void setSepalLength(Double sepalLength) {
        this.sepalLength = sepalLength;
    }

    public Double getSepalWidth() {
        return sepalWidth;
    }

    public void setSepalWidth(Double sepalWidth) {
        this.sepalWidth = sepalWidth;
    }

    public Double getPetalLength() {
        return petalLength;
    }

    public void setPetalLength(Double petalLength) {
        this.petalLength = petalLength;
    }

    public Double getPetalWidth() {
        return petalWidth;
    }

    public void setPetalWidth(Double petalWidth) {
        this.petalWidth = petalWidth;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }
}
