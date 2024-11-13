-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: 10.136.129.15    Database: dms_community
-- ------------------------------------------------------
-- Server version	5.5.5-10.2.26-MariaDB-log

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
-- Table structure for table `activity_log`
--

DROP TABLE IF EXISTS `activity_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `operation_id` int(4) NOT NULL,
  `object_id` int(4) NOT NULL,
  `object_pk` int(10) NOT NULL,
  `operation_time` bigint(13) NOT NULL,
  `origin` varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `content` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `attachment_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_id` (`operation_id`),
  KEY `idx_object_id` (`object_id`),
  KEY `idx_object_pk` (`object_pk`),
  KEY `idx_attachment_id` (`attachment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=756953 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_log_testbywill`
--

DROP TABLE IF EXISTS `activity_log_testbywill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_log_testbywill` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` char(7) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `operation_id` int(4) NOT NULL DEFAULT 0,
  `object_id` int(4) NOT NULL DEFAULT 0,
  `object_pk` int(10) NOT NULL DEFAULT 0,
  `operation_time` bigint(13) NOT NULL DEFAULT 0,
  `origin` varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `content` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `attachment_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10373 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `activity_log_users_ops_objs_view`
--

DROP TABLE IF EXISTS `activity_log_users_ops_objs_view`;
/*!50001 DROP VIEW IF EXISTS `activity_log_users_ops_objs_view`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `activity_log_users_ops_objs_view` AS SELECT
 1 AS `id`,
 1 AS `user_id`,
 1 AS `cname`,
 1 AS `operation_id`,
 1 AS `operation_name`,
 1 AS `object_id`,
 1 AS `object_name`,
 1 AS `object_pk`,
 1 AS `operation_time`,
 1 AS `origin`,
 1 AS `content`,
 1 AS `attachment_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `attachment_keyman`
--

DROP TABLE IF EXISTS `attachment_keyman`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attachment_keyman` (
  `attachment_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  UNIQUE KEY `attachment_id_user_id` (`attachment_id`,`user_id`),
  KEY `idx_attachment_id_user_id` (`attachment_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bookmark`
--

DROP TABLE IF EXISTS `bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookmark` (
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `object_id` int(4) NOT NULL,
  `object_pk` int(10) NOT NULL DEFAULT 0,
  `bookmark_create_time` bigint(13) NOT NULL,
  UNIQUE KEY `user_id_object_id_object_pk` (`user_id`,`object_id`,`object_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `communities`
--

DROP TABLE IF EXISTS `communities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `communities` (
  `community_id` int(10) NOT NULL AUTO_INCREMENT,
  `community_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_desc` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_type` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_category` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_status` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_img_banner` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `community_img_avatar` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `community_create_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_create_time` bigint(13) NOT NULL DEFAULT 0,
  `community_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `community_delete_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_delete_time` bigint(13) NOT NULL DEFAULT 0,
  `community_last_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_last_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `community_group_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_ddf_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`community_id`),
  UNIQUE KEY `community_name_community_category_community_status` (`community_name`,`community_category`,`community_status`,`community_group_id`,`community_delete_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1830 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `communities_create_review`
--

DROP TABLE IF EXISTS `communities_create_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `communities_create_review` (
  `batch_id` int(10) NOT NULL AUTO_INCREMENT,
  `community_name` char(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_desc` char(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_category` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `applicant_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `application_time` bigint(13) NOT NULL DEFAULT 0,
  `reviewer_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `review_time` bigint(13) NOT NULL DEFAULT 0,
  `status` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `rejected_message` char(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_type` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_language` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`batch_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1613 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `communities_create_review_setting`
--

DROP TABLE IF EXISTS `communities_create_review_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `communities_create_review_setting` (
  `batch_id` int(10) NOT NULL DEFAULT 0,
  `notification_type` char(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `communities_setting`
--

DROP TABLE IF EXISTS `communities_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `communities_setting` (
  `community_id` int(10) NOT NULL DEFAULT 0,
  `community_language` char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `notification_type` char(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`community_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `communities_view`
--

DROP TABLE IF EXISTS `communities_view`;
/*!50001 DROP VIEW IF EXISTS `communities_view`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `communities_view` AS SELECT
 1 AS `community_id`,
 1 AS `community_name`,
 1 AS `community_desc`,
 1 AS `community_type`,
 1 AS `community_category`,
 1 AS `community_status`,
 1 AS `community_create_user_id`,
 1 AS `community_create_time`,
 1 AS `community_modified_user_id`,
 1 AS `community_modified_time`,
 1 AS `community_delete_user_id`,
 1 AS `community_delete_time`,
 1 AS `community_last_modified_user_id`,
 1 AS `community_last_modified_time`,
 1 AS `community_group_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `community_announcement`
--

DROP TABLE IF EXISTS `community_announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_announcement` (
  `community_id` int(10) NOT NULL,
  `community_text` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `community_create_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_create_time` bigint(13) NOT NULL DEFAULT 0,
  `community_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_modified_time` bigint(13) NOT NULL DEFAULT 0,
  PRIMARY KEY (`community_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_delete_review`
--

DROP TABLE IF EXISTS `community_delete_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_delete_review` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `community_id` int(10) NOT NULL DEFAULT 0,
  `community_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `applicant_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `application_subject` char(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `application_desc` char(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `application_time` bigint(13) NOT NULL DEFAULT 0,
  `reviewer_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `review_time` bigint(13) NOT NULL DEFAULT 0,
  `status` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'approved/rejected',
  `rejected_message` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=403 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=COMPACT;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_join_review`
--

DROP TABLE IF EXISTS `community_join_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_join_review` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `community_id` int(10) NOT NULL DEFAULT 0,
  `application_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `application_desc` char(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `application_time` bigint(13) NOT NULL DEFAULT 0,
  `reviewer_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `review_time` bigint(13) NOT NULL DEFAULT 0,
  `status` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'approved/auto-approved/rejected',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=254 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_role`
--

DROP TABLE IF EXISTS `community_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_role` (
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `community_id` int(10) NOT NULL DEFAULT 0,
  `role_id` int(4) NOT NULL DEFAULT 0,
  UNIQUE KEY `user_id_community_id_role_id` (`user_id`,`community_id`,`role_id`),
  KEY `idx_community_id` (`community_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_special_type`
--

DROP TABLE IF EXISTS `community_special_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_special_type` (
  `community_special_type_id` int(2) NOT NULL AUTO_INCREMENT,
  `community_special_type_name` char(8) COLLATE utf8mb4_bin NOT NULL,
  `conclusion_alert` tinyint(1) NOT NULL DEFAULT 0,
  `dashboard` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`community_special_type_id`),
  UNIQUE KEY `community_special_type_name` (`community_special_type_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_special_type_mapping`
--

DROP TABLE IF EXISTS `community_special_type_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_special_type_mapping` (
  `community_special_type_id` int(2) NOT NULL,
  `topic_type_id` int(2) NOT NULL,
  PRIMARY KEY (`community_special_type_id`,`topic_type_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_special_type_rule_column_mapping`
--

DROP TABLE IF EXISTS `community_special_type_rule_column_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_special_type_rule_column_mapping` (
  `community_special_type_id` int(2) NOT NULL,
  `rule_column_id` int(2) NOT NULL,
  `rule_column_order` int(2) NOT NULL,
  KEY `FK_community_rule_column_mapping_forum_rule_column` (`rule_column_id`),
  CONSTRAINT `FK_community_rule_column_mapping_forum_rule_column` FOREIGN KEY (`rule_column_id`) REFERENCES `forum_conclusion_alert_rule_column` (`rule_column_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_support_special_type`
--

DROP TABLE IF EXISTS `community_support_special_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_support_special_type` (
  `community_id` int(10) NOT NULL,
  `community_special_type_id` int(2) NOT NULL,
  PRIMARY KEY (`community_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ddf_delete_queue`
--

DROP TABLE IF EXISTS `ddf_delete_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ddf_delete_queue` (
  `ddf_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`ddf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ddf_generate_queue`
--

DROP TABLE IF EXISTS `ddf_generate_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ddf_generate_queue` (
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id` int(11) NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` longtext COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `time_stamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  UNIQUE KEY `type_id_status` (`type`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eerp_report_log`
--

DROP TABLE IF EXISTS `eerp_report_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eerp_report_log` (
  `process_start_time` char(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `process_end_time` char(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `eerp_type` char(2) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `report_status` char(10) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT 'success/fail',
  `message` text COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `report_start_timestamp` bigint(13) NOT NULL,
  `report_end_timestamp` bigint(13) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eerp_report_log_recipient`
--

DROP TABLE IF EXISTS `eerp_report_log_recipient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eerp_report_log_recipient` (
  `address` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `emoji`
--

DROP TABLE IF EXISTS `emoji`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emoji` (
  `id` int(4) NOT NULL,
  `desc` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_archive_queue`
--

DROP TABLE IF EXISTS `file_archive_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_archive_queue` (
  `type` enum('EERPMHIGHLEVEL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `status` enum('WAIT','FAIL','PROCESSING') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WAIT',
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `action_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`type`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_extension_order`
--

DROP TABLE IF EXISTS `file_extension_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_extension_order` (
  `file_ext` char(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_icon` char(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ext_order` int(5) NOT NULL,
  KEY `file_ext` (`file_ext`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_privilege`
--

DROP TABLE IF EXISTS `file_privilege`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_privilege` (
  `file_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `member` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `admin` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert`
--

DROP TABLE IF EXISTS `forum_conclusion_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert` (
  `forum_id` int(10) NOT NULL,
  `forum_conclusion_alert_group_last_modified_user_id` char(7) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_conclusion_alert_group_last_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `forum_conclusion_alert_rule_last_modified_user_id` char(7) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_conclusion_alert_rule_last_modified_time` bigint(13) NOT NULL DEFAULT 0,
  PRIMARY KEY (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert_group`
--

DROP TABLE IF EXISTS `forum_conclusion_alert_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert_group` (
  `forum_conclusion_alert_group_id` int(10) NOT NULL AUTO_INCREMENT,
  `forum_id` int(10) NOT NULL,
  `forum_conclusion_alert_group_name` varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`forum_conclusion_alert_group_id`),
  KEY `forum_id` (`forum_id`),
  KEY `forum_conclusion_alert_group_name` (`forum_conclusion_alert_group_name`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert_group_member`
--

DROP TABLE IF EXISTS `forum_conclusion_alert_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert_group_member` (
  `forum_conclusion_alert_group_id` int(10) NOT NULL,
  `forum_conclusion_alert_group_user_id` char(7) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`forum_conclusion_alert_group_id`,`forum_conclusion_alert_group_user_id`),
  CONSTRAINT `FK_forum_conclusion_alert_group_id` FOREIGN KEY (`forum_conclusion_alert_group_id`) REFERENCES `forum_conclusion_alert_group` (`forum_conclusion_alert_group_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert_rule`
--

DROP TABLE IF EXISTS `forum_conclusion_alert_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert_rule` (
  `forum_conclusion_alert_rule_id` int(10) NOT NULL AUTO_INCREMENT,
  `forum_id` int(10) NOT NULL,
  `forum_conclusion_alert_rule_start_day` tinyint(4) NOT NULL DEFAULT 0,
  `forum_conclusion_alert_rule_end_day` tinyint(4) NOT NULL DEFAULT 0,
  `factory_id` int(10) NOT NULL DEFAULT 1,
  `rule_type` enum('GENERAL','HIGH') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GENERAL',
  PRIMARY KEY (`forum_conclusion_alert_rule_id`),
  KEY `forum_id` (`forum_id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert_rule_column`
--

DROP TABLE IF EXISTS `forum_conclusion_alert_rule_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert_rule_column` (
  `rule_column_id` int(2) NOT NULL,
  `rule_column_name` char(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `rule_column_type` char(14) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`rule_column_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert_rule_column_dropdown`
--

DROP TABLE IF EXISTS `forum_conclusion_alert_rule_column_dropdown`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert_rule_column_dropdown` (
  `dropdown_id` int(10) NOT NULL AUTO_INCREMENT,
  `rule_column_id` int(2) NOT NULL,
  `dropdown_name` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`dropdown_id`),
  KEY `FK_rule_column_dropdown_rule_column` (`rule_column_id`),
  CONSTRAINT `FK_rule_column_dropdown_rule_column` FOREIGN KEY (`rule_column_id`) REFERENCES `forum_conclusion_alert_rule_column` (`rule_column_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert_rule_column_rangeday`
--

DROP TABLE IF EXISTS `forum_conclusion_alert_rule_column_rangeday`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert_rule_column_rangeday` (
  `rangeday_id` int(10) NOT NULL AUTO_INCREMENT,
  `rule_column_id` int(2) NOT NULL,
  `allow_limit` tinyint(1) NOT NULL DEFAULT 0,
  `from_day` tinyint(4) NOT NULL DEFAULT 0,
  `end_day` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`rangeday_id`),
  KEY `FK_rule_column_rangeday_rule_column` (`rule_column_id`),
  CONSTRAINT `FK_rule_column_rangeday_rule_column` FOREIGN KEY (`rule_column_id`) REFERENCES `forum_conclusion_alert_rule_column` (`rule_column_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_conclusion_alert_rule_member`
--

DROP TABLE IF EXISTS `forum_conclusion_alert_rule_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_conclusion_alert_rule_member` (
  `forum_conclusion_alert_rule_id` int(10) NOT NULL,
  `forum_conclusion_alert_rule_member_id` varchar(7) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_conclusion_alert_rule_member_type` char(8) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT 'group/user',
  PRIMARY KEY (`forum_conclusion_alert_rule_id`,`forum_conclusion_alert_rule_member_id`),
  CONSTRAINT `FK_forum_conclusion_alert_rule_id` FOREIGN KEY (`forum_conclusion_alert_rule_id`) REFERENCES `forum_conclusion_alert_rule` (`forum_conclusion_alert_rule_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_join_review`
--

DROP TABLE IF EXISTS `forum_join_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_join_review` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `forum_id` int(10) NOT NULL DEFAULT 0,
  `application_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `application_desc` char(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `application_time` bigint(13) NOT NULL DEFAULT 0,
  `reviewer_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `review_time` bigint(13) NOT NULL DEFAULT 0,
  `status` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'approved/auto-approved/rejected',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=158 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_role`
--

DROP TABLE IF EXISTS `forum_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_role` (
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_id` int(10) NOT NULL DEFAULT 0,
  `role_id` int(4) NOT NULL DEFAULT 0,
  UNIQUE KEY `user_id_forum_id_role_id` (`user_id`,`forum_id`,`role_id`),
  KEY `idx_forum_id` (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_support_topic_type`
--

DROP TABLE IF EXISTS `forum_support_topic_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_support_topic_type` (
  `forum_id` int(10) NOT NULL,
  `topic_type_id` int(2) NOT NULL,
  PRIMARY KEY (`forum_id`,`topic_type_id`),
  KEY `forum_id` (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forum_tag`
--

DROP TABLE IF EXISTS `forum_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_tag` (
  `forum_id` int(10) NOT NULL DEFAULT 0,
  `forum_tag` char(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forums`
--

DROP TABLE IF EXISTS `forums`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forums` (
  `forum_id` int(10) NOT NULL AUTO_INCREMENT,
  `community_id` int(10) NOT NULL DEFAULT 0,
  `forum_type` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `forum_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `forum_desc` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `forum_img_avatar` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `forum_status` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `forum_create_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_create_time` bigint(13) NOT NULL DEFAULT 0,
  `forum_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `forum_delete_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_delete_time` bigint(13) NOT NULL DEFAULT 0,
  `forum_last_topic_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `forum_last_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `forum_last_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `forum_ddf_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `forum_topping_order` int(1) unsigned DEFAULT 0,
  PRIMARY KEY (`forum_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3181 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `images`
--

DROP TABLE IF EXISTS `images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `images` (
  `image_id` int(10) NOT NULL,
  `image_content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `innovation_award`
--

DROP TABLE IF EXISTS `innovation_award`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `innovation_award` (
  `innovation_award_id` int(10) NOT NULL AUTO_INCREMENT,
  `classification_name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `project_item_name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `oa_instance_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `project_executive_summary` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT 'wait/attachmentCreated/creating/success/fail',
  `message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `apply_time` bigint(13) NOT NULL DEFAULT 0,
  `apply_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `create_topic_time` bigint(13) NOT NULL DEFAULT 0,
  PRIMARY KEY (`innovation_award_id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `innovation_award_attachment`
--

DROP TABLE IF EXISTS `innovation_award_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `innovation_award_attachment` (
  `innovation_award_id` int(10) NOT NULL DEFAULT 0,
  `attachment_path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `attachment_path_status` char(12) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'wait' COMMENT 'wait/checked/downloaded',
  PRIMARY KEY (`attachment_path`),
  KEY `innovation_award_id` (`innovation_award_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `innovation_award_attachment_ddf`
--

DROP TABLE IF EXISTS `innovation_award_attachment_ddf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `innovation_award_attachment_ddf` (
  `attachment_path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `file_size` bigint(13) NOT NULL DEFAULT 0,
  `ddf_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`attachment_path`,`file_name`),
  KEY `attachment_path` (`attachment_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `innovation_award_member`
--

DROP TABLE IF EXISTS `innovation_award_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `innovation_award_member` (
  `innovation_award_id` int(10) NOT NULL DEFAULT 0,
  `user_id` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `user_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  UNIQUE KEY `user_id_innovation_award_id_type` (`user_id`,`innovation_award_id`,`user_type`),
  KEY `idx_innovation_award_id` (`innovation_award_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `innovation_award_member_view`
--

DROP TABLE IF EXISTS `innovation_award_member_view`;
/*!50001 DROP VIEW IF EXISTS `innovation_award_member_view`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `innovation_award_member_view` AS SELECT
 1 AS `innovation_award_id`,
 1 AS `user_id`,
 1 AS `user_type`,
 1 AS `cname`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `innovation_award_view`
--

DROP TABLE IF EXISTS `innovation_award_view`;
/*!50001 DROP VIEW IF EXISTS `innovation_award_view`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `innovation_award_view` AS SELECT
 1 AS `innovation_award_id`,
 1 AS `classification_name`,
 1 AS `project_item_name`,
 1 AS `status`,
 1 AS `message`,
 1 AS `apply_time`,
 1 AS `cname`,
 1 AS `create_topic_time`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `ios_device`
--

DROP TABLE IF EXISTS `ios_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ios_device` (
  `device_uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `language` char(7) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`device_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mail`
--

DROP TABLE IF EXISTS `mail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mail` (
  `recipient` longtext COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `subject` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` int(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `batch_id` int(10) NOT NULL DEFAULT 0,
  `type` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `type` char(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` enum('UNREAD','READ') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNREAD',
  `title` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `time` bigint(13) NOT NULL DEFAULT 0,
  `priority` int(10) NOT NULL DEFAULT 5,
  `community_id` int(11) NOT NULL DEFAULT 0,
  `community_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `community_category` char(36) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `forum_id` int(11) NOT NULL DEFAULT 0,
  `forum_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `forum_type` char(16) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `topic_id` int(11) NOT NULL DEFAULT 0,
  `topic_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `topic_type` char(16) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `sender_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=40370 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_access_time`
--

DROP TABLE IF EXISTS `notification_access_time`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_access_time` (
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `time` bigint(13) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_review`
--

DROP TABLE IF EXISTS `notification_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_review` (
  `id` int(11) NOT NULL COMMENT 'notification_id',
  `application_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `objects`
--

DROP TABLE IF EXISTS `objects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `objects` (
  `object_id` int(4) NOT NULL,
  `object_name` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `operations`
--

DROP TABLE IF EXISTS `operations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operations` (
  `operation_id` int(4) NOT NULL,
  `operation_name` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `permission_id` int(4) NOT NULL,
  `object_id` int(4) NOT NULL DEFAULT 0,
  `operation_id` int(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `replies`
--

DROP TABLE IF EXISTS `replies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `replies` (
  `reply_id` int(10) NOT NULL AUTO_INCREMENT,
  `forum_id` int(10) NOT NULL DEFAULT 0,
  `follow_topic_id` int(10) NOT NULL DEFAULT 0,
  `follow_reply_id` int(10) NOT NULL DEFAULT 0,
  `reply_status` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `reply_create_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `reply_create_time` bigint(13) NOT NULL DEFAULT 0,
  `reply_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `reply_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `reply_delete_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `reply_delete_time` bigint(13) NOT NULL DEFAULT 0,
  `reply_index` int(10) NOT NULL DEFAULT 0,
  `reply_respondee` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`reply_id`),
  UNIQUE KEY `follow_topic_id_follow_reply_id_reply_index` (`follow_topic_id`,`follow_reply_id`,`reply_index`,`reply_create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=11155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `replies_text`
--

DROP TABLE IF EXISTS `replies_text`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `replies_text` (
  `reply_id` int(10) NOT NULL,
  `reply_text` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `reply_conclusion_text` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`reply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reply_attachment`
--

DROP TABLE IF EXISTS `reply_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_attachment` (
  `reply_id` int(10) NOT NULL DEFAULT 0,
  `attachment_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `attachment_status` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open' COMMENT 'open/delete',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_ext` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_created_time` bigint(13) DEFAULT NULL,
  `delete_user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `user_delete_time` bigint(13) NOT NULL DEFAULT 0,
  KEY `idx_reply_attachment_reply_id` (`reply_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reply_attachment_app_field`
--

DROP TABLE IF EXISTS `reply_attachment_app_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_attachment_app_field` (
  `attachment_id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `app_field_id` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`attachment_id`,`app_field_id`),
  KEY `attachment_id` (`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reply_attachment_record_type`
--

DROP TABLE IF EXISTS `reply_attachment_record_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_attachment_record_type` (
  `attachment_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `record_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reply_bgbu_notification`
--

DROP TABLE IF EXISTS `reply_bgbu_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_bgbu_notification` (
  `reply_id` int(10) NOT NULL,
  `org_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `users` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`reply_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reply_emoji`
--

DROP TABLE IF EXISTS `reply_emoji`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_emoji` (
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `reply_id` int(10) NOT NULL DEFAULT 0,
  `emoji_id` int(4) NOT NULL DEFAULT 0,
  `operation_time` bigint(13) NOT NULL,
  UNIQUE KEY `user_id_reply_id` (`user_id`,`reply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reply_notification`
--

DROP TABLE IF EXISTS `reply_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_notification` (
  `reply_id` int(10) NOT NULL,
  `notification_type` char(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recipient` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  UNIQUE KEY `reply_id` (`reply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reply_orgMember_notification`
--

DROP TABLE IF EXISTS `reply_orgMember_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_orgMember_notification` (
  `reply_id` int(10) NOT NULL,
  `org_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `users` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`reply_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_permission`
--

DROP TABLE IF EXISTS `role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `role_id` int(4) NOT NULL DEFAULT 0,
  `permission_id` int(4) NOT NULL DEFAULT 0,
  KEY `role_id_permission_id` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `role_id` int(4) NOT NULL,
  `role_name` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `roles_permissions_objects_opeartions_view`
--

DROP TABLE IF EXISTS `roles_permissions_objects_opeartions_view`;
/*!50001 DROP VIEW IF EXISTS `roles_permissions_objects_opeartions_view`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `roles_permissions_objects_opeartions_view` AS SELECT
 1 AS `role_id`,
 1 AS `role_name`,
 1 AS `permission_id`,
 1 AS `object_id`,
 1 AS `object_name`,
 1 AS `operation_id`,
 1 AS `operation_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `shedlock`
--

DROP TABLE IF EXISTS `shedlock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shedlock` (
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lock_until` timestamp(3) NULL DEFAULT NULL,
  `locked_at` timestamp(3) NULL DEFAULT NULL,
  `locked_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sql_statement_log`
--

DROP TABLE IF EXISTS `sql_statement_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sql_statement_log` (
  `event_time` tinyint(4) NOT NULL,
  `argument` tinyint(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistic_action_results`
--

DROP TABLE IF EXISTS `statistic_action_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistic_action_results` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `action_id` int(10) NOT NULL,
  `action_count` bigint(20) NOT NULL DEFAULT 0,
  `count_time` bigint(13) NOT NULL DEFAULT 0,
  `statistic_level_action_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action_id` (`action_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3159 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistic_actions`
--

DROP TABLE IF EXISTS `statistic_actions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistic_actions` (
  `action_id` int(10) NOT NULL,
  `action_name` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL,
  `method_name` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`action_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistic_level_action`
--

DROP TABLE IF EXISTS `statistic_level_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistic_level_action` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `level_id` int(10) NOT NULL,
  `action_id` int(10) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_level_id` (`level_id`),
  KEY `idx_action_id` (`action_id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistic_level_results`
--

DROP TABLE IF EXISTS `statistic_level_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistic_level_results` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `level_id` int(10) NOT NULL DEFAULT 0,
  `level_count` bigint(20) NOT NULL DEFAULT 0,
  `count_time` bigint(13) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_level_id` (`level_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2606549 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistic_levels`
--

DROP TABLE IF EXISTS `statistic_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistic_levels` (
  `level_id` int(10) NOT NULL AUTO_INCREMENT,
  `level_name` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `start_time` bigint(13) NOT NULL,
  `end_time` bigint(13) NOT NULL DEFAULT 0,
  PRIMARY KEY (`level_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sync_dia_attachment_log`
--

DROP TABLE IF EXISTS `sync_dia_attachment_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sync_dia_attachment_log` (
  `start_time` char(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `end_time` char(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `sync_status` char(10) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT 'success/fail',
  `message` text COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT 'ddfId/error message'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sync_dia_log_recipient`
--

DROP TABLE IF EXISTS `sync_dia_log_recipient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sync_dia_log_recipient` (
  `address` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `temp_gem_statistics`
--

DROP TABLE IF EXISTS `temp_gem_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temp_gem_statistics` (
  `user_id` char(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `green` int(20) DEFAULT 0,
  `blue` int(20) DEFAULT 0,
  `red` int(20) DEFAULT 0,
  `diamond` int(20) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `temp_org`
--

DROP TABLE IF EXISTS `temp_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temp_org` (
  `id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(512) DEFAULT NULL,
  `id_path` varchar(512) DEFAULT NULL,
  `name_path` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_role_privilege`
--

DROP TABLE IF EXISTS `test_role_privilege`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_role_privilege` (
  `role_id` int(11) NOT NULL AUTO_INCREMENT,
  `role_name` char(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `can_search` tinyint(4) NOT NULL,
  `can_read_info` tinyint(4) NOT NULL,
  `can_read_protected` tinyint(4) NOT NULL,
  `can_read_raw` tinyint(4) NOT NULL,
  `can_read_privilege` tinyint(4) NOT NULL,
  `can_update_data` tinyint(4) NOT NULL,
  `can_update_privilege` tinyint(4) NOT NULL,
  `can_delete` tinyint(4) NOT NULL,
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_app_field`
--

DROP TABLE IF EXISTS `topic_app_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_app_field` (
  `topic_id` int(10) NOT NULL,
  `app_field_id` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`topic_id`,`app_field_id`) USING BTREE,
  KEY `topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_attachment`
--

DROP TABLE IF EXISTS `topic_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_attachment` (
  `topic_id` int(10) NOT NULL DEFAULT 0,
  `attachment_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `attachment_status` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open' COMMENT 'open/delete',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_ext` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_created_time` bigint(13) DEFAULT NULL,
  `delete_user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `user_delete_time` bigint(13) NOT NULL DEFAULT 0,
  KEY `idx_topic_attachment_topic_id` (`topic_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_attachment_app_field`
--

DROP TABLE IF EXISTS `topic_attachment_app_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_attachment_app_field` (
  `attachment_id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `app_field_id` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`attachment_id`,`app_field_id`),
  KEY `attachment_id` (`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_bgbu_notification`
--

DROP TABLE IF EXISTS `topic_bgbu_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_bgbu_notification` (
  `topic_id` int(10) NOT NULL,
  `org_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `users` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`topic_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_conclusion_state`
--

DROP TABLE IF EXISTS `topic_conclusion_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_conclusion_state` (
  `topic_conclusion_state_id` int(2) NOT NULL,
  `topic_conclusion_state` char(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `name_enUs` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `name_zhTw` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `name_zhCn` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `topic_conclusion_state_order` int(2) NOT NULL,
  PRIMARY KEY (`topic_conclusion_state_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_emoji`
--

DROP TABLE IF EXISTS `topic_emoji`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_emoji` (
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `topic_id` int(10) NOT NULL DEFAULT 0,
  `emoji_id` int(4) NOT NULL DEFAULT 0,
  `operation_time` bigint(13) NOT NULL,
  UNIQUE KEY `user_id_topic_id` (`user_id`,`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_notification`
--

DROP TABLE IF EXISTS `topic_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_notification` (
  `topic_id` int(10) NOT NULL,
  `notification_type` char(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `recipient` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  UNIQUE KEY `topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_orgMember_notification`
--

DROP TABLE IF EXISTS `topic_orgMember_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_orgMember_notification` (
  `topic_id` int(10) NOT NULL,
  `org_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `users` text COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`topic_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_tag`
--

DROP TABLE IF EXISTS `topic_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_tag` (
  `topic_id` int(10) NOT NULL DEFAULT 0,
  `topic_tag` char(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_type`
--

DROP TABLE IF EXISTS `topic_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_type` (
  `topic_type_id` int(2) NOT NULL,
  `topic_type` char(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `editable` tinyint(1) NOT NULL DEFAULT 0,
  `conclusion_alert` tinyint(1) NOT NULL DEFAULT 0,
  `show_unconclude_state` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'remake topic ddf when state changed',
  `name_enUs` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `name_zhTw` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `name_zhCn` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `topic_type_order` int(2) NOT NULL,
  `app_field_default_id` varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `archive_conclusion_attachment` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`topic_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topic_type_conclusion_mapping`
--

DROP TABLE IF EXISTS `topic_type_conclusion_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_type_conclusion_mapping` (
  `topic_type_id` int(2) NOT NULL,
  `topic_conclusion_state_id` int(2) NOT NULL,
  PRIMARY KEY (`topic_type_id`,`topic_conclusion_state_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topics`
--

DROP TABLE IF EXISTS `topics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topics` (
  `topic_id` int(10) NOT NULL AUTO_INCREMENT,
  `forum_id` int(10) NOT NULL DEFAULT 0,
  `topic_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `topic_type_id` int(2) NOT NULL DEFAULT 0,
  `topic_conclusion_state_id` int(2) NOT NULL DEFAULT 1,
  `topic_status` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `topic_situation` char(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT 'normal/sealed',
  `topic_create_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `topic_create_time` bigint(13) NOT NULL DEFAULT 0,
  `topic_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `topic_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `topic_delete_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `topic_delete_time` bigint(13) NOT NULL DEFAULT 0,
  `topic_view_count` int(10) NOT NULL DEFAULT 0,
  `topic_last_modified_user_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `topic_last_modified_time` bigint(13) NOT NULL DEFAULT 0,
  `topic_ddf_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic_topping_order` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`topic_id`),
  KEY `idx_topics_forum_id` (`forum_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7830 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `topics_text`
--

DROP TABLE IF EXISTS `topics_text`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topics_text` (
  `topic_id` int(10) NOT NULL,
  `topic_text` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transfer_attachment_log`
--

DROP TABLE IF EXISTS `transfer_attachment_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfer_attachment_log` (
  `id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `sourceType` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `oldAttachmentId` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `newAttachmentId` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `oldCreatedTime` bigint(20) DEFAULT NULL,
  `logTime` bigint(20) DEFAULT NULL,
  `communityId` int(11) DEFAULT NULL,
  `communityName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `communityType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `communityCategory` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `forumId` int(11) DEFAULT NULL,
  `forumName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `forumType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `topicId` int(11) DEFAULT NULL,
  `topicName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `topicType` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`,`oldAttachmentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transfer_author_log`
--

DROP TABLE IF EXISTS `transfer_author_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfer_author_log` (
  `id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `oldAttachmentId` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `newAttachmentId` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sourceType` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`,`oldAttachmentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transfer_richtext_log`
--

DROP TABLE IF EXISTS `transfer_richtext_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfer_richtext_log` (
  `id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sourceType` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `account` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `cname` char(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` char(36) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_community_award`
--

DROP TABLE IF EXISTS `v_community_award`;
/*!50001 DROP VIEW IF EXISTS `v_community_award`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_community_award` AS SELECT
 1 AS `id`,
 1 AS `type`,
 1 AS `award_id`,
 1 AS `disabled`,
 1 AS `contest_name`,
 1 AS `award_title`,
 1 AS `contest_proposal`,
 1 AS `team_name`,
 1 AS `link`,
 1 AS `create_time`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_dropdown_i18n`
--

DROP TABLE IF EXISTS `v_dropdown_i18n`;
/*!50001 DROP VIEW IF EXISTS `v_dropdown_i18n`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_dropdown_i18n` AS SELECT
 1 AS `parent`,
 1 AS `dropdown_id`,
 1 AS `category`,
 1 AS `en_US`,
 1 AS `zh_TW`,
 1 AS `zh_CN`,
 1 AS `i18n_id`,
 1 AS `priority`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_medal`
--

DROP TABLE IF EXISTS `v_medal`;
/*!50001 DROP VIEW IF EXISTS `v_medal`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_medal` AS SELECT
 1 AS `id`,
 1 AS `type`,
 1 AS `medal_id`,
 1 AS `selected`,
 1 AS `medal_name`,
 1 AS `disabled`,
 1 AS `expire_time`,
 1 AS `create_time`,
 1 AS `group_frame`,
 1 AS `user_frame`,
 1 AS `title`,
 1 AS `certification`,
 1 AS `certification_order`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_topic_eerpm`
--

DROP TABLE IF EXISTS `v_topic_eerpm`;
/*!50001 DROP VIEW IF EXISTS `v_topic_eerpm`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_topic_eerpm` AS SELECT
 1 AS `topic_id`,
 1 AS `topic_title`,
 1 AS `topic_create_time`,
 1 AS `forum_name`,
 1 AS `community_id`,
 1 AS `conclusion_state_id`,
 1 AS `conclusion_create_time`,
 1 AS `factory`,
 1 AS `device_model`,
 1 AS `error_code`,
 1 AS `error_count`,
 1 AS `worst_device_id`,
 1 AS `detail`,
 1 AS `error_desc`,
 1 AS `conclusion`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_topic_eerpp`
--

DROP TABLE IF EXISTS `v_topic_eerpp`;
/*!50001 DROP VIEW IF EXISTS `v_topic_eerpp`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_topic_eerpp` AS SELECT
 1 AS `topic_id`,
 1 AS `topic_title`,
 1 AS `topic_create_time`,
 1 AS `forum_name`,
 1 AS `community_id`,
 1 AS `conclusion_state_id`,
 1 AS `conclusion_create_time`,
 1 AS `factory`,
 1 AS `department`,
 1 AS `area`,
 1 AS `loss_code`,
 1 AS `loss_code_desc_tw`,
 1 AS `loss_code_desc_cn`,
 1 AS `loss_code_desc_en`,
 1 AS `duration`,
 1 AS `line`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_user`
--

DROP TABLE IF EXISTS `v_user`;
/*!50001 DROP VIEW IF EXISTS `v_user`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_user` AS SELECT
 1 AS `user_id`,
 1 AS `account`,
 1 AS `cname`,
 1 AS `mail`,
 1 AS `status`*/;
SET character_set_client = @saved_cs_client;

--
-- Dumping routines for database 'dms_community'
--
/*!50003 DROP PROCEDURE IF EXISTS `procedure_hasPermission` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_bin */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `procedure_hasPermission`(var_user_ids TEXT, var_community_id INT(10), var_forum_id INT(10), var_object_name VARCHAR(36), var_operation_name VARCHAR(36))
BEGIN
	SELECT
		CASE
			WHEN
				(
					SELECT COUNT(*) FROM
						(
							SELECT role_id FROM forum_role WHERE FIND_IN_SET(user_id, @var_user_ids) AND forum_id = @var_forum_id
							UNION
							SELECT role_id FROM community_role WHERE FIND_IN_SET(user_id, @var_user_ids) AND community_id = @var_community_id
							UNION
							SELECT role_id FROM system_role WHERE FIND_IN_SET(user_id, @var_user_ids)
 							UNION
							SELECT
								CASE
									WHEN
										(SELECT COUNT(*) FROM users WHERE FIND_IN_SET(user_id,  @var_user_ids)) > 0
									THEN 6
								END
						) ur,
						role_permission rp, permissions pms, objects objs, operations ops
					WHERE
						ur.role_id = rp.role_id
						AND rp.permission_id = pms.permission_id
						AND pms.object_id = objs.object_id
						AND objs.object_name collate utf8mb4_unicode_ci = @var_object_name
						AND pms.operation_id = ops.operation_id
						AND ops.operation_name collate utf8mb4_unicode_ci = @var_operation_name
				)
			> 0
			THEN
				1
			ELSE 0
		END AS has_permission;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Final view structure for view `activity_log_users_ops_objs_view`
--

/*!50001 DROP VIEW IF EXISTS `activity_log_users_ops_objs_view`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_bin */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `activity_log_users_ops_objs_view` AS select `a`.`id` AS `id`,`a`.`user_id` AS `user_id`,`b`.`cname` AS `cname`,`a`.`operation_id` AS `operation_id`,`c`.`operation_name` AS `operation_name`,`a`.`object_id` AS `object_id`,`d`.`object_name` AS `object_name`,`a`.`object_pk` AS `object_pk`,from_unixtime(`a`.`operation_time` / 1000) AS `operation_time`,`a`.`origin` AS `origin`,`a`.`content` AS `content`,`a`.`attachment_id` AS `attachment_id` from (((`activity_log` `a` join `users` `b`) join `operations` `c`) join `objects` `d`) where `a`.`user_id` = `b`.`user_id` and `a`.`operation_id` = `c`.`operation_id` and `a`.`object_id` = `d`.`object_id` order by `a`.`operation_time` desc limit 1000 */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `communities_view`
--

/*!50001 DROP VIEW IF EXISTS `communities_view`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_bin */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `communities_view` AS select `communities`.`community_id` AS `community_id`,`communities`.`community_name` AS `community_name`,`communities`.`community_desc` AS `community_desc`,`communities`.`community_type` AS `community_type`,`communities`.`community_category` AS `community_category`,`communities`.`community_status` AS `community_status`,`communities`.`community_create_user_id` AS `community_create_user_id`,`communities`.`community_create_time` AS `community_create_time`,`communities`.`community_modified_user_id` AS `community_modified_user_id`,`communities`.`community_modified_time` AS `community_modified_time`,`communities`.`community_delete_user_id` AS `community_delete_user_id`,`communities`.`community_delete_time` AS `community_delete_time`,`communities`.`community_last_modified_user_id` AS `community_last_modified_user_id`,`communities`.`community_last_modified_time` AS `community_last_modified_time`,`communities`.`community_group_id` AS `community_group_id` from `communities` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `innovation_award_member_view`
--

/*!50001 DROP VIEW IF EXISTS `innovation_award_member_view`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `innovation_award_member_view` AS select `dms_community`.`innovation_award_member`.`innovation_award_id` AS `innovation_award_id`,`dms_community`.`innovation_award_member`.`user_id` AS `user_id`,`dms_community`.`innovation_award_member`.`user_type` AS `user_type`,`v_user`.`cname` AS `cname` from (`dms_community`.`innovation_award_member` left join `dms_community`.`v_user` on(`dms_community`.`innovation_award_member`.`user_id` = `v_user`.`user_id`)) order by `dms_community`.`innovation_award_member`.`innovation_award_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `innovation_award_view`
--

/*!50001 DROP VIEW IF EXISTS `innovation_award_view`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `innovation_award_view` AS select `dms_community`.`innovation_award`.`innovation_award_id` AS `innovation_award_id`,`dms_community`.`innovation_award`.`classification_name` AS `classification_name`,`dms_community`.`innovation_award`.`project_item_name` AS `project_item_name`,`dms_community`.`innovation_award`.`status` AS `status`,`dms_community`.`innovation_award`.`message` AS `message`,from_unixtime(`dms_community`.`innovation_award`.`apply_time` / 1000) AS `apply_time`,`v_user`.`cname` AS `cname`,from_unixtime(`dms_community`.`innovation_award`.`create_topic_time` / 1000) AS `create_topic_time` from (`dms_community`.`innovation_award` left join `dms_community`.`v_user` on(`dms_community`.`innovation_award`.`apply_user_id` = `v_user`.`user_id`)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `roles_permissions_objects_opeartions_view`
--

/*!50001 DROP VIEW IF EXISTS `roles_permissions_objects_opeartions_view`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `roles_permissions_objects_opeartions_view` AS select `RP`.`role_id` AS `role_id`,`R`.`role_name` AS `role_name`,`P`.`permission_id` AS `permission_id`,`P`.`object_id` AS `object_id`,`OBJ`.`object_name` AS `object_name`,`P`.`operation_id` AS `operation_id`,`OP`.`operation_name` AS `operation_name` from ((((`role_permission` `RP` left join `roles` `R` on(`RP`.`role_id` = `R`.`role_id`)) left join `permissions` `P` on(`RP`.`permission_id` = `P`.`permission_id`)) left join `objects` `OBJ` on(`P`.`object_id` = `OBJ`.`object_id`)) left join `operations` `OP` on(`P`.`operation_id` = `OP`.`operation_id`)) order by `RP`.`role_id`,`OBJ`.`object_id`,`OP`.`operation_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_community_award`
--

/*!50001 DROP VIEW IF EXISTS `v_community_award`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_bin */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_community_award` AS select `v_award`.`id` AS `id`,`v_award`.`type` AS `type`,`v_award`.`award_id` AS `award_id`,`v_award`.`disabled` AS `disabled`,`v_award`.`contest_name` AS `contest_name`,`v_award`.`award_title` AS `award_title`,`v_award`.`contest_proposal` AS `contest_proposal`,`v_award`.`team_name` AS `team_name`,`v_award`.`link` AS `link`,`v_award`.`create_time` AS `create_time` from `dms_medal`.`v_award` where `v_award`.`type` = 'COMMUNITY' */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_dropdown_i18n`
--

/*!50001 DROP VIEW IF EXISTS `v_dropdown_i18n`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_bin */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_dropdown_i18n` AS select `d`.`parent` AS `parent`,`d`.`val` AS `dropdown_id`,`d`.`category` AS `category`,`i`.`en_US` AS `en_US`,`i`.`zh_TW` AS `zh_TW`,`i`.`zh_CN` AS `zh_CN`,`d`.`val` AS `i18n_id`,`d`.`seq` AS `priority` from (`mydms`.`dropdown` `d` join `mydms`.`i18n` `i` on(`d`.`val` = `i`.`id`)) where `d`.`category` = 'ApplicationField' */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_medal`
--

/*!50001 DROP VIEW IF EXISTS `v_medal`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_bin */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_medal` AS select `v_medal`.`id` AS `id`,`v_medal`.`type` AS `type`,`v_medal`.`medal_id` AS `medal_id`,`v_medal`.`selected` AS `selected`,`v_medal`.`medal_name` AS `medal_name`,`v_medal`.`disabled` AS `disabled`,`v_medal`.`expire_time` AS `expire_time`,`v_medal`.`create_time` AS `create_time`,`v_medal`.`group_frame` AS `group_frame`,`v_medal`.`user_frame` AS `user_frame`,`v_medal`.`title` AS `title`,`v_medal`.`certification` AS `certification`,`v_medal`.`certification_order` AS `certification_order` from `dms_medal`.`v_medal` where `v_medal`.`type` in ('USER','COMMUNITY') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_topic_eerpm`
--

/*!50001 DROP VIEW IF EXISTS `v_topic_eerpm`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_topic_eerpm` AS select `RAW`.`topic_id` AS `topic_id`,`RAW`.`topic_title` AS `topic_title`,`RAW`.`topic_create_time` AS `topic_create_time`,`RAW`.`forum_name` AS `forum_name`,`RAW`.`community_id` AS `community_id`,`RAW`.`conclusion_state_id` AS `conclusion_state_id`,`RAW`.`conclusion_create_time` AS `conclusion_create_time`,json_value(`RAW`.`device`,'$.factory') AS `factory`,json_value(`RAW`.`device`,'$.deviceModel') AS `device_model`,json_value(`RAW`.`device`,'$.errorCode') AS `error_code`,json_value(`RAW`.`device`,'$.errorCount') AS `error_count`,json_query(`RAW`.`device`,'$.worstDeviceId') AS `worst_device_id`,`RAW`.`detail` AS `detail`,`RAW`.`error_desc` AS `error_desc`,`RAW`.`conclusion` AS `conclusion` from (select `T`.`topic_id` AS `topic_id`,`T`.`topic_title` AS `topic_title`,`T`.`topic_create_time` AS `topic_create_time`,`F`.`forum_name` AS `forum_name`,`F`.`community_id` AS `community_id`,`T`.`topic_conclusion_state_id` AS `conclusion_state_id`,ifnull(`R`.`reply_create_time`,0) AS `conclusion_create_time`,json_query(`TT`.`topic_text`,'$.deviceDatas[0]') AS `device`,json_query(`TT`.`topic_text`,'$.deviceDatas[0].methods') AS `detail`,ifnull(json_value(`RT`.`reply_conclusion_text`,'$.errorDesc'),json_value(`TT`.`topic_text`,'$.deviceDatas[0].methods[0].description')) AS `error_desc`,ifnull(`RT`.`reply_conclusion_text`,'') AS `conclusion` from ((((`dms_community`.`topics` `T` left join `dms_community`.`topics_text` `TT` on(`T`.`topic_id` = `TT`.`topic_id`)) left join `dms_community`.`forums` `F` on(`T`.`forum_id` = `F`.`forum_id`)) left join `dms_community`.`replies` `R` on(`T`.`topic_id` = `R`.`follow_topic_id` and `R`.`follow_reply_id` = 0 and `R`.`reply_index` = 0)) left join `dms_community`.`replies_text` `RT` on(`R`.`reply_id` = `RT`.`reply_id`)) where `T`.`topic_type_id` in (4,9) and `T`.`topic_status` <> 'delete') `RAW` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_topic_eerpp`
--

/*!50001 DROP VIEW IF EXISTS `v_topic_eerpp`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_topic_eerpp` AS select `RAW`.`topic_id` AS `topic_id`,`RAW`.`topic_title` AS `topic_title`,`RAW`.`topic_create_time` AS `topic_create_time`,`RAW`.`forum_name` AS `forum_name`,`RAW`.`community_id` AS `community_id`,`RAW`.`conclusion_state_id` AS `conclusion_state_id`,`RAW`.`conclusion_create_time` AS `conclusion_create_time`,json_value(`RAW`.`device`,'$.factory') AS `factory`,ifnull(json_value(`RAW`.`device`,'$.typeCode'),regexp_replace(`RAW`.`forum_name`,'.+-','')) AS `department`,json_value(`RAW`.`device`,'$.area') AS `area`,json_value(`RAW`.`device`,'$.lossCode') AS `loss_code`,json_value(`RAW`.`device`,'$.lossCodeDesc.zh-tw') AS `loss_code_desc_tw`,json_value(`RAW`.`device`,'$.lossCodeDesc.zh-cn') AS `loss_code_desc_cn`,json_value(`RAW`.`device`,'$.lossCodeDesc.en-us') AS `loss_code_desc_en`,json_extract(`RAW`.`device`,'$.solution[*].duration') AS `duration`,json_extract(`RAW`.`device`,'$.solution[*].line') AS `line` from (select `T`.`topic_id` AS `topic_id`,`T`.`topic_title` AS `topic_title`,`T`.`topic_create_time` AS `topic_create_time`,`F`.`forum_name` AS `forum_name`,`F`.`community_id` AS `community_id`,`T`.`topic_conclusion_state_id` AS `conclusion_state_id`,ifnull(`R`.`reply_create_time`,0) AS `conclusion_create_time`,`TT`.`topic_text` AS `device` from (((`dms_community`.`topics` `T` left join `dms_community`.`topics_text` `TT` on(`T`.`topic_id` = `TT`.`topic_id`)) left join `dms_community`.`forums` `F` on(`T`.`forum_id` = `F`.`forum_id`)) left join `dms_community`.`replies` `R` on(`T`.`topic_id` = `R`.`follow_topic_id` and `R`.`follow_reply_id` = 0 and `R`.`reply_index` = 0)) where `T`.`topic_type_id` = 8 and `T`.`topic_status` <> 'delete') `RAW` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_user`
--

/*!50001 DROP VIEW IF EXISTS `v_user`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_bin */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_user` AS select `a`.`uid` AS `user_id`,`a`.`samaccount` AS `account`,`a`.`name` AS `cname`,`a`.`email` AS `mail`,case when `c`.`emp_code` is null then `a`.`status` when `c`.`terminate_date` is null then 0 when `c`.`terminate_date` is not null then 1 end AS `status` from ((`usergroup`.`user_account` `a` left join `internal_talent`.`employee_basic` `c` on(`a`.`uid` = `c`.`uid`)) left join `internal_talent`.`employee` `b` on(`c`.`emp_code` = `b`.`emp_code`)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-01-09 17:13:41
