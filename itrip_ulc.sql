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

-- ----------------------------
-- Table structure for processor
-- ----------------------------
DROP TABLE IF EXISTS `processor`;
CREATE TABLE `processor` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hostId` int(11) NOT NULL DEFAULT '-1' COMMENT '管理的主机ID',
  `name` varchar(100) NOT NULL COMMENT '进程名称',
  `path` varchar(255) NOT NULL COMMENT '进程路径',
  `shell` varchar(2555) NOT NULL DEFAULT 'service.sh' COMMENT '进程管理脚本[start stop restart]',
  PRIMARY KEY (`id`),
  UNIQUE KEY `host_name` (`hostId`,`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for menu
-- ----------------------------
DROP TABLE IF EXISTS `menu`;
CREATE TABLE `menu`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单称',
  `href` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单链接',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of menu
-- ----------------------------
INSERT INTO `menu` VALUES (1, '日志搜索', './log.html');
INSERT INTO `menu` VALUES (2, '进程管理', './proc_mgr.html');
INSERT INTO `menu` VALUES (3, '主机管理', './proc.html');
INSERT INTO `menu` VALUES (4, '系统信息', './index.html');
INSERT INTO `menu` VALUES (5, 'PV', './pv.html');
INSERT INTO `menu` VALUES (6, '菜单管理', './menu.html');

-- ----------------------------
-- Table structure for user_menu
-- ----------------------------
DROP TABLE IF EXISTS `user_menu`;
CREATE TABLE `user_menu`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mail` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户邮箱',
  `menu_id` int(11) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_menu
-- ----------------------------
INSERT INTO `user_menu` VALUES (1, 'jeff.cao@aoliday.com', 1);
INSERT INTO `user_menu` VALUES (2, 'jeff.cao@aoliday.com', 2);
INSERT INTO `user_menu` VALUES (3, 'jeff.cao@aoliday.com', 6);
INSERT INTO `user_menu` VALUES (4, 'jeff.cao@aoliday.com', 5);
