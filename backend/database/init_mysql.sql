-- 如果 bigData 数据库不存在，则创建数据库。
CREATE DATABASE IF NOT EXISTS bigData DEFAULT CHARACTER SET utf8mb4;

-- 切换到 bigData 数据库。
USE bigData;

-- 如果已有 iris 表，先删除，避免重复初始化时报错。
DROP TABLE IF EXISTS iris;

-- 创建 iris 数据表，字段命名参考课程示例数据。
CREATE TABLE iris (
  id INT PRIMARY KEY AUTO_INCREMENT,
  SepL FLOAT,
  SepW FLOAT,
  PetL FLOAT,
  PetW FLOAT,
  Species VARCHAR(30)
) DEFAULT CHARSET=utf8mb4;

-- 插入 iris 示例数据，用于聚类、分类和回归。
INSERT INTO iris (id, SepL, SepW, PetL, PetW, Species) VALUES
(1,3.5,5.1,0.2,1.4,'setosa'),
(2,3.0,4.9,0.2,1.4,'setosa'),
(3,3.2,4.7,0.2,1.3,'setosa'),
(4,3.6,5.0,0.2,1.4,'setosa'),
(5,3.9,5.4,0.4,1.7,'setosa'),
(6,2.7,5.8,1.0,4.1,'versicolor'),
(7,2.7,6.0,1.6,5.1,'versicolor'),
(8,2.9,6.1,1.4,4.7,'versicolor'),
(9,3.1,6.7,1.4,4.4,'versicolor'),
(10,3.2,6.4,1.5,4.5,'versicolor'),
(11,3.0,6.5,2.2,5.8,'virginica'),
(12,3.0,7.6,2.1,6.6,'virginica'),
(13,2.9,7.3,1.8,6.3,'virginica'),
(14,3.3,6.7,2.5,5.7,'virginica'),
(15,3.6,7.2,2.5,6.1,'virginica');

-- 如果已有 transaction_items 表，先删除。
DROP TABLE IF EXISTS transaction_items;

-- 创建交易明细表，一行表示某个交易中出现的一个商品。
CREATE TABLE transaction_items (
  id INT PRIMARY KEY AUTO_INCREMENT,
  transaction_id INT NOT NULL,
  item_name VARCHAR(50) NOT NULL
) DEFAULT CHARSET=utf8mb4;

-- 插入交易示例数据，用于关联规则分析。
INSERT INTO transaction_items (transaction_id, item_name) VALUES
(1,'milk'),(1,'bread'),(1,'eggs'),
(2,'milk'),(2,'bread'),
(3,'bread'),(3,'butter'),
(4,'milk'),(4,'eggs'),
(5,'bread'),(5,'eggs'),(5,'butter'),
(6,'milk'),(6,'bread'),(6,'butter'),
(7,'milk'),(7,'bread'),(7,'eggs'),(7,'butter'),
(8,'bread'),(8,'eggs'),
(9,'milk'),(9,'butter'),
(10,'milk'),(10,'bread'),(10,'eggs');
