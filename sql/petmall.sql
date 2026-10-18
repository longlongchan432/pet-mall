-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: pet_mall
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cart`
--

DROP TABLE IF EXISTS `cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商品名称',
  `product_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商品图片',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `merchant_id` bigint DEFAULT NULL COMMENT '商家ID',
  `merchant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商家名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='购物车表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart`
--

LOCK TABLES `cart` WRITE;
/*!40000 ALTER TABLE `cart` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'宠物食品','各种宠物食品，零食，狗粮，猫粮等',0,1,'2026-01-18 14:06:21','2026-06-07 13:39:54'),(2,'宠物用品','宠物日常用品',0,2,'2026-01-18 14:06:21','2026-01-18 14:06:21'),(3,'宠物玩具','宠物玩具',0,3,'2026-01-18 14:06:21','2026-01-18 14:06:21'),(4,'宠物医疗','宠物医疗用品',0,4,'2026-01-18 14:06:21','2026-01-18 14:06:21'),(5,'狗粮','狗狗专用粮食',1,1,'2026-01-18 14:06:21','2026-01-18 14:06:21'),(6,'猫粮','猫咪专用粮食',1,2,'2026-01-18 14:06:21','2026-01-18 14:06:21'),(7,'狗窝','狗狗专用窝',2,1,'2026-01-18 14:06:21','2026-01-18 14:06:21'),(8,'猫砂','猫咪专用猫砂',2,2,'2026-01-18 14:06:21','2026-01-18 14:06:21');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order`
--

DROP TABLE IF EXISTS `order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '总金额',
  `status` int DEFAULT '0' COMMENT '状态：0-待支付 1-待发货 2-待收货 3-已完成 4-已取消 5-退款中 6-已退款',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '收货地址',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '收货人电话',
  `logistics_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '物流单号',
  `logistics_company` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '物流公司',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '收货时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `order_no` (`order_no`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_merchant_id` (`merchant_id`) USING BTREE,
  KEY `idx_order_no` (`order_no`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order`
--

LOCK TABLES `order` WRITE;
/*!40000 ALTER TABLE `order` DISABLE KEYS */;
INSERT INTO `order` VALUES (3,'PT17687200864938AD228',3,2,1495.00,1,'河南省郑州市','张三','17388791',NULL,NULL,'2026-01-18 15:08:06','2026-01-18 15:08:07',NULL,NULL,'2026-01-18 15:08:07'),(4,'PT1768721590418C08769',4,2,318.00,3,'河南省郑州市','张三','1789737444','796456417445515','顺丰','2026-01-18 15:33:10','2026-01-18 15:33:11','2026-01-18 15:34:03','2026-01-18 15:34:12','2026-01-18 15:34:12'),(5,'PT1770078683322E5CCC4',1,2,50.00,3,'测试','张三','1738798754','112212123232323342','中通','2026-02-03 08:31:23','2026-02-03 08:31:24','2026-02-03 08:33:22','2026-02-03 08:33:30','2026-02-03 08:33:30'),(6,'PT1770079019882EECC8E',1,2,50.00,1,'测试','测试','4651231321',NULL,NULL,'2026-02-03 08:37:00','2026-02-03 08:37:00',NULL,NULL,'2026-02-03 08:37:00'),(7,'PT177019516977521A374',7,2,49.00,3,'信阳市吴河乡万安村','赵四','15436755478','123423513532','顺丰','2026-02-04 16:52:50','2026-02-04 16:52:50','2026-02-04 16:53:53','2026-02-04 16:54:01','2026-02-04 16:54:01'),(8,'PT1770211902961AF2234',7,2,159.00,3,'123','赵四','17685994569','12342qrtwaetr4356457','中通','2026-02-04 21:31:43','2026-02-04 21:31:43','2026-02-04 21:32:38','2026-02-04 21:32:47','2026-02-04 21:32:47'),(9,'PT17722662537598A0A64',7,2,384.00,3,'河南牧业经济学院英才校区','赵四','18790416261','1231hasdfyiu398','顺丰','2026-02-28 16:10:54','2026-02-28 16:10:54','2026-02-28 16:11:25','2026-02-28 16:11:36','2026-02-28 16:11:36'),(10,'PT1772269274625686B16',7,2,164.30,3,'河南牧业经济学院英才校区','123','13800000000','','','2026-02-28 17:01:15','2026-02-28 17:01:15','2026-02-28 17:04:18','2026-03-05 22:18:20','2026-03-05 22:18:20'),(11,'PT17727813099256DCE00',7,2,164.00,3,'信阳市吴河乡万安村','赵四','18790416263','123239546978ashjosadfi','京东','2026-03-06 15:15:10','2026-03-06 15:15:10','2026-03-06 15:16:02','2026-03-06 15:16:16','2026-03-06 15:16:16'),(12,'PT17727853799514E248C',7,2,16.80,3,'上海市浦东新区虹桥村','赵四','18790416263','1892472903523189sxahld','顺丰','2026-03-06 16:23:00','2026-03-06 16:23:00','2026-03-06 16:24:20','2026-03-06 16:24:27','2026-03-06 16:24:27'),(13,'PT17742649437468886D2',7,2,49.90,3,'上海路91号','赵四','19878699987','91919191','韵达','2026-03-23 19:22:24','2026-03-23 19:22:24','2026-03-23 19:22:47','2026-03-23 19:22:55','2026-05-14 13:01:25'),(14,'PT1774265309527F76609',7,2,18.80,6,'上海路91号','赵四','19191919191','917813','顺丰','2026-03-23 19:28:30','2026-03-23 19:28:30','2026-03-23 19:28:59','2026-03-23 19:29:11','2026-03-29 16:12:33'),(15,'PT177426610080254E6B4',7,2,18.80,6,'上海路91号','赵四','917813','918765','顺丰','2026-03-23 19:41:41','2026-03-23 19:41:41','2026-03-23 19:42:16','2026-03-23 19:42:26','2026-03-29 16:11:39'),(16,'PT1774266578234436AA9',7,2,18.80,6,'上海市','赵四','19187136969','91781369','韵达','2026-03-23 19:49:38','2026-03-23 19:49:39','2026-03-23 19:50:03','2026-03-23 19:50:10','2026-03-29 16:00:21'),(17,'PT1774268461154CB7C86',7,2,25.90,6,'上海市91路','赵四','18790416261','123','123','2026-03-23 20:21:01','2026-03-23 20:21:01','2026-03-23 20:21:35','2026-03-23 20:21:44','2026-03-29 15:59:30'),(18,'PT17746861776783C7E9A',7,2,16.80,1,'黄山市黄山路91号','赵四','19945611619',NULL,NULL,'2026-03-28 16:22:58','2026-03-28 16:22:58',NULL,NULL,'2026-03-28 16:22:58'),(19,'PT1774769421286967042',7,2,177.80,6,'上海市上海路91号','赵四','18976543897','测试','测试','2026-03-29 15:30:21','2026-03-29 15:30:22','2026-03-29 18:58:11','2026-03-29 19:01:52','2026-04-10 21:23:57'),(20,'PT17758253319829AE677',7,2,803.50,1,'123','赵四','789113',NULL,NULL,'2026-04-10 20:48:52','2026-04-10 20:48:52',NULL,NULL,'2026-04-10 20:48:52'),(21,'PT17758255343500C5594',7,2,586.00,1,'123','赵四','781391',NULL,NULL,'2026-04-10 20:52:14','2026-04-10 20:52:15',NULL,NULL,'2026-04-10 20:52:15'),(22,'PT177582584924298554A',7,2,145.50,1,'91路','赵四','19987697549',NULL,NULL,'2026-04-10 20:57:29','2026-04-10 20:57:30',NULL,NULL,'2026-04-10 20:57:30'),(23,'PT1775826381614A71D0C',7,2,29.90,1,'91路','赵四','917813',NULL,NULL,'2026-04-10 21:06:22','2026-04-10 21:06:30',NULL,NULL,'2026-04-10 21:06:30'),(24,'PT1775826910359BBCEEB',7,2,46.00,1,'91路','赵四','917813',NULL,NULL,'2026-04-10 21:15:10','2026-04-10 21:19:30',NULL,NULL,'2026-04-10 21:19:30'),(25,'PT1775827776942066F8E',7,2,271.00,4,'123','赵四','123',NULL,NULL,'2026-04-10 21:29:37',NULL,NULL,NULL,'2026-04-10 21:29:59'),(26,'PT1775827780949198EA5',7,2,271.00,4,'123','赵四','123',NULL,NULL,'2026-04-10 21:29:41',NULL,NULL,NULL,'2026-04-10 21:29:55'),(33,'PT17764101011204F3A88',12,2,16.00,3,'123','123','13800000000','','','2026-04-17 15:15:01','2026-04-17 15:15:15','2026-04-17 15:34:07','2026-04-17 15:34:21','2026-04-17 15:43:54'),(34,'PT17764175461649A1248',12,2,17.80,2,'123','123','123','','','2026-04-17 17:19:06','2026-04-17 17:19:10','2026-04-17 17:19:22',NULL,'2026-04-17 17:19:22'),(35,'PT1779189320646D68126',7,2,90.80,2,'测试','测试','测试','测试','测试','2026-05-19 19:15:21','2026-05-19 19:15:25','2026-05-19 19:16:47',NULL,'2026-05-19 19:16:47'),(36,'PT17791896290826F4845',7,2,161.50,2,'测试','测试','测试','测试','测试','2026-05-19 19:20:29','2026-05-19 19:20:33','2026-05-19 19:21:41',NULL,'2026-05-19 19:21:41'),(37,'PT17791898161847A1CD5',7,2,135.00,3,'测试','测试','测试','测试','测试','2026-05-19 19:23:36','2026-05-19 19:23:40','2026-05-19 19:24:38','2026-05-19 19:26:00','2026-05-19 19:26:00');
/*!40000 ALTER TABLE `order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_item`
--

DROP TABLE IF EXISTS `order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商品名称',
  `product_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商品图片',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `quantity` int DEFAULT NULL COMMENT '数量',
  `subtotal` decimal(10,2) DEFAULT NULL COMMENT '小计',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE,
  KEY `idx_order_no` (`order_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_item`
--

LOCK TABLES `order_item` WRITE;
/*!40000 ALTER TABLE `order_item` DISABLE KEYS */;
INSERT INTO `order_item` VALUES (1,3,'PT17687200864938AD228',1,'优质狗粮 10kg','/upload/d5f8ff6e-87d6-4bce-b6cf-5ed9f108bff0.jpg',299.00,5,1495.00,'2026-01-18 15:08:06'),(2,4,'PT1768721590418C08769',3,'舒适狗窝','/upload/fc31b948-a7ed-4069-a01e-efd4c7f69782.jpg',159.00,2,318.00,'2026-01-18 15:33:10'),(3,5,'PT1770078683322E5CCC4',5,'测试','/upload/cfff5808-6ac9-4335-a028-446c9c0a4761.jpg',50.00,1,50.00,'2026-02-03 08:31:23'),(4,6,'PT1770079019882EECC8E',5,'测试','/upload/cfff5808-6ac9-4335-a028-446c9c0a4761.jpg',50.00,1,50.00,'2026-02-03 08:37:00'),(5,7,'PT177019516977521A374',4,'膨润土猫砂 10L','/upload/039bd1c0-b5b5-4fb9-9fb1-1475ca0ac5dc.jpg',49.00,1,49.00,'2026-02-04 16:52:50'),(6,8,'PT1770211902961AF2234',3,'舒适狗窝','/upload/fc31b948-a7ed-4069-a01e-efd4c7f69782.jpg',159.00,1,159.00,'2026-02-04 21:31:43'),(7,9,'PT17722662537598A0A64',37,'皇家猫粮 美短成猫粮 ASA31 通用粮 12月以上 2KG','/upload/abcbe73c-d119-433c-bc41-b4f8d398f30e.png',164.00,2,328.00,'2026-02-28 16:10:54'),(8,9,'PT17722662537598A0A64',32,'猫狗泪痕口服液','/upload/9842cfc8-e17b-4b91-8bf3-4af94f7410f1.png',56.00,1,56.00,'2026-02-28 16:10:54'),(9,10,'PT1772269274625686B16',39,'全能优选成猫粮肠道舒适型调理粮2Kg猫咪主食粮','/upload/b4cb33e0-92db-493f-91d3-3adc8242a1f5.png',145.50,1,145.50,'2026-02-28 17:01:15'),(10,10,'PT1772269274625686B16',40,'猫砂膨润土无尘除臭','/upload/9de22485-7ed6-49d8-9523-a835b1ca0b25.png',18.80,1,18.80,'2026-02-28 17:01:15'),(11,11,'PT17727813099256DCE00',37,'皇家猫粮 美短成猫粮 ASA31 通用粮 12月以上 2KG','/upload/abcbe73c-d119-433c-bc41-b4f8d398f30e.png',164.00,1,164.00,'2026-03-06 15:15:10'),(12,12,'PT17727853799514E248C',42,'无花果豆腐混合猫砂','/upload/62546362-e1b4-45e3-8da9-85cac28f6f0e.png',16.80,1,16.80,'2026-03-06 16:23:00'),(13,13,'PT17742649437468886D2',38,'鲜肉双拼猫粮','/upload/f9276034-fa3c-4d3f-8f3b-6f0c91573866.png',49.90,1,49.90,'2026-03-23 19:22:24'),(14,14,'PT1774265309527F76609',40,'猫砂膨润土无尘除臭','/upload/9de22485-7ed6-49d8-9523-a835b1ca0b25.png',18.80,1,18.80,'2026-03-23 19:28:30'),(15,15,'PT177426610080254E6B4',40,'猫砂膨润土无尘除臭','/upload/9de22485-7ed6-49d8-9523-a835b1ca0b25.png',18.80,1,18.80,'2026-03-23 19:41:41'),(16,16,'PT1774266578234436AA9',40,'猫砂膨润土无尘除臭','/upload/9de22485-7ed6-49d8-9523-a835b1ca0b25.png',18.80,1,18.80,'2026-03-23 19:49:38'),(17,17,'PT1774268461154CB7C86',41,'木薯猫砂不沾底强吸水','/upload/29f69f61-f6ad-46f1-bcc4-1c43daf730c2.png',25.90,1,25.90,'2026-03-23 20:21:01'),(18,18,'PT17746861776783C7E9A',42,'无花果豆腐混合猫砂','/upload/62546362-e1b4-45e3-8da9-85cac28f6f0e.png',16.80,1,16.80,'2026-03-28 16:22:58'),(19,19,'PT1774769421286967042',3,'舒适狗窝','/upload/35146308-725b-4893-ad7a-a3effc2a7889.png',159.00,1,159.00,'2026-03-29 15:30:21'),(20,19,'PT1774769421286967042',40,'猫砂膨润土无尘除臭','/upload/9de22485-7ed6-49d8-9523-a835b1ca0b25.png',18.80,1,18.80,'2026-03-29 15:30:21'),(21,20,'PT17758253319829AE677',8,'大中型犬成犬狗粮15kg','/upload/1daa8535-84a9-49db-a3b7-20a3aa76ddf9.jpg',586.00,1,586.00,'2026-04-10 20:48:52'),(22,20,'PT17758253319829AE677',39,'全能优选成猫粮肠道舒适型调理粮2Kg猫咪主食粮','/upload/b4cb33e0-92db-493f-91d3-3adc8242a1f5.png',145.50,1,145.50,'2026-04-10 20:48:52'),(23,20,'PT17758253319829AE677',19,'宠物磨牙棒','/upload/880f880a-d70e-414d-9ea0-e9739de2ab79.png',72.00,1,72.00,'2026-04-10 20:48:52'),(24,21,'PT17758255343500C5594',8,'大中型犬成犬狗粮15kg','/upload/1daa8535-84a9-49db-a3b7-20a3aa76ddf9.jpg',586.00,1,586.00,'2026-04-10 20:52:14'),(25,22,'PT177582584924298554A',39,'全能优选成猫粮肠道舒适型调理粮2Kg猫咪主食粮','/upload/b4cb33e0-92db-493f-91d3-3adc8242a1f5.png',145.50,1,145.50,'2026-04-10 20:57:29'),(26,23,'PT1775826381614A71D0C',35,'猫咪洗耳液','/upload/71d049c4-ff1a-463e-ab7a-1669b1ee3007.png',29.90,1,29.90,'2026-04-10 21:06:22'),(27,24,'PT1775826910359BBCEEB',10,'德牧专用狗粮','/upload/3ece7fae-fe6d-44de-b8ae-fc256f5d6d3b.jpg',46.00,1,46.00,'2026-04-10 21:15:10'),(28,25,'PT1775827776942066F8E',2,'进口猫粮 5kg','/upload/67070844-b180-425b-a46c-f0262329ad5e.png',199.00,1,199.00,'2026-04-10 21:29:37'),(29,25,'PT1775827776942066F8E',19,'宠物磨牙棒','/upload/880f880a-d70e-414d-9ea0-e9739de2ab79.png',72.00,1,72.00,'2026-04-10 21:29:37'),(30,26,'PT1775827780949198EA5',2,'进口猫粮 5kg','/upload/67070844-b180-425b-a46c-f0262329ad5e.png',199.00,1,199.00,'2026-04-10 21:29:41'),(31,26,'PT1775827780949198EA5',19,'宠物磨牙棒','/upload/880f880a-d70e-414d-9ea0-e9739de2ab79.png',72.00,1,72.00,'2026-04-10 21:29:41'),(32,27,'PT1775827847074F0FB10',29,'猫抓柱仙人掌立式猫抓','/upload/c7ad3d6a-c00d-476f-871b-ce8cd1821cc5.png',40.00,1,40.00,'2026-04-10 21:30:47'),(33,28,'PT177640943745839AB9A',40,'猫砂膨润土无尘除臭','/upload/9de22485-7ed6-49d8-9523-a835b1ca0b25.png',18.80,1,18.80,'2026-04-17 15:03:57'),(34,28,'PT177640943745839AB9A',7,'自制狗粮','/upload/d76bb68d-2478-433b-aab2-8eab5bfd31d7.jpg',48.00,1,48.00,'2026-04-17 15:03:57'),(35,29,'PT177640956128614F3D2',30,'小型犬猫咪碗','/upload/d6f721fc-6890-465a-acf3-de75b7e1152c.png',16.00,1,16.00,'2026-04-17 15:06:01'),(36,30,'PT17764095641920A1A05',30,'小型犬猫咪碗','/upload/d6f721fc-6890-465a-acf3-de75b7e1152c.png',16.00,1,16.00,'2026-04-17 15:06:04'),(37,31,'PT177640956673796F217',30,'小型犬猫咪碗','/upload/d6f721fc-6890-465a-acf3-de75b7e1152c.png',16.00,1,16.00,'2026-04-17 15:06:07'),(38,32,'PT1776409757321403C99',29,'猫抓柱仙人掌立式猫抓','/upload/c7ad3d6a-c00d-476f-871b-ce8cd1821cc5.png',40.00,1,40.00,'2026-04-17 15:09:17'),(39,33,'PT17764101011204F3A88',30,'小型犬猫咪碗','/upload/d6f721fc-6890-465a-acf3-de75b7e1152c.png',16.00,1,16.00,'2026-04-17 15:15:01'),(40,34,'PT17764175461649A1248',33,'狗狗玩具耐咬磨牙球','/upload/49a4ec96-e5ed-4a99-9a53-8b43f6cc26e0.png',17.80,1,17.80,'2026-04-17 17:19:06'),(41,35,'PT1779189320646D68126',32,'猫狗泪痕口服液','/upload/9842cfc8-e17b-4b91-8bf3-4af94f7410f1.png',56.00,1,56.00,'2026-05-19 19:15:21'),(42,35,'PT1779189320646D68126',26,'猫罐头24罐整箱85g罐12罐营养猫咪主食','/upload/cfc1d6ed-c0db-46da-8198-b30ee1158fac.png',34.80,1,34.80,'2026-05-19 19:15:21'),(43,36,'PT17791896290826F4845',39,'全能优选成猫粮肠道舒适型调理粮2Kg猫咪主食粮','/upload/b4cb33e0-92db-493f-91d3-3adc8242a1f5.png',145.50,1,145.50,'2026-05-19 19:20:29'),(44,36,'PT17791896290826F4845',30,'小型犬猫咪碗','/upload/d6f721fc-6890-465a-acf3-de75b7e1152c.png',16.00,1,16.00,'2026-05-19 19:20:29'),(45,37,'PT17791898161847A1CD5',24,'泰国进口 猫罐头85g*24罐 (汤汁型)','/upload/7b37934d-491d-40c1-a9d0-c30bb59a6e8f.png',95.00,1,95.00,'2026-05-19 19:23:36'),(46,37,'PT17791898161847A1CD5',29,'猫抓柱仙人掌立式猫抓','/upload/c7ad3d6a-c00d-476f-871b-ce8cd1821cc5.png',40.00,1,40.00,'2026-05-19 19:23:36');
/*!40000 ALTER TABLE `order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `payment_method` int DEFAULT NULL COMMENT '支付方式：1-支付宝 2-微信 3-银行卡',
  `status` int DEFAULT '0' COMMENT '状态：0-未支付 1-已支付 2-已退款',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `payment_no` (`payment_no`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE,
  KEY `idx_payment_no` (`payment_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='支付表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
INSERT INTO `payment` VALUES (1,'PAY1768720086869',3,'PT17687200864938AD228',1495.00,1,1,'2026-01-18 15:08:07','2026-01-18 15:08:07'),(2,'PAY1768721590766',4,'PT1768721590418C08769',318.00,1,1,'2026-01-18 15:33:11','2026-01-18 15:33:11'),(3,'PAY1770078683538',5,'PT1770078683322E5CCC4',50.00,4,1,'2026-02-03 08:31:24','2026-02-03 08:31:24'),(4,'PAY1770079020146',6,'PT1770079019882EECC8E',50.00,1,1,'2026-02-03 08:37:00','2026-02-03 08:37:00'),(5,'PAY1770195170042',7,'PT177019516977521A374',49.00,4,1,'2026-02-04 16:52:50','2026-02-04 16:52:50'),(6,'PAY1770211903303',8,'PT1770211902961AF2234',159.00,4,1,'2026-02-04 21:31:43','2026-02-04 21:31:43'),(7,'PAY1772266254108',9,'PT17722662537598A0A64',384.00,4,1,'2026-02-28 16:10:54','2026-02-28 16:10:54'),(8,'PAY1772269275009',10,'PT1772269274625686B16',164.30,4,1,'2026-02-28 17:01:15','2026-02-28 17:01:15'),(9,'PAY1772781310274',11,'PT17727813099256DCE00',164.00,4,1,'2026-03-06 15:15:10','2026-03-06 15:15:10'),(10,'PAY1772785380184',12,'PT17727853799514E248C',16.80,4,1,'2026-03-06 16:23:00','2026-03-06 16:23:00'),(11,'PAY1774264944003',13,'PT17742649437468886D2',49.90,4,1,'2026-03-23 19:22:24','2026-03-23 19:22:24'),(12,'PAY1774265309803',14,'PT1774265309527F76609',18.80,4,2,'2026-03-23 19:28:30','2026-03-23 19:28:30'),(13,'PAY1774266101078',15,'PT177426610080254E6B4',18.80,4,2,'2026-03-23 19:41:41','2026-03-23 19:41:41'),(14,'PAY1774266578517',16,'PT1774266578234436AA9',18.80,4,2,'2026-03-23 19:49:39','2026-03-23 19:49:39'),(15,'PAY1774268461424',17,'PT1774268461154CB7C86',25.90,4,2,'2026-03-23 20:21:01','2026-03-23 20:21:01'),(16,'PAY1774686177883',18,'PT17746861776783C7E9A',16.80,4,1,'2026-03-28 16:22:58','2026-03-28 16:22:58'),(17,'PAY1774769421531',19,'PT1774769421286967042',177.80,4,2,'2026-03-29 15:30:22','2026-03-29 15:30:22'),(18,'PAY1775825332248',20,'PT17758253319829AE677',803.50,4,1,'2026-04-10 20:48:52','2026-04-10 20:48:52'),(19,'PAY1775825534700',21,'PT17758255343500C5594',586.00,4,1,'2026-04-10 20:52:15','2026-04-10 20:52:15'),(20,'PAY1775825849510',22,'PT177582584924298554A',145.50,4,1,'2026-04-10 20:57:30','2026-04-10 20:57:30'),(21,'PAY1775826390018',23,'PT1775826381614A71D0C',29.90,4,1,'2026-04-10 21:06:30','2026-04-10 21:06:30'),(22,'PAY1775827170166',24,'PT1775826910359BBCEEB',46.00,4,1,'2026-04-10 21:19:30','2026-04-10 21:19:30'),(23,'PAY1775827856258',27,'PT1775827847074F0FB10',40.00,4,2,'2026-04-10 21:30:56','2026-04-10 21:30:56'),(24,'PAY1776409470938',28,'PT177640943745839AB9A',66.80,4,1,'2026-04-17 15:04:31','2026-04-17 15:04:31'),(25,'PAY1776409609669',30,'PT17764095641920A1A05',16.00,4,1,'2026-04-17 15:06:50','2026-04-17 15:06:50'),(26,'PAY1776409768703',32,'PT1776409757321403C99',40.00,4,1,'2026-04-17 15:09:29','2026-04-17 15:09:29'),(27,'PAY1776410115187',33,'PT17764101011204F3A88',16.00,4,1,'2026-04-17 15:15:15','2026-04-17 15:15:15'),(28,'PAY1776417549577',34,'PT17764175461649A1248',17.80,4,1,'2026-04-17 17:19:10','2026-04-17 17:19:10'),(29,'PAY1779189324912',35,'PT1779189320646D68126',90.80,4,1,'2026-05-19 19:15:25','2026-05-19 19:15:25'),(30,'PAY1779189633281',36,'PT17791896290826F4845',161.50,4,1,'2026-05-19 19:20:33','2026-05-19 19:20:33'),(31,'PAY1779189820117',37,'PT17791898161847A1CD5',135.00,4,1,'2026-05-19 19:23:40','2026-05-19 19:23:40');
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `stock` int DEFAULT '0' COMMENT '库存',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商品图片',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `status` int DEFAULT '1' COMMENT '状态：0-下架 1-上架',
  `sales` int DEFAULT '0' COMMENT '销量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category_id` (`category_id`) USING BTREE,
  KEY `idx_merchant_id` (`merchant_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='商品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,'优质狗粮 10kg','营养均衡的优质狗粮，适合成年犬',299.00,95,'/upload/b631cece-76b2-4d1b-ae9f-d24b061542ee.png',5,2,1,0,'2026-01-18 14:06:21','2026-02-09 13:03:29'),(2,'进口猫粮 5kg','进口天然猫粮，无添加',199.00,80,'/upload/67070844-b180-425b-a46c-f0262329ad5e.png',6,2,1,0,'2026-01-18 14:06:21','2026-04-10 21:29:59'),(3,'舒适狗窝','柔软舒适的狗狗专用窝',159.00,47,'/upload/35146308-725b-4893-ad7a-a3effc2a7889.png',7,2,1,1,'2026-01-18 14:06:21','2026-04-10 21:23:56'),(4,'膨润土猫砂 10L','强力除臭，易结团',49.00,199,'/upload/7f60ccad-55de-4fcf-be03-352cf97f315a.png',8,2,1,0,'2026-01-18 14:06:21','2026-02-09 13:01:56'),(6,'小型犬狗粮','养肠护胃，助益免疫。',35.90,100,'/upload/4afd056e-e7bd-432c-a840-49f9ac553006.jpg',5,2,1,0,'2026-02-09 13:10:24','2026-02-09 13:10:24'),(7,'自制狗粮','营养均衡，健康放心。',48.00,99,'/upload/d76bb68d-2478-433b-aab2-8eab5bfd31d7.jpg',5,2,1,0,'2026-02-09 13:13:40','2026-04-17 15:03:57'),(8,'大中型犬成犬狗粮15kg','主打稳固免疫配方，含维生素C/E+活性益生菌，添加葡萄糖胺护关节。粗蛋白&ge;26%满足营养需求，颗粒适中适口性好，2.5kg小包装超保鲜。',586.00,98,'/upload/1daa8535-84a9-49db-a3b7-20a3aa76ddf9.jpg',5,2,1,0,'2026-02-09 13:22:37','2026-04-10 20:52:14'),(9,'进口冻干狗粮','进口狗粮 双拼冻干鸡肉小型犬粮10磅/4.5kg',233.00,100,'/upload/4c1e1d97-fb67-43f5-bdba-79acaef3f319.jpg',5,2,1,0,'2026-02-09 13:52:11','2026-02-09 13:52:11'),(10,'德牧专用狗粮','德国牧羊犬警犬幼犬幼崽成犬通用型天然5斤10斤装 鸡肉冻干肉松三拼粮5斤+钙片1瓶',46.00,99,'/upload/3ece7fae-fe6d-44de-b8ae-fc256f5d6d3b.jpg',5,2,1,0,'2026-02-09 14:30:30','2026-04-10 21:15:10'),(11,'博美专用狗粮2.5kg (5斤)','博美专用狗粮冻干通用型成犬幼犬补钙小型犬轻泪痕护胃 博美成犬专用(经典牛肉味)',51.00,100,'/upload/ccceeb89-9585-4b8f-8731-19ed5c845763.jpg',5,2,1,0,'2026-02-09 14:35:54','2026-02-28 16:22:00'),(12,'博美专用狗粮','博美专用狗粮冻干通用型成犬幼犬补钙小型犬轻泪痕护胃 博美成犬专用(经典牛肉味)',51.00,100,'/upload/37fb257c-2bbc-447c-a898-79542cddbc4d.jpg',1,2,0,0,'2026-02-09 14:37:18','2026-02-09 14:45:18'),(13,'德牧专用狗粮','警犬德国牧羊犬幼犬成犬成年通用型大型犬40斤装鸡肉冻干牛肉粒三拼粮40斤',138.00,100,'/upload/e7dcd436-9f9f-472d-bb40-f9965c1074cf.jpg',1,2,0,0,'2026-02-09 14:38:49','2026-02-09 14:45:17'),(14,'进口冻干狗粮','风干狗粮羊肉帝王鲑320g袋装 幼犬成犬中大型犬粮新西兰原装进口\n',134.28,100,'/upload/e3098823-55fc-453e-874b-3fc3a28a05cb.jpg',1,2,0,0,'2026-02-09 14:40:18','2026-02-09 14:45:16'),(15,'大中型犬成犬狗粮15kg','大中型犬成犬狗粮15kg 稳固免疫 全价狗粮',586.00,100,'/upload/1214af9c-c044-4a48-8e48-c412af96307d.jpg',1,2,0,0,'2026-02-09 14:42:05','2026-02-09 14:45:14'),(16,'自制狗粮','自制狗粮，健康卫生，放心可靠。',46.00,100,'/upload/276ee5b2-d90b-47e6-a93a-1cc79cc9532d.jpg',1,2,0,0,'2026-02-09 14:43:20','2026-02-09 14:45:13'),(17,'小型犬狗粮','',35.90,100,'/upload/5460f24a-6db1-4d50-8032-238ccf8ea8d0.jpg',1,2,0,0,'2026-02-09 14:44:13','2026-02-09 14:45:07'),(18,'边牧专用狗粮 中大型犬呵护骨骼钙磷配比全阶段10kg','',159.00,100,'/upload/1cff2dce-56d1-470f-ae15-2ffb1bcc6a3c.png',5,2,1,0,'2026-02-09 14:46:54','2026-02-09 14:46:53'),(19,'宠物磨牙棒','',72.00,99,'/upload/880f880a-d70e-414d-9ea0-e9739de2ab79.png',1,2,1,0,'2026-02-09 14:50:00','2026-04-10 21:29:59'),(20,'皇家猫粮','',190.00,100,'/upload/8239491b-1342-40a3-9705-d50d85b16c59.png',6,2,1,0,'2026-02-09 14:51:37','2026-02-09 14:51:37'),(21,'鸭肉干狗狗零食','',65.00,100,'/upload/0fcd5c1a-47ce-43a1-a3db-b17856e4952f.png',1,2,1,0,'2026-02-09 14:53:03','2026-02-09 14:53:02'),(22,'宠物零食罐头','',22.90,100,'/upload/6dd7a445-916e-4a8a-b615-5f7406125750.png',1,2,1,0,'2026-02-09 14:54:09','2026-02-09 14:54:08'),(23,'宠物零食猫零食猫条','',62.00,100,'/upload/fe2bab3a-3bfd-49f0-8efb-e7d1db7cc5b8.png',1,2,1,0,'2026-02-09 14:55:15','2026-02-09 14:55:14'),(24,'泰国进口 猫罐头85g*24罐 (汤汁型)','',95.00,99,'/upload/7b37934d-491d-40c1-a9d0-c30bb59a6e8f.png',1,2,1,1,'2026-02-09 14:56:34','2026-05-19 19:23:36'),(25,'宠物狗狗零食鸡肉干鸭肉干','',70.60,100,'/upload/ee8e2c56-22eb-4c23-a423-b7052060519c.png',1,2,1,0,'2026-02-09 14:58:10','2026-02-09 14:58:09'),(26,'猫罐头24罐整箱85g罐12罐营养猫咪主食','',34.80,99,'/upload/cfc1d6ed-c0db-46da-8198-b30ee1158fac.png',1,2,1,0,'2026-02-09 15:00:06','2026-05-19 19:15:20'),(27,'狗窝保暖冬季封闭式狗狗四季通用睡垫','',35.00,100,'/upload/01d035f7-03d9-4a25-9138-187397189bd1.png',7,2,1,0,'2026-02-09 15:02:38','2026-02-09 15:02:38'),(28,'狗窝冬季保暖可拆洗小型犬狗床','',125.00,100,'/upload/a27fd4e1-8212-4198-8d69-03262d0e3b4e.png',7,2,1,0,'2026-02-09 15:05:14','2026-02-09 15:05:14'),(29,'猫抓柱仙人掌立式猫抓','',40.00,98,'/upload/c7ad3d6a-c00d-476f-871b-ce8cd1821cc5.png',2,2,1,3,'2026-02-09 15:06:52','2026-05-19 19:23:36'),(30,'小型犬猫咪碗','',16.00,96,'/upload/d6f721fc-6890-465a-acf3-de75b7e1152c.png',2,2,1,1,'2026-02-09 15:08:00','2026-05-19 19:20:29'),(31,'宠物饮水机','',264.00,100,'/upload/b40970b1-6386-42f4-aee8-e9b9ebc9fca3.png',2,2,1,0,'2026-02-09 15:08:51','2026-02-09 15:08:51'),(32,'猫狗泪痕口服液','',56.00,98,'/upload/9842cfc8-e17b-4b91-8bf3-4af94f7410f1.png',4,2,1,0,'2026-02-09 15:09:59','2026-05-19 19:15:20'),(33,'狗狗玩具耐咬磨牙球','',17.80,99,'/upload/49a4ec96-e5ed-4a99-9a53-8b43f6cc26e0.png',3,2,1,0,'2026-02-09 15:11:59','2026-04-17 17:19:06'),(34,'宠物洁牙清洁指套','',18.80,100,'/upload/bef0d314-be04-4105-b9ea-e334172b6d2b.png',2,2,1,0,'2026-02-09 15:13:24','2026-02-09 15:13:24'),(35,'猫咪洗耳液','',29.90,99,'/upload/71d049c4-ff1a-463e-ab7a-1669b1ee3007.png',2,2,1,0,'2026-02-09 15:14:25','2026-04-10 21:06:21'),(36,'幼猫全价幼猫粮鲜肉冻干奶猫干湿两吃2kg','',104.00,100,'/upload/38c7902e-b8bd-4253-8369-c41a0b719d52.png',2,2,1,0,'2026-02-09 15:18:16','2026-02-09 15:18:16'),(37,'皇家猫粮 美短成猫粮 ASA31 通用粮 12月以上 2KG','',164.00,97,'/upload/abcbe73c-d119-433c-bc41-b4f8d398f30e.png',6,2,1,0,'2026-02-09 15:19:24','2026-05-14 13:24:00'),(38,'鲜肉双拼猫粮','',49.90,99,'/upload/f9276034-fa3c-4d3f-8f3b-6f0c91573866.png',6,2,1,0,'2026-02-09 15:21:13','2026-03-23 19:22:23'),(39,'全能优选成猫粮肠道舒适型调理粮2Kg猫咪主食粮','',145.50,96,'/upload/b4cb33e0-92db-493f-91d3-3adc8242a1f5.png',6,2,1,0,'2026-02-09 15:22:48','2026-05-19 19:20:29');
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recharge_record`
--

DROP TABLE IF EXISTS `recharge_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `recharge_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '充值单号',
  `amount` decimal(10,2) NOT NULL COMMENT '充值金额',
  `payment_method` int DEFAULT '1' COMMENT '支付方式：1-支付宝 2-微信 3-银行卡',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-待审核 1-审核通过 2-审核拒绝',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `recharge_no` (`recharge_no`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_recharge_no` (`recharge_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='充值记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recharge_record`
--

LOCK TABLES `recharge_record` WRITE;
/*!40000 ALTER TABLE `recharge_record` DISABLE KEYS */;
INSERT INTO `recharge_record` VALUES (1,1,'RC177007865651120F6',1.00,1,1,'2026-02-03 08:30:57'),(2,1,'RC1770078663664AED8',50.00,1,1,'2026-02-03 08:31:04'),(3,7,'RC1770195071377B9D6',1.00,1,1,'2026-02-04 16:51:11'),(4,7,'RC177019508786503E5',34.00,1,1,'2026-02-04 16:51:28'),(5,7,'RC1770195095868EFB2',34.00,1,1,'2026-02-04 16:51:36'),(6,7,'RC17701951147284DFF',500.00,1,1,'2026-02-04 16:51:55'),(7,7,'RC177226620934303CD',1000.00,1,1,'2026-02-28 16:10:09'),(8,7,'RC17727113956620CF7',1000.00,1,1,'2026-03-05 19:49:56'),(9,7,'RC177271573288359bae758',1.00,1,1,'2026-03-05 21:02:13'),(10,7,'RC177271584752663c7da6d',123.00,1,1,'2026-03-05 21:04:08'),(11,7,'RC1772715855433eb6d86e3',12.00,1,1,'2026-03-05 21:04:15'),(12,7,'RC177271677254142351646',1000.00,1,1,'2026-03-05 21:19:33'),(13,1,'RC17727815024245d5f45a7',10000.00,1,1,'2026-03-06 15:18:22'),(14,7,'RC1772784327969a3e6bfb3',5000.00,1,1,'2026-03-06 16:05:28'),(15,7,'RC177340997805838c7352b',1.00,1,1,'2026-03-13 21:52:58'),(16,7,'RC177426480310872dd0790',1000.00,1,1,'2026-03-23 19:20:03'),(17,7,'RC177478345450188a29c7a',1.00,2,1,'2026-03-29 19:24:15'),(18,10,'RC1776339002666b71fb2db',100.50,1,1,'2026-04-16 19:30:03'),(19,7,'RC1776345847344e847e0e0',1.00,2,1,'2026-04-16 21:24:07'),(20,12,'RC177640848293781ff9750',50.00,1,1,'2026-04-17 14:48:03'),(24,12,'RC1776409341542d179c2f0',500.00,1,1,'2026-04-17 15:02:22'),(25,12,'RC1776414542643c923da2d',100.00,1,1,'2026-04-17 16:29:03'),(26,7,'RC1779189344605b9256eff',1.00,2,1,'2026-05-19 19:15:45'),(27,7,'RC1779189651331a732395c',1.00,2,1,'2026-05-19 19:20:51'),(28,7,'RC1779189831961bb4a926f',1.00,2,1,'2026-05-19 19:23:52');
/*!40000 ALTER TABLE `recharge_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `rating` int DEFAULT NULL COMMENT '评分：1-5星',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '评价内容',
  `images` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '评价图片',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) DEFAULT 'pending' COMMENT '审核状态：pending-待审核，passed-已通过，rejected-已驳回',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_product_id` (`product_id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='评价表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
INSERT INTO `review` VALUES (1,5,5,1,5,'测试',NULL,'2026-02-03 08:38:10','1'),(2,7,4,7,5,'非常好用',NULL,'2026-02-04 16:54:21','1'),(3,8,3,7,5,'666666',NULL,'2026-02-04 21:32:57','1'),(4,8,3,7,5,'666666',NULL,'2026-02-05 15:19:54','1'),(5,9,37,7,5,'66666',NULL,'2026-02-28 16:11:54','1'),(6,9,32,7,5,'666666',NULL,'2026-02-28 16:12:02','1'),(7,9,37,7,5,'666',NULL,'2026-02-28 17:02:29','1'),(8,9,37,7,5,'88888',NULL,'2026-03-05 21:52:53','1'),(9,10,39,7,5,'量大管饱。',NULL,'2026-03-05 22:18:42','1'),(10,11,37,7,5,'猫爱吃',NULL,'2026-03-06 15:16:39','1'),(11,12,42,7,5,'不粘连，好清理。',NULL,'2026-03-06 16:24:49','1'),(13,14,40,7,5,'917813',NULL,'2026-03-23 19:30:13','1'),(16,13,38,7,5,'测试',NULL,'2026-03-29 19:28:20','1'),(18,33,30,12,5,'测试',NULL,'2026-04-17 15:44:30','1'),(19,37,29,7,5,'测试',NULL,'2026-05-19 19:26:08','1');
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '真实姓名',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '地址',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '账户余额',
  `role` int DEFAULT '0' COMMENT '角色：0-普通用户 1-商家 2-管理员',
  `status` int DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `username` (`username`) USING BTREE,
  KEY `idx_username` (`username`) USING BTREE,
  KEY `idx_role` (`role`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','0192023a7bbd73250516f069df18b500','admin@petmall.com',NULL,NULL,NULL,10001.00,2,1,'2026-01-18 14:06:21','2026-03-06 15:18:35'),(2,'merchant1','a52f2c0dbf38ade4f715e02c7124046e','merchant1@petmall.com',NULL,NULL,NULL,0.00,1,1,'2026-01-18 14:06:21','2026-02-04 17:15:20'),(3,'demo123','e39b11c785532c3dfd8a632142eabd6d','3512314@qq.com','13783177750',NULL,NULL,0.00,0,1,'2026-01-18 14:23:17','2026-01-18 14:23:16'),(4,'789789','21b95a0f90138767b0fd324e6be3457b','test789@qq.com','17983799851',NULL,NULL,0.00,0,1,'2026-01-18 15:32:28','2026-01-18 15:32:28'),(5,'111111','e39b11c785532c3dfd8a632142eabd6d','32431@qq.com','15270722801',NULL,NULL,0.00,0,1,'2026-01-18 22:15:44','2026-01-18 22:15:43'),(6,'121212','e39b11c785532c3dfd8a632142eabd6d','3544217987@qq.com','17837988771',NULL,NULL,0.00,1,1,'2026-01-18 22:19:20','2026-02-06 13:44:24'),(7,'赵四','e39b11c785532c3dfd8a632142eabd6d','3133239817@qq.com','15436755478',NULL,NULL,6664.30,0,1,'2026-02-04 16:50:06','2026-06-17 16:40:27'),(8,'海薇玛夫人','e39b11c785532c3dfd8a632142eabd6d','3144523165@qq.com','13287453548',NULL,NULL,0.00,1,1,'2026-02-06 13:45:42','2026-04-10 20:09:25'),(12,'test001','e39b11c785532c3dfd8a632142eabd6d','test789@163.com','13800000000',NULL,NULL,493.40,0,1,'2026-04-17 12:36:09','2026-04-17 17:19:09'),(14,'merchant2','d964173dc44da83eeafa3aebbee9a1a0','3133239816@qq.com','13800000000',NULL,NULL,0.00,1,1,'2026-04-17 16:48:34','2026-05-19 19:25:09'),(15,'testuser','e39b11c785532c3dfd8a632142eabd6d','18790416261@163.com','18790426161',NULL,NULL,0.00,0,1,'2026-05-19 19:34:43','2026-06-07 13:41:20');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-27 17:23:04

--
-- Table structure for table `pet_profile`
--
-- 宠物档案表（AI 导购新功能 v1）

DROP TABLE IF EXISTS `pet_profile`;
CREATE TABLE `pet_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `pet_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '宠物类型: dog/cat/bird/fish/other',
  `breed` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '品种',
  `age_months` smallint DEFAULT NULL COMMENT '年龄（月）',
  `weight_kg` decimal(5,2) DEFAULT NULL COMMENT '体重（kg）',
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注（如过敏、特殊饮食）',
  `is_primary` tinyint DEFAULT '0' COMMENT '是否主要宠物: 0-否 1-是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='宠物档案表';
