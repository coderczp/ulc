/*
Navicat MySQL Data Transfer
Source Server Version : 50621
Source Database       : itrip_ulc

Target Server Type    : MYSQL
Target Server Version : 50621
File Encoding         : 65001

Date: 2017-09-11 09:36:28
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for host_bean
-- ----------------------------
DROP TABLE IF EXISTS `host_bean`;
CREATE TABLE `host_bean` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `host` varchar(150) NOT NULL,
  `port` int(11) NOT NULL,
  `user` varchar(150) NOT NULL,
  `pwd` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `host_port` (`host`,`port`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for index_meta
-- ----------------------------
DROP TABLE IF EXISTS `index_meta`;
CREATE TABLE `index_meta` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `bytes` bigint(20) NOT NULL COMMENT '收集的字节数',
  `lines` bigint(20) NOT NULL COMMENT '收集的行数',
  `docs` bigint(20) NOT NULL COMMENT '索引数',
  `shard_id` int(11) NOT NULL COMMENT '分片ID',
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '插入时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6222 DEFAULT CHARSET=utf8;
INSERT INTO index_meta (bytes,`lines`,docs,shard_id) VALUES(0,0,0,0);

-- ----------------------------
-- Table structure for keyword_rule
-- ----------------------------
DROP TABLE IF EXISTS `keyword_rule`;
CREATE TABLE `keyword_rule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host_id` int(11) NOT NULL,
  `file` varchar(150) NOT NULL,
  `keyword` varchar(255) NOT NULL,
  `exclude` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `host_file` (`host_id`,`file`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for lucene_file
-- ----------------------------
DROP TABLE IF EXISTS `lucene_file`;
CREATE TABLE `lucene_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) NOT NULL COMMENT '当前纪录在哪个机器',
  `server` varchar(255) DEFAULT NULL COMMENT '当前纪录是哪一个被监控服务器的日志',
  `itime` bigint(20) DEFAULT NULL COMMENT '该文件夹对应的时间',
  `path` varchar(255) DEFAULT NULL COMMENT 'lucene索引文件的完整路径',
  PRIMARY KEY (`id`),
  KEY `time_index` (`itime`) USING BTREE,
  KEY `server_index` (`server`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for monitor_config
-- ----------------------------
DROP TABLE IF EXISTS `monitor_config`;
CREATE TABLE `monitor_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hostId` int(11) DEFAULT NULL,
  `file` varchar(150) NOT NULL,
  `excludeFile` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `host_file` (`hostId`,`file`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for monitor_file
-- ----------------------------
DROP TABLE IF EXISTS `monitor_file`;
CREATE TABLE `monitor_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `hostId` int(11) NOT NULL,
  `file` varchar(255) NOT NULL COMMENT '文件全路径',
  `shard` int(11) DEFAULT NULL COMMENT '分配ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
