# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.7.12-log)
# Database: bigData
# Generation Time: 2021-07-05 05:24:47 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table iris
# ------------------------------------------------------------

DROP TABLE IF EXISTS `iris`;

CREATE TABLE `iris` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `SepL` float DEFAULT NULL,
  `SepW` float DEFAULT NULL,
  `PetL` float DEFAULT NULL,
  `PetW` float DEFAULT NULL,
  `Species` char(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `iris` WRITE;
/*!40000 ALTER TABLE `iris` DISABLE KEYS */;

INSERT INTO `iris` (`id`, `SepL`, `SepW`, `PetL`, `PetW`, `Species`)
VALUES
	(1,3.5,5.1,0.2,1.4,'setosa'),
	(2,3,4.9,0.2,1.4,'setosa'),
	(3,3.2,4.7,0.2,1.3,'setosa'),
	(4,3.1,4.6,0.2,1.5,'setosa'),
	(5,3.6,5,0.2,1.4,'setosa'),
	(6,3.9,5.4,0.4,1.7,'setosa'),
	(7,3.4,4.6,0.3,1.4,'setosa'),
	(8,3.4,5,0.2,1.5,'setosa'),
	(9,2.9,4.4,0.2,1.4,'setosa'),
	(10,3.1,4.9,0.1,1.5,'setosa'),
	(11,3.7,5.4,0.2,1.5,'setosa'),
	(12,3.4,4.8,0.2,1.6,'setosa'),
	(13,3,4.8,0.1,1.4,'setosa'),
	(14,3,4.3,0.1,1.1,'setosa'),
	(15,4,5.8,0.2,1.2,'setosa'),
	(16,4.4,5.7,0.4,1.5,'setosa'),
	(17,3.9,5.4,0.4,1.3,'setosa'),
	(18,3.5,5.1,0.3,1.4,'setosa'),
	(19,3.8,5.7,0.3,1.7,'setosa'),
	(20,3.8,5.1,0.3,1.5,'setosa'),
	(21,3.4,5.4,0.2,1.7,'setosa'),
	(22,3.7,5.1,0.4,1.5,'setosa'),
	(23,3.6,4.6,0.2,1,'setosa'),
	(24,3.3,5.1,0.5,1.7,'setosa'),
	(25,3.4,4.8,0.2,1.9,'setosa'),
	(26,3,5,0.2,1.6,'setosa'),
	(27,3.4,5,0.4,1.6,'setosa'),
	(28,3.5,5.2,0.2,1.5,'setosa'),
	(29,3.4,5.2,0.2,1.4,'setosa'),
	(30,3.2,4.7,0.2,1.6,'setosa'),
	(31,3.1,4.8,0.2,1.6,'setosa'),
	(32,3.4,5.4,0.4,1.5,'setosa'),
	(33,4.1,5.2,0.1,1.5,'setosa'),
	(34,4.2,5.5,0.2,1.4,'setosa'),
	(35,3.1,4.9,0.2,1.5,'setosa'),
	(36,3.2,5,0.2,1.2,'setosa'),
	(37,3.5,5.5,0.2,1.3,'setosa'),
	(38,3.6,4.9,0.1,1.4,'setosa'),
	(39,3,4.4,0.2,1.3,'setosa'),
	(40,3.4,5.1,0.2,1.5,'setosa'),
	(41,3.5,5,0.3,1.3,'setosa'),
	(42,2.3,4.5,0.3,1.3,'setosa'),
	(43,3.2,4.4,0.2,1.3,'setosa'),
	(44,3.5,5,0.6,1.6,'setosa'),
	(45,3.8,5.1,0.4,1.9,'setosa'),
	(46,3,4.8,0.3,1.4,'setosa'),
	(47,3.8,5.1,0.2,1.6,'setosa'),
	(48,3.2,4.6,0.2,1.4,'setosa'),
	(49,3.7,5.3,0.2,1.5,'setosa'),
	(50,3.3,5,0.2,1.4,'setosa'),
	(51,3.2,7,1.4,4.7,'versicolor'),
	(52,3.2,6.4,1.5,4.5,'versicolor'),
	(53,3.1,6.9,1.5,4.9,'versicolor'),
	(54,2.3,5.5,1.3,4,'versicolor'),
	(55,2.8,6.5,1.5,4.6,'versicolor'),
	(56,2.8,5.7,1.3,4.5,'versicolor'),
	(57,3.3,6.3,1.6,4.7,'versicolor'),
	(58,2.4,4.9,1,3.3,'versicolor'),
	(59,2.9,6.6,1.3,4.6,'versicolor'),
	(60,2.7,5.2,1.4,3.9,'versicolor'),
	(61,2,5,1,3.5,'versicolor'),
	(62,3,5.9,1.5,4.2,'versicolor'),
	(63,2.2,6,1,4,'versicolor'),
	(64,2.9,6.1,1.4,4.7,'versicolor'),
	(65,2.9,5.6,1.3,3.6,'versicolor'),
	(66,3.1,6.7,1.4,4.4,'versicolor'),
	(67,3,5.6,1.5,4.5,'versicolor'),
	(68,2.7,5.8,1,4.1,'versicolor'),
	(69,2.2,6.2,1.5,4.5,'versicolor'),
	(70,2.5,5.6,1.1,3.9,'versicolor'),
	(71,3.2,5.9,1.8,4.8,'versicolor'),
	(72,2.8,6.1,1.3,4,'versicolor'),
	(73,2.5,6.3,1.5,4.9,'versicolor'),
	(74,2.8,6.1,1.2,4.7,'versicolor'),
	(75,2.9,6.4,1.3,4.3,'versicolor'),
	(76,3,6.6,1.4,4.4,'versicolor'),
	(77,2.8,6.8,1.4,4.8,'versicolor'),
	(78,3,6.7,1.7,5,'versicolor'),
	(79,2.9,6,1.5,4.5,'versicolor'),
	(80,2.6,5.7,1,3.5,'versicolor'),
	(81,2.4,5.5,1.1,3.8,'versicolor'),
	(82,2.4,5.5,1,3.7,'versicolor'),
	(83,2.7,5.8,1.2,3.9,'versicolor'),
	(84,2.7,6,1.6,5.1,'versicolor'),
	(85,3,5.4,1.5,4.5,'versicolor'),
	(86,3.4,6,1.6,4.5,'versicolor'),
	(87,3.1,6.7,1.5,4.7,'versicolor'),
	(88,2.3,6.3,1.3,4.4,'versicolor'),
	(89,3,5.6,1.3,4.1,'versicolor'),
	(90,2.5,5.5,1.3,4,'versicolor'),
	(91,2.6,5.5,1.2,4.4,'versicolor'),
	(92,3,6.1,1.4,4.6,'versicolor'),
	(93,2.6,5.8,1.2,4,'versicolor'),
	(94,2.3,5,1,3.3,'versicolor'),
	(95,2.7,5.6,1.3,4.2,'versicolor'),
	(96,3,5.7,1.2,4.2,'versicolor'),
	(97,2.9,5.7,1.3,4.2,'versicolor'),
	(98,2.9,6.2,1.3,4.3,'versicolor'),
	(99,2.5,5.1,1.1,3,'versicolor'),
	(100,2.8,5.7,1.3,4.1,'versicolor'),
	(101,3.3,6.3,2.5,6,'virginica'),
	(102,2.7,5.8,1.9,5.1,'virginica'),
	(103,3,7.1,2.1,5.9,'virginica'),
	(104,2.9,6.3,1.8,5.6,'virginica'),
	(105,3,6.5,2.2,5.8,'virginica'),
	(106,3,7.6,2.1,6.6,'virginica'),
	(107,2.5,4.9,1.7,4.5,'virginica'),
	(108,2.9,7.3,1.8,6.3,'virginica'),
	(109,2.5,6.7,1.8,5.8,'virginica'),
	(110,3.6,7.2,2.5,6.1,'virginica'),
	(111,3.2,6.5,2,5.1,'virginica'),
	(112,2.7,6.4,1.9,5.3,'virginica'),
	(113,3,6.8,2.1,5.5,'virginica'),
	(114,2.5,5.7,2,5,'virginica'),
	(115,2.8,5.8,2.4,5.1,'virginica'),
	(116,3.2,6.4,2.3,5.3,'virginica'),
	(117,3,6.5,1.8,5.5,'virginica'),
	(118,3.8,7.7,2.2,6.7,'virginica'),
	(119,2.6,7.7,2.3,6.9,'virginica'),
	(120,2.2,6,1.5,5,'virginica'),
	(121,3.2,6.9,2.3,5.7,'virginica'),
	(122,2.8,5.6,2,4.9,'virginica'),
	(123,2.8,7.7,2,6.7,'virginica'),
	(124,2.7,6.3,1.8,4.9,'virginica'),
	(125,3.3,6.7,2.1,5.7,'virginica'),
	(126,3.2,7.2,1.8,6,'virginica'),
	(127,2.8,6.2,1.8,4.8,'virginica'),
	(128,3,6.1,1.8,4.9,'virginica'),
	(129,2.8,6.4,2.1,5.6,'virginica'),
	(130,3,7.2,1.6,5.8,'virginica'),
	(131,2.8,7.4,1.9,6.1,'virginica'),
	(132,3.8,7.9,2,6.4,'virginica'),
	(133,2.8,6.4,2.2,5.6,'virginica'),
	(134,2.8,6.3,1.5,5.1,'virginica'),
	(135,2.6,6.1,1.4,5.6,'virginica'),
	(136,3,7.7,2.3,6.1,'virginica'),
	(137,3.4,6.3,2.4,5.6,'virginica'),
	(138,3.1,6.4,1.8,5.5,'virginica'),
	(139,3,6,1.8,4.8,'virginica'),
	(140,3.1,6.9,2.1,5.4,'virginica'),
	(141,3.1,6.7,2.4,5.6,'virginica'),
	(142,3.1,6.9,2.3,5.1,'virginica'),
	(143,2.7,5.8,1.9,5.1,'virginica'),
	(144,3.2,6.8,2.3,5.9,'virginica'),
	(145,3.3,6.7,2.5,5.7,'virginica'),
	(146,3,6.7,2.3,5.2,'virginica'),
	(147,2.5,6.3,1.9,5,'virginica'),
	(148,3,6.5,2,5.2,'virginica'),
	(149,3.4,6.2,2.3,5.4,'virginica'),
	(150,3,5.9,1.8,5.1,'virginica');

/*!40000 ALTER TABLE `iris` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table orderSum
# ------------------------------------------------------------

DROP TABLE IF EXISTS `orderSum`;

CREATE TABLE `orderSum` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `orderNum` int(11) DEFAULT NULL,
  `city` char(20) CHARACTER SET utf8 DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `orderSum` WRITE;
/*!40000 ALTER TABLE `orderSum` DISABLE KEYS */;

INSERT INTO `orderSum` (`id`, `orderNum`, `city`)
VALUES
	(1,167,'NanYang'),
	(2,67,'Zhoukou'),
	(3,123,'LuoHe'),
	(4,55,'ZhengZhou'),
	(5,98,'XiXia');

/*!40000 ALTER TABLE `orderSum` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table positionInfo
# ------------------------------------------------------------

DROP TABLE IF EXISTS `positionInfo`;

CREATE TABLE `positionInfo` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `post` char(20) CHARACTER SET utf8 DEFAULT NULL,
  `level` char(20) CHARACTER SET utf8 DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

LOCK TABLES `positionInfo` WRITE;
/*!40000 ALTER TABLE `positionInfo` DISABLE KEYS */;

INSERT INTO `positionInfo` (`id`, `post`, `level`)
VALUES
	(1,'YunWei','professor'),
	(2,'ChanPin','primary');

/*!40000 ALTER TABLE `positionInfo` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
