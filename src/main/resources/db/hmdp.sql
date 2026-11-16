/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 50622
 Source Host           : localhost:3306
 Source Schema         : hmdp

 Target Server Type    : MySQL
 Target Server Version : 50622
 File Encoding         : 65001

 Date: 14/03/2022 21:38:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_blog
-- ----------------------------
DROP TABLE IF EXISTS `tb_blog`;
CREATE TABLE `tb_blog`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` bigint(20) NOT NULL COMMENT '商户id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `images` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '探店的照片，最多9张，多张以\",\"隔开',
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '探店的文字描述',
  `liked` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '点赞数量',
  `comments` int(8) UNSIGNED NULL DEFAULT NULL COMMENT '评论数量',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_blog
-- ----------------------------
INSERT INTO `tb_blog` VALUES (4, 4, 2, '无尽浪漫的夜晚丨在万花丛中摇晃着红酒杯🍷品战斧牛排🥩', '/imgs/blogs/7/14/4771fefb-1a87-4252-816c-9f7ec41ffa4a.jpg,/imgs/blogs/4/10/2f07e3c9-ddce-482d-9ea7-c21450f8d7cd.jpg,/imgs/blogs/2/6/b0756279-65da-4f2d-b62a-33f74b06454a.jpg,/imgs/blogs/10/7/7e97f47d-eb49-4dc9-a583-95faa7aed287.jpg,/imgs/blogs/1/2/4a7b496b-2a08-4af7-aa95-df2c3bd0ef97.jpg,/imgs/blogs/14/3/52b290eb-8b5d-403b-8373-ba0bb856d18e.jpg', '生活就是一半烟火·一半诗意<br/>手执烟火谋生活·心怀诗意以谋爱·<br/>当然<br/>\r\n男朋友给不了的浪漫要学会自己给🍒<br/>\n无法重来的一生·尽量快乐.<br/><br/>🏰「小筑里·神秘浪漫花园餐厅」🏰<br/><br/>\n💯这是一家最最最美花园的西餐厅·到处都是花餐桌上是花前台是花  美好无处不在\n品一口葡萄酒，维亚红酒马瑟兰·微醺上头工作的疲惫消失无际·生如此多娇🍃<br/><br/>📍地址:延安路200号(家乐福面)<br/><br/>🚌交通:地铁①号线定安路B口出右转过下通道右转就到啦～<br/><br/>--------------🥣菜品详情🥣---------------<br/><br/>「战斧牛排]<br/>\n超大一块战斧牛排经过火焰的炙烤发出阵阵香，外焦里嫩让人垂涎欲滴，切开牛排的那一刻，牛排的汁水顺势流了出来，分熟的牛排肉质软，简直细嫩到犯规，一刻都等不了要放入嘴里咀嚼～<br/><br/>「奶油培根意面」<br/>太太太好吃了💯<br/>我真的无法形容它的美妙，意面混合奶油香菇的香味真的太太太香了，我真的舔盘了，一丁点美味都不想浪费‼️<br/><br/><br/>「香菜汁烤鲈鱼」<br/>这个酱是辣的 真的绝好吃‼️<br/>鲈鱼本身就很嫩没什么刺，烤过之后外皮酥酥的，鱼肉蘸上酱料根本停不下来啊啊啊啊<br/>能吃辣椒的小伙伴一定要尝尝<br/><br/>非常可 好吃子🍽\n<br/>--------------🍃个人感受🍃---------------<br/><br/>【👩🏻‍🍳服务】<br/>小姐姐特别耐心的给我们介绍彩票 <br/>推荐特色菜品，拍照需要帮忙也是尽心尽力配合，太爱他们了<br/><br/>【🍃环境】<br/>比较有格调的西餐厅 整个餐厅的布局可称得上的万花丛生 有种在人间仙境的感觉🌸<br/>集美食美酒与鲜花为一体的风格店铺 令人向往<br/>烟火皆是生活 人间皆是浪漫<br/>', 1, 104, '2021-12-28 19:50:01', '2022-03-10 14:26:34');
INSERT INTO `tb_blog` VALUES (5, 1, 2, '人均30💰杭州这家港式茶餐厅我疯狂打call‼️', '/imgs/blogs/4/7/863cc302-d150-420d-a596-b16e9232a1a6.jpg,/imgs/blogs/11/12/8b37d208-9414-4e78-b065-9199647bb3e3.jpg,/imgs/blogs/4/1/fa74a6d6-3026-4cb7-b0b6-35abb1e52d11.jpg,/imgs/blogs/9/12/ac2ce2fb-0605-4f14-82cc-c962b8c86688.jpg,/imgs/blogs/4/0/26a7cd7e-6320-432c-a0b4-1b7418f45ec7.jpg,/imgs/blogs/15/9/cea51d9b-ac15-49f6-b9f1-9cf81e9b9c85.jpg', '又吃到一家好吃的茶餐厅🍴环境是怀旧tvb港风📺边吃边拍照片📷几十种菜品均价都在20+💰可以是很平价了！<br>·<br>店名：九记冰厅(远洋店)<br>地址：杭州市丽水路远洋乐堤港负一楼（溜冰场旁边）<br>·<br>✔️黯然销魂饭（38💰）<br>这碗饭我吹爆！米饭上盖满了甜甜的叉烧 还有两颗溏心蛋🍳每一粒米饭都裹着浓郁的酱汁 光盘了<br>·<br>✔️铜锣湾漏奶华（28💰）<br>黄油吐司烤的脆脆的 上面洒满了可可粉🍫一刀切开 奶盖流心像瀑布一样流出来  满足<br>·<br>✔️神仙一口西多士士（16💰）<br>简简单单却超级好吃！西多士烤的很脆 黄油味浓郁 面包体超级柔软 上面淋了炼乳<br>·<br>✔️怀旧五柳炸蛋饭（28💰）<br>四个鸡蛋炸成蓬松的炸蛋！也太好吃了吧！还有大块鸡排 上淋了酸甜的酱汁 太合我胃口了！！<br>·<br>✔️烧味双拼例牌（66💰）<br>选了烧鹅➕叉烧 他家烧腊品质真的惊艳到我！据说是每日广州发货 到店现烧现卖的黑棕鹅 每口都是正宗的味道！肉质很嫩 皮超级超级酥脆！一口爆油！叉烧肉也一点都不柴 甜甜的很入味 搭配梅子酱很解腻 ！<br>·<br>✔️红烧脆皮乳鸽（18.8💰）<br>乳鸽很大只 这个价格也太划算了吧， 肉质很有嚼劲 脆皮很酥 越吃越香～<br>·<br>✔️大满足小吃拼盘（25💰）<br>翅尖➕咖喱鱼蛋➕蝴蝶虾➕盐酥鸡<br>zui喜欢里面的咖喱鱼！咖喱酱香甜浓郁！鱼蛋很q弹～<br>·<br>✔️港式熊仔丝袜奶茶（19💰）<br>小熊🐻造型的奶茶冰也太可爱了！颜值担当 很地道的丝袜奶茶 茶味特别浓郁～<br>·', 1, 0, '2021-12-28 20:57:49', '2022-03-10 09:21:39');
INSERT INTO `tb_blog` VALUES (6, 10, 1, '杭州周末好去处｜💰50就可以骑马啦🐎', '/imgs/blogs/blog1.jpg', '杭州周末好去处｜💰50就可以骑马啦🐎', 1, 0, '2022-01-11 16:05:47', '2022-03-10 09:21:41');
INSERT INTO `tb_blog` VALUES (7, 10, 1, '杭州周末好去处｜💰50就可以骑马啦🐎', '/imgs/blogs/blog1.jpg', '杭州周末好去处｜💰50就可以骑马啦🐎', 1, 0, '2022-01-11 16:05:47', '2022-03-10 09:21:42');

-- ----------------------------
-- Table structure for tb_blog_comments
-- ----------------------------
DROP TABLE IF EXISTS `tb_blog_comments`;
CREATE TABLE `tb_blog_comments`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `blog_id` bigint(20) UNSIGNED NOT NULL COMMENT '探店id',
  `parent_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的1级评论id，如果是一级评论，则值为0',
  `answer_id` bigint(20) UNSIGNED NOT NULL COMMENT '回复的评论id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回复的内容',
  `liked` int(8) UNSIGNED NULL DEFAULT NULL COMMENT '点赞数',
  `status` tinyint(1) UNSIGNED NULL DEFAULT NULL COMMENT '状态，0：正常，1：被举报，2：禁止查看',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_blog_comments
-- ----------------------------

-- ----------------------------
-- Table structure for tb_follow
-- ----------------------------
DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `follow_user_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的用户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_follow
-- ----------------------------

-- ----------------------------
-- Table structure for tb_seckill_voucher
-- ----------------------------
DROP TABLE IF EXISTS `tb_seckill_voucher`;
CREATE TABLE `tb_seckill_voucher`  (
  `voucher_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的优惠券的id',
  `stock` int(8) NOT NULL COMMENT '库存',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `begin_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '生效时间',
  `end_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '失效时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`voucher_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '秒杀优惠券表，与优惠券是一对一关系' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_seckill_voucher
-- ----------------------------

-- ----------------------------
-- Table structure for tb_shop
-- ----------------------------
DROP TABLE IF EXISTS `tb_shop`;
CREATE TABLE `tb_shop`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商铺名称',
  `type_id` bigint(20) UNSIGNED NOT NULL COMMENT '商铺类型的id',
  `images` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商铺图片，多个图片以\',\'隔开',
  `area` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商圈，例如陆家嘴',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地址',
  `x` double UNSIGNED NOT NULL COMMENT '经度',
  `y` double UNSIGNED NOT NULL COMMENT '维度',
  `avg_price` bigint(10) UNSIGNED NULL DEFAULT NULL COMMENT '均价，取整数',
  `sold` int(10) UNSIGNED ZEROFILL NOT NULL COMMENT '销量',
  `comments` int(10) UNSIGNED ZEROFILL NOT NULL COMMENT '评论数量',
  `score` int(2) UNSIGNED ZEROFILL NOT NULL COMMENT '评分，1~5分，乘10保存，避免小数',
  `open_hours` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '营业时间，例如 10:00-22:00',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `foreign_key_type`(`type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_shop
-- ----------------------------
INSERT INTO `tb_shop` VALUES (1, '103茶餐厅', 1, 'https://qcloud.dpfile.com/pc/jiclIsCKmOI2arxKN1Uf0Hx3PucIJH8q0QSz-Z8llzcN56-_QiKuOvyio1OOxsRtFoXqu0G3iT2T27qat3WhLVEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vfCF2ubeXzk49OsGrXt_KYDCngOyCwZK-s3fqawWswzk.jpg,https://qcloud.dpfile.com/pc/IOf6VX3qaBgFXFVgp75w-KKJmWZjFc8GXDU8g9bQC6YGCpAmG00QbfT4vCCBj7njuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '大关', '金华路锦昌文华苑29号', 120.149192, 30.316078, 80, 0000004215, 0000003035, 37, '10:00-22:00', '2021-12-22 18:10:39', '2022-01-13 17:32:19');
INSERT INTO `tb_shop` VALUES (2, '蔡馬洪涛烤肉·老北京铜锅涮羊肉', 1, 'https://p0.meituan.net/bbia/c1870d570e73accbc9fee90b48faca41195272.jpg,http://p0.meituan.net/mogu/397e40c28fc87715b3d5435710a9f88d706914.jpg,https://qcloud.dpfile.com/pc/MZTdRDqCZdbPDUO0Hk6lZENRKzpKRF7kavrkEI99OxqBZTzPfIxa5E33gBfGouhFuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '拱宸桥/上塘', '上塘路1035号（中国工商银行旁）', 120.151505, 30.333422, 85, 0000002160, 0000001460, 46, '11:30-03:00', '2021-12-22 19:00:13', '2022-01-11 16:12:26');
INSERT INTO `tb_shop` VALUES (3, '新白鹿餐厅(运河上街店)', 1, 'https://p0.meituan.net/biztone/694233_1619500156517.jpeg,https://img.meituan.net/msmerchant/876ca8983f7395556eda9ceb064e6bc51840883.png,https://img.meituan.net/msmerchant/86a76ed53c28eff709a36099aefe28b51554088.png', '运河上街', '台州路2号运河上街购物中心F5', 120.151954, 30.32497, 61, 0000012035, 0000008045, 47, '10:30-21:00', '2021-12-22 19:10:05', '2022-01-11 16:12:42');
INSERT INTO `tb_shop` VALUES (4, 'Mamala(杭州远洋乐堤港店)', 1, 'https://img.meituan.net/msmerchant/232f8fdf09050838bd33fb24e79f30f9606056.jpg,https://qcloud.dpfile.com/pc/rDe48Xe15nQOHCcEEkmKUp5wEKWbimt-HDeqYRWsYJseXNncvMiXbuED7x1tXqN4uzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '拱宸桥/上塘', '丽水路66号远洋乐堤港商城2期1层B115号', 120.146659, 30.312742, 290, 0000013519, 0000009529, 49, '11:00-22:00', '2021-12-22 19:17:15', '2022-01-11 16:12:51');
INSERT INTO `tb_shop` VALUES (5, '海底捞火锅(水晶城购物中心店）', 1, 'https://img.meituan.net/msmerchant/054b5de0ba0b50c18a620cc37482129a45739.jpg,https://img.meituan.net/msmerchant/59b7eff9b60908d52bd4aea9ff356e6d145920.jpg,https://qcloud.dpfile.com/pc/Qe2PTEuvtJ5skpUXKKoW9OQ20qc7nIpHYEqJGBStJx0mpoyeBPQOJE4vOdYZwm9AuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '大关', '上塘路458号水晶城购物中心F6', 120.15778, 30.310633, 104, 0000004125, 0000002764, 49, '10:00-07:00', '2021-12-22 19:20:58', '2022-01-11 16:13:01');
INSERT INTO `tb_shop` VALUES (6, '幸福里老北京涮锅（丝联店）', 1, 'https://img.meituan.net/msmerchant/e71a2d0d693b3033c15522c43e03f09198239.jpg,https://img.meituan.net/msmerchant/9f8a966d60ffba00daf35458522273ca658239.jpg,https://img.meituan.net/msmerchant/ef9ca5ef6c05d381946fe4a9aa7d9808554502.jpg', '拱宸桥/上塘', '金华南路189号丝联166号', 120.148603, 30.318618, 130, 0000009531, 0000007324, 46, '11:00-13:50,17:00-20:50', '2021-12-22 19:24:53', '2022-01-11 16:13:09');
INSERT INTO `tb_shop` VALUES (7, '炉鱼(拱墅万达广场店)', 1, 'https://img.meituan.net/msmerchant/909434939a49b36f340523232924402166854.jpg,https://img.meituan.net/msmerchant/32fd2425f12e27db0160e837461c10303700032.jpg,https://img.meituan.net/msmerchant/f7022258ccb8dabef62a0514d3129562871160.jpg', '北部新城', '杭行路666号万达商业中心4幢2单元409室(铺位号4005)', 120.124691, 30.336819, 85, 0000002631, 0000001320, 47, '00:00-24:00', '2021-12-22 19:40:52', '2022-01-11 16:13:19');
INSERT INTO `tb_shop` VALUES (8, '浅草屋寿司（运河上街店）', 1, 'https://img.meituan.net/msmerchant/cf3dff697bf7f6e11f4b79c4e7d989e4591290.jpg,https://img.meituan.net/msmerchant/0b463f545355c8d8f021eb2987dcd0c8567811.jpg,https://img.meituan.net/msmerchant/c3c2516939efaf36c4ccc64b0e629fad587907.jpg', '运河上街', '拱墅区金华路80号运河上街B1', 120.150526, 30.325231, 88, 0000002406, 0000001206, 46, ' 11:00-21:30', '2021-12-22 19:51:06', '2022-01-11 16:13:25');
INSERT INTO `tb_shop` VALUES (9, '羊老三羊蝎子牛仔排北派炭火锅(运河上街店)', 1, 'https://p0.meituan.net/biztone/163160492_1624251899456.jpeg,https://img.meituan.net/msmerchant/e478eb16f7e31a7f8b29b5e3bab6de205500837.jpg,https://img.meituan.net/msmerchant/6173eb1d18b9d70ace7fdb3f2dd939662884857.jpg', '运河上街', '台州路2号运河上街购物中心F5', 120.150598, 30.325251, 101, 0000002763, 0000001363, 44, '11:00-21:30', '2021-12-22 19:53:59', '2022-01-11 16:13:34');
INSERT INTO `tb_shop` VALUES (10, '开乐迪KTV（运河上街店）', 2, 'https://p0.meituan.net/joymerchant/a575fd4adb0b9099c5c410058148b307-674435191.jpg,https://p0.meituan.net/merchantpic/68f11bf850e25e437c5f67decfd694ab2541634.jpg,https://p0.meituan.net/dpdeal/cb3a12225860ba2875e4ea26c6d14fcc197016.jpg', '运河上街', '台州路2号运河上街购物中心F4', 120.149093, 30.324666, 67, 0000026891, 0000000902, 37, '00:00-24:00', '2021-12-22 20:25:16', '2021-12-22 20:25:16');
INSERT INTO `tb_shop` VALUES (11, 'INLOVE KTV(水晶城店)', 2, 'https://p0.meituan.net/dpmerchantpic/53e74b200211d68988a4f02ae9912c6c1076826.jpg,https://qcloud.dpfile.com/pc/4iWtIvzLzwM2MGgyPu1PCDb4SWEaKqUeHm--YAt1EwR5tn8kypBcqNwHnjg96EvT_Gd2X_f-v9T8Yj4uLt25Gg.jpg,https://qcloud.dpfile.com/pc/WZsJWRI447x1VG2x48Ujgu7vwqksi_9WitdKI4j3jvIgX4MZOpGNaFtM93oSSizbGybIjx5eX6WNgCPvcASYAw.jpg', '水晶城', '上塘路458号水晶城购物中心6层', 120.15853, 30.310002, 75, 0000035977, 0000005684, 47, '11:30-06:00', '2021-12-22 20:29:02', '2021-12-22 20:39:00');
INSERT INTO `tb_shop` VALUES (12, '魅(杭州远洋乐堤港店)', 2, 'https://p0.meituan.net/dpmerchantpic/63833f6ba0393e2e8722420ef33f3d40466664.jpg,https://p0.meituan.net/dpmerchantpic/ae3c94cc92c529c4b1d7f68cebed33fa105810.png,', '远洋乐堤港', '丽水路58号远洋乐堤港F4', 120.14983, 30.31211, 88, 0000006444, 0000000235, 46, '10:00-02:00', '2021-12-22 20:34:34', '2021-12-22 20:34:34');
INSERT INTO `tb_shop` VALUES (13, '讴K拉量贩KTV(北城天地店)', 2, 'https://p1.meituan.net/merchantpic/598c83a8c0d06fe79ca01056e214d345875600.jpg,https://qcloud.dpfile.com/pc/HhvI0YyocYHRfGwJWqPQr34hRGRl4cWdvlNwn3dqghvi4WXlM2FY1te0-7pE3Wb9_Gd2X_f-v9T8Yj4uLt25Gg.jpg,https://qcloud.dpfile.com/pc/F5ZVzZaXFE27kvQzPnaL4V8O9QCpVw2nkzGrxZE8BqXgkfyTpNExfNG5CEPQX4pjGybIjx5eX6WNgCPvcASYAw.jpg', 'D32天阳购物中心', '湖州街567号北城天地5层', 120.130453, 30.327655, 58, 0000018997, 0000001857, 41, '12:00-02:00', '2021-12-22 20:38:54', '2021-12-22 20:40:04');
INSERT INTO `tb_shop` VALUES (14, '星聚会KTV(拱墅区万达店)', 2, 'https://p0.meituan.net/dpmerchantpic/f4cd6d8d4eb1959c3ea826aa05a552c01840451.jpg,https://p0.meituan.net/dpmerchantpic/2efc07aed856a8ab0fc75c86f4b9b0061655777.jpg,https://qcloud.dpfile.com/pc/zWfzzIorCohKT0bFwsfAlHuayWjI6DBEMPHHncmz36EEMU9f48PuD9VxLLDAjdoU_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '北部新城', '杭行路666号万达广场C座1-2F', 120.128958, 30.337252, 60, 0000017771, 0000000685, 47, '10:00-22:00', '2021-12-22 20:48:54', '2021-12-22 20:48:54');

-- ----------------------------
-- Table structure for tb_shop_type
-- ----------------------------
DROP TABLE IF EXISTS `tb_shop_type`;
CREATE TABLE `tb_shop_type`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型名称',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int(3) UNSIGNED NULL DEFAULT NULL COMMENT '顺序',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_shop_type
-- ----------------------------
INSERT INTO `tb_shop_type` VALUES (1, '美食', '/types/ms.png', 1, '2021-12-22 20:17:47', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (2, 'KTV', '/types/KTV.png', 2, '2021-12-22 20:18:27', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (3, '丽人·美发', '/types/lrmf.png', 3, '2021-12-22 20:18:48', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (4, '健身运动', '/types/jsyd.png', 10, '2021-12-22 20:19:04', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (5, '按摩·足疗', '/types/amzl.png', 5, '2021-12-22 20:19:27', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (6, '美容SPA', '/types/spa.png', 6, '2021-12-22 20:19:35', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (7, '亲子游乐', '/types/qzyl.png', 7, '2021-12-22 20:19:53', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (8, '酒吧', '/types/jiuba.png', 8, '2021-12-22 20:20:02', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (9, '轰趴馆', '/types/hpg.png', 9, '2021-12-22 20:20:08', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (10, '美睫·美甲', '/types/mjmj.png', 4, '2021-12-22 20:21:46', '2021-12-23 11:24:31');

-- ----------------------------
-- Table structure for tb_sign
-- ----------------------------
DROP TABLE IF EXISTS `tb_sign`;
CREATE TABLE `tb_sign`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `year` year NOT NULL COMMENT '签到的年',
  `month` tinyint(2) NOT NULL COMMENT '签到的月',
  `date` date NOT NULL COMMENT '签到的日期',
  `is_backup` tinyint(1) UNSIGNED NULL DEFAULT NULL COMMENT '是否补签',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_sign
-- ----------------------------

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号码',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '密码，加密存储',
  `nick_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '昵称，默认是用户id',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '人物头像',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniqe_key_phone`(`phone`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1010 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_user
-- ----------------------------
INSERT INTO `tb_user` VALUES (1, '13800000004', '', '小鱼同学', '/imgs/blogs/blog1.jpg', '2021-12-24 10:27:19', '2022-01-11 16:04:00');
INSERT INTO `tb_user` VALUES (2, '13800001005', '', '可可今天不吃肉', '/imgs/icons/kkjtbcr.jpg', '2021-12-24 15:14:39', '2021-12-28 19:58:04');
INSERT INTO `tb_user` VALUES (4, '13800000003', '', 'user_slxaxy2au9f3tanffaxr', '', '2022-01-07 12:07:53', '2022-01-07 12:07:53');
INSERT INTO `tb_user` VALUES (5, '13800000002', '', '可爱多', '/imgs/icons/user5-icon.png', '2022-01-07 16:11:33', '2022-03-11 09:09:20');
INSERT INTO `tb_user` VALUES (6, '13800000001', '', 'user_xn5wr3hpsv', '', '2022-02-07 17:54:10', '2022-02-07 17:54:10');
INSERT INTO `tb_user` VALUES (10, '13800000005', '', 'user_88arndojw9', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (11, '13800000006', '', 'user_qcfr2k1lmi', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (12, '13800000007', '', 'user_ffsk4hli07', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (13, '13800000008', '', 'user_r62q62ijef', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (14, '13800000009', '', 'user_f3rymyt1q5', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (15, '13800000010', '', 'user_hnyhc3mjat', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (16, '13800000011', '', 'user_2spo35f5rl', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (17, '13800000012', '', 'user_q3r70baqe1', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (18, '13800000013', '', 'user_v73ottjqxt', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (19, '13800000014', '', 'user_tmh8o4r11q', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (20, '13800000015', '', 'user_4epgb7b5u1', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (21, '13800000016', '', 'user_g474zoujxj', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (22, '13800000017', '', 'user_r3kh1g6aah', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (23, '13800000018', '', 'user_u3uuo7l5fo', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (24, '13800000019', '', 'user_9o93lbsojt', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (25, '13800000020', '', 'user_jbhmr43wpq', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (26, '13800000021', '', 'user_nevyd3c5ux', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (27, '13800000022', '', 'user_oow4frmjp3', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (28, '13800000023', '', 'user_cvmknmec74', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (29, '13800000024', '', 'user_0t2x5njbz7', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (30, '13800000025', '', 'user_y5x09783hp', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (31, '13800000026', '', 'user_owe4eyuhhh', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (32, '13800000027', '', 'user_j76auh0ggg', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (33, '13800000028', '', 'user_aal5w9rm33', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (34, '13800000029', '', 'user_a2pgu8cr21', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (35, '13800000030', '', 'user_nle60p846v', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (36, '13800000031', '', 'user_w1mck7c7yv', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (37, '13800000032', '', 'user_bnpiybumlk', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (38, '13800000033', '', 'user_4w7xeo2yyt', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (39, '13800000034', '', 'user_99u4voj7xl', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (40, '13800000035', '', 'user_g03is27pd6', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (41, '13800000036', '', 'user_3j9erfkl0p', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (42, '13800000037', '', 'user_l7rs56ah9y', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (43, '13800000038', '', 'user_p3655ctliy', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (44, '13800000039', '', 'user_qi1qze1yp1', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (45, '13800000040', '', 'user_vrd5ir0rj0', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (46, '13800000041', '', 'user_tubboh1byc', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (47, '13800000042', '', 'user_j2bdj3d2eo', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (48, '13800000043', '', 'user_ncj7r0vu1h', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (49, '13800000044', '', 'user_63rhqjqa0a', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (50, '13800000045', '', 'user_80ue5cywnk', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (51, '13800000046', '', 'user_j4q037vhpi', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (52, '13800000047', '', 'user_ms0uat5bf0', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (53, '13800000048', '', 'user_oqep16bdel', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (54, '13800000049', '', 'user_vjtvjjdqh7', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (55, '13800000050', '', 'user_0168i9hv5g', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (56, '13800000051', '', 'user_vh1j6zw1q4', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (57, '13800000052', '', 'user_rkf2nxouof', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (58, '13800000053', '', 'user_whlt2chtv3', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (59, '13800000054', '', 'user_lpqr90wbeo', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (60, '13800000055', '', 'user_h40y3ipk9k', '', '2022-02-28 10:50:47', '2022-02-28 10:50:47');
INSERT INTO `tb_user` VALUES (61, '13800000056', '', 'user_awdqkmbkt7', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (62, '13800000057', '', 'user_1xgbg9v4r5', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (63, '13800000058', '', 'user_7vf5fgiu68', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (64, '13800000059', '', 'user_lsgiz015vf', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (65, '13800000060', '', 'user_0nqjvanruk', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (66, '13800000061', '', 'user_8alg1taath', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (67, '13800000062', '', 'user_q45ykjgpxe', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (68, '13800000063', '', 'user_4hy0o6ir0r', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (69, '13800000064', '', 'user_q6rh7e6zo9', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (70, '13800000065', '', 'user_1wp3ygfyn2', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (71, '13800000066', '', 'user_13vjvo6flp', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (72, '13800000067', '', 'user_glyshbbwin', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (73, '13800000068', '', 'user_3ewzgsnhzj', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (74, '13800000069', '', 'user_ky481zf1fs', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (75, '13800000070', '', 'user_o5yzu0epev', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (76, '13800000071', '', 'user_ycbracmsi3', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (77, '13800000072', '', 'user_974wwi1283', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (78, '13800000073', '', 'user_1y0xokmk9w', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (79, '13800000074', '', 'user_nd74cho3tu', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (80, '13800000075', '', 'user_5z7u2eysa4', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (81, '13800000076', '', 'user_yvf8hfu5yy', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (82, '13800000077', '', 'user_2poi4wvpms', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (83, '13800000078', '', 'user_v4ysxjt1yu', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (84, '13800000079', '', 'user_kbvn4gpgk6', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (85, '13800000080', '', 'user_23niik1tyg', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (86, '13800000081', '', 'user_uf2zz6ispe', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (87, '13800000082', '', 'user_5k19vf7c4o', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (88, '13800000083', '', 'user_5ahdd98xbr', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (89, '13800000084', '', 'user_a5cnfnoopx', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (90, '13800000085', '', 'user_utnmcyfg13', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (91, '13800000086', '', 'user_0k6n8ikb95', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (92, '13800000087', '', 'user_zqk5maqtmi', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (93, '13800000088', '', 'user_9i9suwd3nd', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (94, '13800000089', '', 'user_u0y0ngrdjo', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (95, '13800000090', '', 'user_stvijjwvzu', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (96, '13800000091', '', 'user_7if7tttays', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (97, '13800000092', '', 'user_f9hmz0ngdu', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (98, '13800000093', '', 'user_wuhs2nq9d0', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (99, '13800000094', '', 'user_1u3rlntd5s', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (100, '13800000095', '', 'user_ywe62vhu7u', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (101, '13800000096', '', 'user_rbfpzdt6tg', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (102, '13800000097', '', 'user_jv69l0d1kg', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (103, '13800000098', '', 'user_dg6hwl48r6', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (104, '13800000099', '', 'user_8rwl92pixr', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (105, '13800000100', '', 'user_k5dek2os3m', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (106, '13800000101', '', 'user_kw1nr2scyz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (107, '13800000102', '', 'user_h81c5g0t7s', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (108, '13800000103', '', 'user_jar6djas5e', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (109, '13800000104', '', 'user_f9x2qm4grh', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (110, '13800000105', '', 'user_mg5h6c4bcu', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (111, '13800000106', '', 'user_hcg7ocbnus', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (112, '13800000107', '', 'user_rsbxx7g2yz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (113, '13800000108', '', 'user_bi3fhutbd1', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (114, '13800000109', '', 'user_o4pjkkyu3q', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (115, '13800000110', '', 'user_7zfs7g5vqb', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (116, '13800000111', '', 'user_oq71inq541', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (117, '13800000112', '', 'user_u9zoiadq6l', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (118, '13800000113', '', 'user_4elguvu5pz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (119, '13800000114', '', 'user_90dclmdv94', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (120, '13800000115', '', 'user_v2of3k1liq', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (121, '13800000116', '', 'user_bg04b99iqn', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (122, '13800000117', '', 'user_6fo9sul3q6', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (123, '13800000118', '', 'user_vl7ajjlhnn', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (124, '13800000119', '', 'user_df3kaj5zi1', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (125, '13800000120', '', 'user_yo6iohe5s2', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (126, '13800000121', '', 'user_2iss3wrcm1', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (127, '13800000122', '', 'user_7y8qm8sv4r', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (128, '13800000123', '', 'user_f825rhknpq', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (129, '13800000124', '', 'user_3bigm0aauz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (130, '13800000125', '', 'user_ib9eo5dtgk', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (131, '13800000126', '', 'user_ci5hhnufj9', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (132, '13800000127', '', 'user_cc56u62rny', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (133, '13800000128', '', 'user_i8fg7azvnt', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (134, '13800000129', '', 'user_bw5dqkv6d9', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (135, '13800000130', '', 'user_1n1o9ri5oz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (136, '13800000131', '', 'user_1b0zexoldy', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (137, '13800000132', '', 'user_lz9dr6wxkw', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (138, '13800000133', '', 'user_zfpfscu53e', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (139, '13800000134', '', 'user_5kldn59nn9', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (140, '13800000135', '', 'user_p0mjswjh9x', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (141, '13800000136', '', 'user_z4jcqggm11', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (142, '13800000137', '', 'user_pv9yctbxza', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (143, '13800000138', '', 'user_u702tikvol', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (144, '13800000139', '', 'user_sy4a5f3qmy', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (145, '13800000140', '', 'user_n6g293r60l', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (146, '13800000141', '', 'user_uylyp6ttqz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (147, '13800000142', '', 'user_h2zmzefha3', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (148, '13800000143', '', 'user_5outop6hz2', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (149, '13800000144', '', 'user_vp8l74sadq', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (150, '13800000145', '', 'user_n9ww3of8fg', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (151, '13800000146', '', 'user_rfm7pfgkv8', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (152, '13800000147', '', 'user_h7298xuo0u', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (153, '13800000148', '', 'user_72s0smb2wz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (154, '13800000149', '', 'user_twhphih9nd', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (155, '13800000150', '', 'user_vfocakn5gl', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (156, '13800000151', '', 'user_tfwe1v2x82', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (157, '13800000152', '', 'user_eyrq375qgg', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (158, '13800000153', '', 'user_rg2obilrot', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (159, '13800000154', '', 'user_rzfwln2aw2', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (160, '13800000155', '', 'user_rzfgzeshe1', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (161, '13800000156', '', 'user_c67s0sjbmv', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (162, '13800000157', '', 'user_fkyp652kkn', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (163, '13800000158', '', 'user_sv1i552okx', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (164, '13800000159', '', 'user_fsrmh6e0d3', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (165, '13800000160', '', 'user_jey7gkjesn', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (166, '13800000161', '', 'user_00xdq55r0f', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (167, '13800000162', '', 'user_wkb6tveg4e', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (168, '13800000163', '', 'user_51ong6aunx', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (169, '13800000164', '', 'user_ke4h0uxthm', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (170, '13800000165', '', 'user_oswyb9opx5', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (171, '13800000166', '', 'user_iy8itbwg6q', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (172, '13800000167', '', 'user_g1mk023p9r', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (173, '13800000168', '', 'user_2507p7kvzs', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (174, '13800000169', '', 'user_piixbanfxl', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (175, '13800000170', '', 'user_w2d2is43jc', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (176, '13800000171', '', 'user_lrk4it56lt', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (177, '13800000172', '', 'user_3273q3j2ft', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (178, '13800000173', '', 'user_ltf42q0vy4', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (179, '13800000174', '', 'user_7npp13snqp', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (180, '13800000175', '', 'user_slxftqmjp9', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (181, '13800000176', '', 'user_il4dsuvdfr', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (182, '13800000177', '', 'user_esm2d4uh7a', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (183, '13800000178', '', 'user_te4prs2y3j', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (184, '13800000179', '', 'user_dycwcufgj0', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (185, '13800000180', '', 'user_jjo4dvsgag', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (186, '13800000181', '', 'user_9opl0t1sd2', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (187, '13800000182', '', 'user_hbm1dnssq6', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (188, '13800000183', '', 'user_tx88zar5cs', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (189, '13800000184', '', 'user_1p206nyupm', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (190, '13800000185', '', 'user_8lnbd2ao5u', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (191, '13800000186', '', 'user_v4uwls1wg7', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (192, '13800000187', '', 'user_z1bkh9lnjj', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (193, '13800000188', '', 'user_r7u852ex1n', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (194, '13800000189', '', 'user_yqr54gdh8t', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (195, '13800000190', '', 'user_x4kngjjng7', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (196, '13800000191', '', 'user_xvtxjocno2', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (197, '13800000192', '', 'user_1zj77q03tf', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (198, '13800000193', '', 'user_0q8yjtlp7a', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (199, '13800000194', '', 'user_nt2ogdyl73', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (200, '13800000195', '', 'user_6w0ex6afsz', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (201, '13800000196', '', 'user_qpm2vyflc3', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (202, '13800000197', '', 'user_dzvf9xujj1', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (203, '13800000198', '', 'user_aiypeaeupd', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (204, '13800000199', '', 'user_5iznj0t5hg', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (205, '13800000200', '', 'user_4t1flvqz2h', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (206, '13800000201', '', 'user_542t36rd41', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (207, '13800000202', '', 'user_kmhowbydu6', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (208, '13800000203', '', 'user_e8nz64jbym', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (209, '13800000204', '', 'user_zjbr3zq6we', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (210, '13800000205', '', 'user_ptk6qaspna', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (211, '13800000206', '', 'user_jei7tqtu1j', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (212, '13800000207', '', 'user_8x7cyv5ey8', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (213, '13800000208', '', 'user_mx3l4tj2jq', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (214, '13800000209', '', 'user_ba6e9l6exg', '', '2022-02-28 10:50:48', '2022-02-28 10:50:48');
INSERT INTO `tb_user` VALUES (215, '13800000210', '', 'user_vlku3rop6e', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (216, '13800000211', '', 'user_hsff9net6q', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (217, '13800000212', '', 'user_mbaficnzfe', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (218, '13800000213', '', 'user_b3wglh40dk', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (219, '13800000214', '', 'user_dvi1yllk48', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (220, '13800000215', '', 'user_cxv8smu642', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (221, '13800000216', '', 'user_ze5exti1z5', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (222, '13800000217', '', 'user_b6524nuosz', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (223, '13800000218', '', 'user_jw3xmz31ss', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (224, '13800000219', '', 'user_3fqrglyqj0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (225, '13800000220', '', 'user_uf9dy1kfmg', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (226, '13800000221', '', 'user_nn4ss4oupv', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (227, '13800000222', '', 'user_khse5vlch8', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (228, '13800000223', '', 'user_xfpqrk3hea', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (229, '13800000224', '', 'user_0sfyf9o74l', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (230, '13800000225', '', 'user_936vlojcwy', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (231, '13800000226', '', 'user_wq9m8aqmay', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (232, '13800000227', '', 'user_uqw0c5ilp5', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (233, '13800000228', '', 'user_qjucgt4ar1', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (234, '13800000229', '', 'user_sry5bqf8ev', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (235, '13800000230', '', 'user_9csdwveeh8', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (236, '13800000231', '', 'user_y9kl1yd7fk', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (237, '13800000232', '', 'user_mahwf66irm', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (238, '13800000233', '', 'user_tud2i4itlr', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (239, '13800000234', '', 'user_p1s640kfny', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (240, '13800000235', '', 'user_2tyzfj49r6', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (241, '13800000236', '', 'user_wjswilvpou', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (242, '13800000237', '', 'user_yuushg7x44', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (243, '13800000238', '', 'user_pb0fas6tob', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (244, '13800000239', '', 'user_3k4nn4dhuh', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (245, '13800000240', '', 'user_lljtt3ttml', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (246, '13800000241', '', 'user_weftqlsasg', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (247, '13800000242', '', 'user_1lo78exvpl', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (248, '13800000243', '', 'user_qzbukb32cm', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (249, '13800000244', '', 'user_k80i5kfvoj', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (250, '13800000245', '', 'user_ggx53ve2zp', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (251, '13800000246', '', 'user_yz0fmlzjxv', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (252, '13800000247', '', 'user_jh6epyjgiw', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (253, '13800000248', '', 'user_0k0ly383fy', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (254, '13800000249', '', 'user_ibc8pgs2jc', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (255, '13800000250', '', 'user_ys8yxdm6cr', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (256, '13800000251', '', 'user_7zdagyyymi', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (257, '13800000252', '', 'user_9q7fiiqwzm', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (258, '13800000253', '', 'user_64qzvesiku', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (259, '13800000254', '', 'user_5fi8fsc9e5', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (260, '13800000255', '', 'user_1wo4aktp89', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (261, '13800000256', '', 'user_5mis2rucuh', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (262, '13800000257', '', 'user_pghkcw4cog', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (263, '13800000258', '', 'user_ymh7a5t69k', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (264, '13800000259', '', 'user_58qypl26r3', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (265, '13800000260', '', 'user_oknszihfil', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (266, '13800000261', '', 'user_rx5qu0277b', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (267, '13800000262', '', 'user_4mwekx3q8z', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (268, '13800000263', '', 'user_ie9qzfwu27', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (269, '13800000264', '', 'user_l80r6phxur', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (270, '13800000265', '', 'user_np6iqqeuql', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (271, '13800000266', '', 'user_5c27qgw2o3', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (272, '13800000267', '', 'user_ujpa6juatc', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (273, '13800000268', '', 'user_van4fds7cx', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (274, '13800000269', '', 'user_ox11o9krp9', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (275, '13800000270', '', 'user_c7o3u0usf2', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (276, '13800000271', '', 'user_cq7hojlerq', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (277, '13800000272', '', 'user_kphis0ntao', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (278, '13800000273', '', 'user_nd12v2tdpj', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (279, '13800000274', '', 'user_5far04rjm0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (280, '13800000275', '', 'user_f33abomjs2', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (281, '13800000276', '', 'user_1qty1oyawt', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (282, '13800000277', '', 'user_9l463o7me2', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (283, '13800000278', '', 'user_0seyfs8ou8', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (284, '13800000279', '', 'user_7uhqt2zf11', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (285, '13800000280', '', 'user_wy2jtbkr1t', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (286, '13800000281', '', 'user_yjf1kbl6r8', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (287, '13800000282', '', 'user_r98pel35gn', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (288, '13800000283', '', 'user_u2eg1jcwvz', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (289, '13800000284', '', 'user_5z7d4fr9ye', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (290, '13800000285', '', 'user_kl0p0ku6zv', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (291, '13800000286', '', 'user_dsdocfa64r', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (292, '13800000287', '', 'user_gbygsd03kc', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (293, '13800000288', '', 'user_dj1xqos2is', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (294, '13800000289', '', 'user_il6yctz040', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (295, '13800000290', '', 'user_y4zn043gvj', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (296, '13800000291', '', 'user_oh9tjoxq8c', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (297, '13800000292', '', 'user_6xlq088asi', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (298, '13800000293', '', 'user_0sepghh66s', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (299, '13800000294', '', 'user_dzo7q333x7', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (300, '13800000295', '', 'user_n7j3j68agt', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (301, '13800000296', '', 'user_b99vc3qr3d', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (302, '13800000297', '', 'user_o2uu01ngfw', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (303, '13800000298', '', 'user_q0yy8pvku3', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (304, '13800000299', '', 'user_lipi4iyiv9', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (305, '13800000300', '', 'user_x2dq9i90ms', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (306, '13800000301', '', 'user_bz9twrcx01', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (307, '13800000302', '', 'user_iun0ocev18', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (308, '13800000303', '', 'user_uob3pxr062', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (309, '13800000304', '', 'user_nxuwi8q3n5', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (310, '13800000305', '', 'user_g6si4hwe4r', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (311, '13800000306', '', 'user_v4xud6pxnh', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (312, '13800000307', '', 'user_n3vq5a4c49', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (313, '13800000308', '', 'user_6qdfn8dkja', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (314, '13800000309', '', 'user_872mdw0ibu', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (315, '13800000310', '', 'user_s426pmlnno', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (316, '13800000311', '', 'user_n7d3fcnlqf', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (317, '13800000312', '', 'user_d1euhpgvjt', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (318, '13800000313', '', 'user_luwqlqye4n', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (319, '13800000314', '', 'user_m9khstqle0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (320, '13800000315', '', 'user_7u54hoeh5p', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (321, '13800000316', '', 'user_hndxi8iyg7', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (322, '13800000317', '', 'user_csagq8b16v', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (323, '13800000318', '', 'user_sa979r5vfr', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (324, '13800000319', '', 'user_gojbeoieko', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (325, '13800000320', '', 'user_vrxmccmk36', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (326, '13800000321', '', 'user_kwzzzxile8', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (327, '13800000322', '', 'user_8ik6wmzcs3', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (328, '13800000323', '', 'user_x9f4po6eok', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (329, '13800000324', '', 'user_vn0g3rywt0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (330, '13800000325', '', 'user_0h0hqnoqvv', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (331, '13800000326', '', 'user_ne3rvn4cim', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (332, '13800000327', '', 'user_bz20tomja0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (333, '13800000328', '', 'user_7k5d8324tm', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (334, '13800000329', '', 'user_5va74it1op', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (335, '13800000330', '', 'user_gc21xkfgph', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (336, '13800000331', '', 'user_rv1o0ousua', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (337, '13800000332', '', 'user_lkp3hk0t8q', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (338, '13800000333', '', 'user_e2kjjqo7ee', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (339, '13800000334', '', 'user_ja24gfl42z', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (340, '13800000335', '', 'user_5sxrarxxd2', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (341, '13800000336', '', 'user_lzilfx23jr', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (342, '13800000337', '', 'user_4healeh2sq', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (343, '13800000338', '', 'user_txh60qz6xe', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (344, '13800000339', '', 'user_ofie8fobtu', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (345, '13800000340', '', 'user_wxfngmqndc', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (346, '13800000341', '', 'user_n11kdqn95y', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (347, '13800000342', '', 'user_7b9etto6jl', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (348, '13800000343', '', 'user_sa23n9pacw', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (349, '13800000344', '', 'user_1lhe46upfz', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (350, '13800000345', '', 'user_jioft4gdiu', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (351, '13800000346', '', 'user_jta7ld4vty', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (352, '13800000347', '', 'user_5tkaejhk7g', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (353, '13800000348', '', 'user_fkoe771g9p', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (354, '13800000349', '', 'user_snv802ujrx', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (355, '13800000350', '', 'user_4szkwdl3hw', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (356, '13800000351', '', 'user_qq4cteo33l', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (357, '13800000352', '', 'user_hwn4ofw0dp', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (358, '13800000353', '', 'user_2r4xhcvxp2', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (359, '13800000354', '', 'user_iphgurk3nk', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (360, '13800000355', '', 'user_ih9kbl5kzb', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (361, '13800000356', '', 'user_2odlf3rqex', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (362, '13800000357', '', 'user_i9jalo3ouw', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (363, '13800000358', '', 'user_whbqd8vhr2', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (364, '13800000359', '', 'user_bzlvvy10mp', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (365, '13800000360', '', 'user_pe7y2zyii7', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (366, '13800000361', '', 'user_cnk071ghc7', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (367, '13800000362', '', 'user_21cue7tpm0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (368, '13800000363', '', 'user_nidneujm1x', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (369, '13800000364', '', 'user_tx2y3v0pb0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (370, '13800000365', '', 'user_i8ikz0nufr', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (371, '13800000366', '', 'user_omq8bsw2ij', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (372, '13800000367', '', 'user_ffpuo977gj', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (373, '13800000368', '', 'user_jcgx7mukv0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (374, '13800000369', '', 'user_gn6k43jfx8', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (375, '13800000370', '', 'user_esfv726lun', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (376, '13800000371', '', 'user_l7vh3qyhnk', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (377, '13800000372', '', 'user_aqo9nsp13v', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (378, '13800000373', '', 'user_45z1cjwo34', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (379, '13800000374', '', 'user_cukuugiquc', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (380, '13800000375', '', 'user_cmzben5ql1', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (381, '13800000376', '', 'user_fm136hckhd', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (382, '13800000377', '', 'user_4neww35d6t', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (383, '13800000378', '', 'user_p4e2t04dl0', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (384, '13800000379', '', 'user_3s22mzjlgl', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (385, '13800000380', '', 'user_kf0pbo00lp', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (386, '13800000381', '', 'user_7tan2lj2jn', '', '2022-02-28 10:50:49', '2022-02-28 10:50:49');
INSERT INTO `tb_user` VALUES (387, '13800000382', '', 'user_w88q6nof2r', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (388, '13800000383', '', 'user_9aze983wkj', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (389, '13800000384', '', 'user_wtioxpbho1', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (390, '13800000385', '', 'user_yf70g0cjfu', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (391, '13800000386', '', 'user_i1w18vru0l', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (392, '13800000387', '', 'user_6lr3w5npe5', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (393, '13800000388', '', 'user_9n8rjbb9gp', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (394, '13800000389', '', 'user_fe3u4g7p1f', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (395, '13800000390', '', 'user_618vib46zb', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (396, '13800000391', '', 'user_mj4b8uxpu3', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (397, '13800000392', '', 'user_3jq7brld6h', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (398, '13800000393', '', 'user_8577262ob3', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (399, '13800000394', '', 'user_x63u1e3sap', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (400, '13800000395', '', 'user_o2c2l1j9zs', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (401, '13800000396', '', 'user_rtupie4qut', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (402, '13800000397', '', 'user_othsv0bkha', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (403, '13800000398', '', 'user_4wqa1vn3kw', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (404, '13800000399', '', 'user_xmhuythdnn', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (405, '13800000400', '', 'user_alzyibl549', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (406, '13800000401', '', 'user_3nhqsa0htg', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (407, '13800000402', '', 'user_vn81ys9n64', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (408, '13800000403', '', 'user_iz6t7kpxz2', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (409, '13800000404', '', 'user_7gnmjhg1rh', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (410, '13800000405', '', 'user_r2i71mq5lk', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (411, '13800000406', '', 'user_gevxv4bfda', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (412, '13800000407', '', 'user_hqneva0vaz', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (413, '13800000408', '', 'user_8fvquxjm0t', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (414, '13800000409', '', 'user_9u8dxzs9nk', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (415, '13800000410', '', 'user_8mwcrg9ez9', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (416, '13800000411', '', 'user_erzqptr80b', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (417, '13800000412', '', 'user_97xgccgwaf', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (418, '13800000413', '', 'user_5rz6s0zuoh', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (419, '13800000414', '', 'user_8o7cg6rp05', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (420, '13800000415', '', 'user_rhftetybs4', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (421, '13800000416', '', 'user_mjh9uzi92z', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (422, '13800000417', '', 'user_bvaub566b3', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (423, '13800000418', '', 'user_e97b0z12jc', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (424, '13800000419', '', 'user_mcc1pteaw5', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (425, '13800000420', '', 'user_gz1ymib0dl', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (426, '13800000421', '', 'user_owszpn6jem', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (427, '13800000422', '', 'user_nyqxiekdus', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (428, '13800000423', '', 'user_ilr27xnuxu', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (429, '13800000424', '', 'user_mhzftdfxi4', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (430, '13800000425', '', 'user_kavgc8r8f6', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (431, '13800000426', '', 'user_svztbpgr9w', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (432, '13800000427', '', 'user_fdjhoysgy8', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (433, '13800000428', '', 'user_drssks7627', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (434, '13800000429', '', 'user_53kuim78p1', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (435, '13800000430', '', 'user_tpjaw9ygl6', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (436, '13800000431', '', 'user_zlj9ao4lbw', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (437, '13800000432', '', 'user_9nsckyz0k8', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (438, '13800000433', '', 'user_rkyjx5n0k9', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (439, '13800000434', '', 'user_e47mr17jmo', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (440, '13800000435', '', 'user_gdouwxu8bm', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (441, '13800000436', '', 'user_7odu05tcri', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (442, '13800000437', '', 'user_x6dga1y84j', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (443, '13800000438', '', 'user_ubzwoytroz', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (444, '13800000439', '', 'user_brivojp5b1', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (445, '13800000440', '', 'user_q5sluitgii', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (446, '13800000441', '', 'user_bbqazzzawl', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (447, '13800000442', '', 'user_82fbd0oo0u', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (448, '13800000443', '', 'user_87ft14as7t', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (449, '13800000444', '', 'user_diabg787km', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (450, '13800000445', '', 'user_oo1gf0pxln', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (451, '13800000446', '', 'user_4fc1q8u2f3', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (452, '13800000447', '', 'user_hgny53jwpn', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (453, '13800000448', '', 'user_5m75miuy6r', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (454, '13800000449', '', 'user_qx5ohjcayd', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (455, '13800000450', '', 'user_cff2zkpu62', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (456, '13800000451', '', 'user_dsb1rk9dsr', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (457, '13800000452', '', 'user_50h3ylhjnz', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (458, '13800000453', '', 'user_i02f5rjdab', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (459, '13800000454', '', 'user_3vwdqpif1l', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (460, '13800000455', '', 'user_g6xewzg33w', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (461, '13800000456', '', 'user_63u60u6o7f', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (462, '13800000457', '', 'user_m6ikxcr92q', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (463, '13800000458', '', 'user_yzd5lmecur', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (464, '13800000459', '', 'user_m3163uc9al', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (465, '13800000460', '', 'user_1x6f94jq0v', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (466, '13800000461', '', 'user_keo0udy60g', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (467, '13800000462', '', 'user_87y52es2uw', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (468, '13800000463', '', 'user_1zkkz9j0e6', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (469, '13800000464', '', 'user_baznwk8x5q', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (470, '13800000465', '', 'user_b4hnhsmpxw', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (471, '13800000466', '', 'user_1hr6wbd939', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (472, '13800000467', '', 'user_4w7dhr290a', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (473, '13800000468', '', 'user_tkxg6jo3xa', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (474, '13800000469', '', 'user_saosjqptnq', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (475, '13800000470', '', 'user_wjge9hxzfv', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (476, '13800000471', '', 'user_8ex8ynmec4', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (477, '13800000472', '', 'user_beih06msot', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (478, '13800000473', '', 'user_e4tuso2fad', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (479, '13800000474', '', 'user_iolxs2wbfs', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (480, '13800000475', '', 'user_5trre9akf1', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (481, '13800000476', '', 'user_y3l832hamq', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (482, '13800000477', '', 'user_gs7kvt65y8', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (483, '13800000478', '', 'user_8rda39nfpx', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (484, '13800000479', '', 'user_wix6t6g14l', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (485, '13800000480', '', 'user_s2k023vtn7', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (486, '13800000481', '', 'user_qc3nhavb9f', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (487, '13800000482', '', 'user_98eoecfe9s', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (488, '13800000483', '', 'user_kknik7xw80', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (489, '13800000484', '', 'user_17d7bifhpp', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (490, '13800000485', '', 'user_04vbus432n', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (491, '13800000486', '', 'user_3e1xl0tvss', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (492, '13800000487', '', 'user_jpvv20rmfk', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (493, '13800000488', '', 'user_51jd3tfl3e', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (494, '13800000489', '', 'user_agt44szvcb', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (495, '13800000490', '', 'user_pr7icguenq', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (496, '13800000491', '', 'user_2jl0qaecm0', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (497, '13800000492', '', 'user_m1fxzx8i0u', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (498, '13800000493', '', 'user_fh7a1j0vaz', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (499, '13800000494', '', 'user_ty7afbm01v', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (500, '13800000495', '', 'user_bwazk1tt71', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (501, '13800000496', '', 'user_c1wrwmqzfi', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (502, '13800000497', '', 'user_nbfyg2pfql', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (503, '13800000498', '', 'user_h85lj9y0jy', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (504, '13800000499', '', 'user_e0r5gg439j', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (505, '13800000500', '', 'user_k0s8h8v8wt', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (506, '13800000501', '', 'user_0v423qhiz2', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (507, '13800000502', '', 'user_zgze48neoq', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (508, '13800000503', '', 'user_un4nppmh7k', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (509, '13800000504', '', 'user_knr2flv5mr', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (510, '13800000505', '', 'user_cvhg3r8aqj', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (511, '13800000506', '', 'user_92xh46mlff', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (512, '13800000507', '', 'user_vhp8pxmhk6', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (513, '13800000508', '', 'user_hc4c7z9y3k', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (514, '13800000509', '', 'user_03ikpqtn96', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (515, '13800000510', '', 'user_g0l23kj1ta', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (516, '13800000511', '', 'user_hdd1qkfbjy', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (517, '13800000512', '', 'user_vmc478haq2', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (518, '13800000513', '', 'user_g16kk9w1jp', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (519, '13800000514', '', 'user_vlviloabak', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (520, '13800000515', '', 'user_f4t9c9x0qs', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (521, '13800000516', '', 'user_uhd0vskqux', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (522, '13800000517', '', 'user_uidqqwety9', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (523, '13800000518', '', 'user_ijqz4fb077', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (524, '13800000519', '', 'user_d16wfogt38', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (525, '13800000520', '', 'user_50cj7qxejp', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (526, '13800000521', '', 'user_w0mawjfxbf', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (527, '13800000522', '', 'user_vihcs8gddy', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (528, '13800000523', '', 'user_1io84j51kb', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (529, '13800000524', '', 'user_sac23jn0ct', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (530, '13800000525', '', 'user_84saoi0eaq', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (531, '13800000526', '', 'user_bqfd0lusff', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (532, '13800000527', '', 'user_a717jzadbk', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (533, '13800000528', '', 'user_3e6nd805yp', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (534, '13800000529', '', 'user_bgkv3zjjsy', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (535, '13800000530', '', 'user_4lzfuo6hcl', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (536, '13800000531', '', 'user_y748pleoq8', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (537, '13800000532', '', 'user_ciyuki97of', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (538, '13800000533', '', 'user_kaulf89rnl', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (539, '13800000534', '', 'user_h0dan7ux0u', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (540, '13800000535', '', 'user_fvmx4u2re0', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (541, '13800000536', '', 'user_njomftlkps', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (542, '13800000537', '', 'user_2ezx5lxtc4', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (543, '13800000538', '', 'user_161mxzchbt', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (544, '13800000539', '', 'user_f8e3enit63', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (545, '13800000540', '', 'user_j1ygvb30zr', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (546, '13800000541', '', 'user_lskpl9geya', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (547, '13800000542', '', 'user_feww9njnhi', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (548, '13800000543', '', 'user_e8x6u5i9ca', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (549, '13800000544', '', 'user_17al8oqa4w', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (550, '13800000545', '', 'user_nbo1m8bazt', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (551, '13800000546', '', 'user_rqfyp2isyr', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (552, '13800000547', '', 'user_epr1i52q5x', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (553, '13800000548', '', 'user_x154dr8sch', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (554, '13800000549', '', 'user_i5lvnupsu6', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (555, '13800000550', '', 'user_qsnre265gc', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (556, '13800000551', '', 'user_7f3zwt1uso', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (557, '13800000552', '', 'user_qgkrbv1r7p', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (558, '13800000553', '', 'user_b39j58u8ql', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (559, '13800000554', '', 'user_wby0tn39v5', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (560, '13800000555', '', 'user_9vt11wm6wb', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (561, '13800000556', '', 'user_y4rokt5rzh', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (562, '13800000557', '', 'user_lyarwzepjm', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (563, '13800000558', '', 'user_er844jel5s', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (564, '13800000559', '', 'user_2gkdrbu7rd', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (565, '13800000560', '', 'user_fnks15rgap', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (566, '13800000561', '', 'user_fe79dtlbf3', '', '2022-02-28 10:50:50', '2022-02-28 10:50:50');
INSERT INTO `tb_user` VALUES (567, '13800000562', '', 'user_jrl1kdhopy', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (568, '13800000563', '', 'user_p5h5hfw0h5', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (569, '13800000564', '', 'user_756lckggox', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (570, '13800000565', '', 'user_9w56lad204', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (571, '13800000566', '', 'user_kjfvuonq64', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (572, '13800000567', '', 'user_k1i16oya8x', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (573, '13800000568', '', 'user_z4wz2wq9oj', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (574, '13800000569', '', 'user_jotms6c1vz', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (575, '13800000570', '', 'user_29iu6j1olp', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (576, '13800000571', '', 'user_rfkqpu7bs1', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (577, '13800000572', '', 'user_yecqp8c38k', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (578, '13800000573', '', 'user_1mbkrz4rng', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (579, '13800000574', '', 'user_bx6h4wu47y', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (580, '13800000575', '', 'user_usub0nsxez', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (581, '13800000576', '', 'user_2vvxjpuwgr', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (582, '13800000577', '', 'user_9tmhy4nfm1', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (583, '13800000578', '', 'user_q1gyjoatnm', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (584, '13800000579', '', 'user_vaqe3soyoz', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (585, '13800000580', '', 'user_bz81fj51ul', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (586, '13800000581', '', 'user_pwp8w2oife', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (587, '13800000582', '', 'user_6i8a8jpc4a', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (588, '13800000583', '', 'user_e19oms872y', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (589, '13800000584', '', 'user_7jnvjujuk5', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (590, '13800000585', '', 'user_1brabvuxfp', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (591, '13800000586', '', 'user_w25xjchkmt', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (592, '13800000587', '', 'user_qc38678j1q', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (593, '13800000588', '', 'user_5wqfc087pg', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (594, '13800000589', '', 'user_l921wy6ghf', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (595, '13800000590', '', 'user_qgdwy1c8ya', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (596, '13800000591', '', 'user_2ftowbar49', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (597, '13800000592', '', 'user_e0rqhfdde9', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (598, '13800000593', '', 'user_vpswd32xps', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (599, '13800000594', '', 'user_ec479wdojq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (600, '13800000595', '', 'user_6kz95u775k', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (601, '13800000596', '', 'user_iyyh1jdjvk', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (602, '13800000597', '', 'user_jbv97r3zcf', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (603, '13800000598', '', 'user_1t7nmmwx2g', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (604, '13800000599', '', 'user_fb8j6mb1cn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (605, '13800000600', '', 'user_ld0b3fd8uk', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (606, '13800000601', '', 'user_sv8tt0fhb0', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (607, '13800000602', '', 'user_ovqhhaqzfs', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (608, '13800000603', '', 'user_mdybbx800t', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (609, '13800000604', '', 'user_dy1n5yoxhv', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (610, '13800000605', '', 'user_xefu4y7d2d', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (611, '13800000606', '', 'user_4aun9z96dn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (612, '13800000607', '', 'user_guva8ofunq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (613, '13800000608', '', 'user_6l4gzaotnf', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (614, '13800000609', '', 'user_ngbcy6a2zk', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (615, '13800000610', '', 'user_dqqj7ti3pd', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (616, '13800000611', '', 'user_5zq4rzlbik', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (617, '13800000612', '', 'user_7e0qi512hf', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (618, '13800000613', '', 'user_jpmnhzwesi', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (619, '13800000614', '', 'user_00xb9uvv0m', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (620, '13800000615', '', 'user_nxn2ldt3gl', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (621, '13800000616', '', 'user_cyd1a9zfqw', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (622, '13800000617', '', 'user_0nhklq4xie', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (623, '13800000618', '', 'user_rtf3z1wptn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (624, '13800000619', '', 'user_ov4uix8csm', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (625, '13800000620', '', 'user_lxi5i68cdf', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (626, '13800000621', '', 'user_do1slgl1ph', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (627, '13800000622', '', 'user_qj8pbsjpl5', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (628, '13800000623', '', 'user_ygrl56l48d', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (629, '13800000624', '', 'user_maynz9h3vn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (630, '13800000625', '', 'user_m7qnvuej5k', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (631, '13800000626', '', 'user_ceg7kezzrd', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (632, '13800000627', '', 'user_v7ky9w9v6f', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (633, '13800000628', '', 'user_kk8rzbittq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (634, '13800000629', '', 'user_mskczihgi8', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (635, '13800000630', '', 'user_0tmadlzf1j', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (636, '13800000631', '', 'user_oeui72807w', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (637, '13800000632', '', 'user_ad49besbbs', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (638, '13800000633', '', 'user_huzzpviaax', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (639, '13800000634', '', 'user_b0p11t8qon', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (640, '13800000635', '', 'user_14k8fje63n', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (641, '13800000636', '', 'user_bl5rc085pr', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (642, '13800000637', '', 'user_938covh4nt', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (643, '13800000638', '', 'user_olt9qfefvw', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (644, '13800000639', '', 'user_bhkdwtkfdg', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (645, '13800000640', '', 'user_we6790rc8v', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (646, '13800000641', '', 'user_wqmiqbrj3a', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (647, '13800000642', '', 'user_ccdo9ncgzt', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (648, '13800000643', '', 'user_pk3l58b3cf', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (649, '13800000644', '', 'user_meqr7dxbog', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (650, '13800000645', '', 'user_x70v1uaf0g', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (651, '13800000646', '', 'user_yijawdxi8k', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (652, '13800000647', '', 'user_qlv8jnv927', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (653, '13800000648', '', 'user_2tkj1s7aex', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (654, '13800000649', '', 'user_5vbfw1gln6', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (655, '13800000650', '', 'user_zseyyi59z2', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (656, '13800000651', '', 'user_8ch1tq5bfp', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (657, '13800000652', '', 'user_gdgb5nbkqn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (658, '13800000653', '', 'user_rr5448qo4l', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (659, '13800000654', '', 'user_e6zwifzqhw', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (660, '13800000655', '', 'user_7ytv4vd6he', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (661, '13800000656', '', 'user_pc84newj49', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (662, '13800000657', '', 'user_h4wpk3e9ht', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (663, '13800000658', '', 'user_d3vt4vqim8', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (664, '13800000659', '', 'user_eqr14mgaz2', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (665, '13800000660', '', 'user_ldd4kzq961', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (666, '13800000661', '', 'user_w4qu1bb2lk', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (667, '13800000662', '', 'user_0627bn8px3', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (668, '13800000663', '', 'user_64aj20cdf1', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (669, '13800000664', '', 'user_l7u34b3ler', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (670, '13800000665', '', 'user_2ze9tl9994', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (671, '13800000666', '', 'user_m5phoejixm', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (672, '13800000667', '', 'user_8ogdovuirm', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (673, '13800000668', '', 'user_wfk4ebck83', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (674, '13800000669', '', 'user_oupbnni466', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (675, '13800000670', '', 'user_89967wcevq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (676, '13800000671', '', 'user_xr6g2q08cm', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (677, '13800000672', '', 'user_m313bjckeq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (678, '13800000673', '', 'user_x25nq1ss14', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (679, '13800000674', '', 'user_jeidzxzp6y', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (680, '13800000675', '', 'user_l7dkffo3n0', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (681, '13800000676', '', 'user_pqio9ol2ln', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (682, '13800000677', '', 'user_k1cbsqi4gt', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (683, '13800000678', '', 'user_p1i9b4p4sv', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (684, '13800000679', '', 'user_07yfm6qtl1', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (685, '13800000680', '', 'user_y3mmmk1kak', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (686, '13800000681', '', 'user_lkxjnwtqa7', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (687, '13800000682', '', 'user_v5w9pm18iq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (688, '13800000683', '', 'user_364l5poxpw', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (689, '13800000684', '', 'user_trlfkptv3g', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (690, '13800000685', '', 'user_rkheg82tnp', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (691, '13800000686', '', 'user_5zmzrjgo5o', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (692, '13800000687', '', 'user_6uacx6cqhp', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (693, '13800000688', '', 'user_wnats1phoj', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (694, '13800000689', '', 'user_dcm1w7674v', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (695, '13800000690', '', 'user_r7ik7ae272', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (696, '13800000691', '', 'user_xk77qyl4gl', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (697, '13800000692', '', 'user_989d1fsji4', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (698, '13800000693', '', 'user_macs32vcct', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (699, '13800000694', '', 'user_z5mahfpa9r', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (700, '13800000695', '', 'user_tn1bnk3zir', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (701, '13800000696', '', 'user_95ajn6osry', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (702, '13800000697', '', 'user_qff1n375uc', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (703, '13800000698', '', 'user_gdjqlq4i6n', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (704, '13800000699', '', 'user_w6tsnpzfqn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (705, '13800000700', '', 'user_lqp4c4j2ch', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (706, '13800000701', '', 'user_1raii40ps1', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (707, '13800000702', '', 'user_0r9izz201x', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (708, '13800000703', '', 'user_vlrp22q0rk', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (709, '13800000704', '', 'user_f7kvbzb8i4', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (710, '13800000705', '', 'user_yn8nntyyur', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (711, '13800000706', '', 'user_p58nxqajou', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (712, '13800000707', '', 'user_61msspy26k', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (713, '13800000708', '', 'user_fqb0ch1hh1', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (714, '13800000709', '', 'user_oyq3nszclx', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (715, '13800000710', '', 'user_ggybvkn73t', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (716, '13800000711', '', 'user_po0gph6jgp', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (717, '13800000712', '', 'user_hlzvh6wl1y', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (718, '13800000713', '', 'user_btb024hqic', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (719, '13800000714', '', 'user_wqasvon847', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (720, '13800000715', '', 'user_rdp7fvaz3z', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (721, '13800000716', '', 'user_oh5q9kfkvc', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (722, '13800000717', '', 'user_ae21kmiles', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (723, '13800000718', '', 'user_b1ouyw3sww', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (724, '13800000719', '', 'user_9o5qz4s17l', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (725, '13800000720', '', 'user_6urs1iwti9', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (726, '13800000721', '', 'user_80pnfhyqyi', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (727, '13800000722', '', 'user_mynvmq4zcn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (728, '13800000723', '', 'user_q09fj27oc4', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (729, '13800000724', '', 'user_v4e7hkfw63', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (730, '13800000725', '', 'user_x4sol5dj4f', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (731, '13800000726', '', 'user_v53dsicdtx', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (732, '13800000727', '', 'user_fcoezs141i', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (733, '13800000728', '', 'user_viv3l4o52c', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (734, '13800000729', '', 'user_8j4m80dj76', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (735, '13800000730', '', 'user_r65xyt3opb', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (736, '13800000731', '', 'user_moi9x442th', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (737, '13800000732', '', 'user_qxkltii6ec', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (738, '13800000733', '', 'user_72vsybd20b', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (739, '13800000734', '', 'user_eai1g9ltu9', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (740, '13800000735', '', 'user_h47ubi7f36', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (741, '13800000736', '', 'user_yxo46519hp', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (742, '13800000737', '', 'user_499diayvwn', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (743, '13800000738', '', 'user_ytomkocl3c', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (744, '13800000739', '', 'user_271mt5x5uo', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (745, '13800000740', '', 'user_h5s36mj609', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (746, '13800000741', '', 'user_sklzx3z3nq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (747, '13800000742', '', 'user_9v2ikjkwm8', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (748, '13800000743', '', 'user_w5jjd49ipx', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (749, '13800000744', '', 'user_3rzokm18yo', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (750, '13800000745', '', 'user_vm6zz6ejs7', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (751, '13800000746', '', 'user_r494p0jlle', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (752, '13800000747', '', 'user_c50thdpyv0', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (753, '13800000748', '', 'user_hc4qi0sfo2', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (754, '13800000749', '', 'user_w8y4nebzxs', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (755, '13800000750', '', 'user_mxxqu6isy9', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (756, '13800000751', '', 'user_sd3f76mtg3', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (757, '13800000752', '', 'user_6zb026vsmm', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (758, '13800000753', '', 'user_mzya91331l', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (759, '13800000754', '', 'user_adu5gmym2g', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (760, '13800000755', '', 'user_31bidh90w5', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (761, '13800000756', '', 'user_iectlacbk7', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (762, '13800000757', '', 'user_by8vl07035', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (763, '13800000758', '', 'user_n8ii3p3b6z', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (764, '13800000759', '', 'user_eopvczuyzq', '', '2022-02-28 10:50:51', '2022-02-28 10:50:51');
INSERT INTO `tb_user` VALUES (765, '13800000760', '', 'user_2m36qy9yht', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (766, '13800000761', '', 'user_re1q80zze2', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (767, '13800000762', '', 'user_lelhu217ad', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (768, '13800000763', '', 'user_dyv7ll1h9r', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (769, '13800000764', '', 'user_7zws9wi4cp', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (770, '13800000765', '', 'user_tvseis2smv', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (771, '13800000766', '', 'user_975ls201ra', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (772, '13800000767', '', 'user_0416smxpjc', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (773, '13800000768', '', 'user_dkdw3wuvxt', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (774, '13800000769', '', 'user_d1z5jtfh2g', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (775, '13800000770', '', 'user_yg9r3ws35z', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (776, '13800000771', '', 'user_9cos7jzgmy', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (777, '13800000772', '', 'user_679sq0b6eb', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (778, '13800000773', '', 'user_kzk5m1pgqv', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (779, '13800000774', '', 'user_28qetr02oe', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (780, '13800000775', '', 'user_peazcxx51i', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (781, '13800000776', '', 'user_roghf2lerp', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (782, '13800000777', '', 'user_sth9xhgsoj', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (783, '13800000778', '', 'user_38ejcd1npp', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (784, '13800000779', '', 'user_m0y48rqbxs', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (785, '13800000780', '', 'user_a0f919rrdw', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (786, '13800000781', '', 'user_veddhmnfa7', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (787, '13800000782', '', 'user_ltexwx6bm6', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (788, '13800000783', '', 'user_euqn9si8dg', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (789, '13800000784', '', 'user_wm4s4v0o87', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (790, '13800000785', '', 'user_mthbqaorve', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (791, '13800000786', '', 'user_k63cindeeh', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (792, '13800000787', '', 'user_kz30acb48r', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (793, '13800000788', '', 'user_1jmeyd8a28', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (794, '13800000789', '', 'user_su5oi3kpfx', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (795, '13800000790', '', 'user_4eurdp0387', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (796, '13800000791', '', 'user_orxdegd4d4', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (797, '13800000792', '', 'user_50vxeli8rd', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (798, '13800000793', '', 'user_vqsnl66ot5', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (799, '13800000794', '', 'user_en3q7qyiqb', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (800, '13800000795', '', 'user_0yyk9mnng0', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (801, '13800000796', '', 'user_l48qjtjmxl', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (802, '13800000797', '', 'user_1wvigh2hxq', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (803, '13800000798', '', 'user_gr0bhwfvhu', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (804, '13800000799', '', 'user_qpku5s9nr6', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (805, '13800000800', '', 'user_kyhepj12kd', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (806, '13800000801', '', 'user_3x99ypxvqy', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (807, '13800000802', '', 'user_np8bk7b07w', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (808, '13800000803', '', 'user_dnu8kswk6o', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (809, '13800000804', '', 'user_u01mnauofu', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (810, '13800000805', '', 'user_48sv36r3xs', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (811, '13800000806', '', 'user_6ojf6nhxch', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (812, '13800000807', '', 'user_wd32jqla7r', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (813, '13800000808', '', 'user_zsdxxcpkuq', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (814, '13800000809', '', 'user_ib97xw8nl2', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (815, '13800000810', '', 'user_b7qb56z1p0', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (816, '13800000811', '', 'user_i7jmrgmisg', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (817, '13800000812', '', 'user_5nf21zmos7', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (818, '13800000813', '', 'user_mck6nqe55g', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (819, '13800000814', '', 'user_6xnadvfus7', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (820, '13800000815', '', 'user_450u8mqe4z', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (821, '13800000816', '', 'user_hv55cq5n1w', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (822, '13800000817', '', 'user_qiy3ulbyd0', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (823, '13800000818', '', 'user_sx58542ugn', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (824, '13800000819', '', 'user_9xs0uuyds5', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (825, '13800000820', '', 'user_zveuo0azp4', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (826, '13800000821', '', 'user_qwt4x5faay', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (827, '13800000822', '', 'user_ztzuqeybvp', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (828, '13800000823', '', 'user_kh5n7wfie8', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (829, '13800000824', '', 'user_dwxkvw03b7', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (830, '13800000825', '', 'user_3tyhv91k7p', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (831, '13800000826', '', 'user_0jwdppbvdk', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (832, '13800000827', '', 'user_twx4z08vzb', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (833, '13800000828', '', 'user_lly5v9ibpk', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (834, '13800000829', '', 'user_kkho7xpu2u', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (835, '13800000830', '', 'user_l51d2os1wh', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (836, '13800000831', '', 'user_i6gkyfawkv', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (837, '13800000832', '', 'user_v2k5vdh4he', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (838, '13800000833', '', 'user_7k9ql2go9e', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (839, '13800000834', '', 'user_tnz3f99w2c', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (840, '13800000835', '', 'user_833zw6fgxz', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (841, '13800000836', '', 'user_f4hq0ga1oj', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (842, '13800000837', '', 'user_uxuxrig26t', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (843, '13800000838', '', 'user_grn37re7bg', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (844, '13800000839', '', 'user_5msjf8z2fj', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (845, '13800000840', '', 'user_53x3w7l7mv', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (846, '13800000841', '', 'user_pyolvy8m0v', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (847, '13800000842', '', 'user_12i4hpk09n', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (848, '13800000843', '', 'user_zjhyyt7zfq', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (849, '13800000844', '', 'user_avv8zgw4qk', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (850, '13800000845', '', 'user_khxmnqb6ni', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (851, '13800000846', '', 'user_i80iu0pb5k', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (852, '13800000847', '', 'user_lqkx9uurmj', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (853, '13800000848', '', 'user_ewiswre8fm', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (854, '13800000849', '', 'user_d0nwznn64y', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (855, '13800000850', '', 'user_v7wyz44u6m', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (856, '13800000851', '', 'user_ipkx0z0nno', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (857, '13800000852', '', 'user_64tnyqwxun', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (858, '13800000853', '', 'user_r9bjp3fegg', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (859, '13800000854', '', 'user_i36s8hsq72', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (860, '13800000855', '', 'user_cqe1zvq4dr', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (861, '13800000856', '', 'user_omdgisd0ls', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (862, '13800000857', '', 'user_3mgz8z636y', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (863, '13800000858', '', 'user_ts3qtzwp68', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (864, '13800000859', '', 'user_56seol3kxp', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (865, '13800000860', '', 'user_4x55muo0si', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (866, '13800000861', '', 'user_ny46fscq78', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (867, '13800000862', '', 'user_raano2keb9', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (868, '13800000863', '', 'user_31m00sj2bt', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (869, '13800000864', '', 'user_2ovmzeq4f3', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (870, '13800000865', '', 'user_dis12x5ko3', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (871, '13800000866', '', 'user_jx9defd5pu', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (872, '13800000867', '', 'user_k3u9zems0n', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (873, '13800000868', '', 'user_o84aucm31f', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (874, '13800000869', '', 'user_h4msccd8qo', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (875, '13800000870', '', 'user_6sk051bxed', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (876, '13800000871', '', 'user_1s1r4kks05', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (877, '13800000872', '', 'user_2pfvfdb27x', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (878, '13800000873', '', 'user_k5nxhuil69', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (879, '13800000874', '', 'user_6wu2vujv7x', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (880, '13800000875', '', 'user_05jr9c63o0', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (881, '13800000876', '', 'user_cc2l1lrlw5', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (882, '13800000877', '', 'user_ieeqrlof8f', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (883, '13800000878', '', 'user_6m5ermqkua', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (884, '13800000879', '', 'user_mh99rug0nh', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (885, '13800000880', '', 'user_n55ceoc392', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (886, '13800000881', '', 'user_72vzhk8py3', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (887, '13800000882', '', 'user_bthii5wt36', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (888, '13800000883', '', 'user_mut3q0vunf', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (889, '13800000884', '', 'user_symgsydmbd', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (890, '13800000885', '', 'user_7qs7kedl19', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (891, '13800000886', '', 'user_uwyx1i29m0', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (892, '13800000887', '', 'user_ls2p6sldmi', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (893, '13800000888', '', 'user_1kmmkpegso', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (894, '13800000889', '', 'user_4zp483y1e7', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (895, '13800000890', '', 'user_nr78kan9c3', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (896, '13800000891', '', 'user_0r0m7ngv6x', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (897, '13800000892', '', 'user_lknjznxmau', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (898, '13800000893', '', 'user_v9g6j6h0ah', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (899, '13800000894', '', 'user_wuyim37fx5', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (900, '13800000895', '', 'user_l0lfqjjzs0', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (901, '13800000896', '', 'user_6uyxk7pa4u', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (902, '13800000897', '', 'user_f17o0qymn9', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (903, '13800000898', '', 'user_ogpqk1b39a', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (904, '13800000899', '', 'user_9jpofrgda1', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (905, '13800000900', '', 'user_n298v8udm3', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (906, '13800000901', '', 'user_0biwjc5wwt', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (907, '13800000902', '', 'user_xbbdx6wq53', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (908, '13800000903', '', 'user_nh79qly5ir', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (909, '13800000904', '', 'user_v86oajknbs', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (910, '13800000905', '', 'user_e13odsshad', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (911, '13800000906', '', 'user_6cvwrirdtl', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (912, '13800000907', '', 'user_nqr7bpgz67', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (913, '13800000908', '', 'user_wn1ae0p6gw', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (914, '13800000909', '', 'user_te48rluimb', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (915, '13800000910', '', 'user_p2r85n4k8g', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (916, '13800000911', '', 'user_ca8fdlrbty', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (917, '13800000912', '', 'user_toque00p0i', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (918, '13800000913', '', 'user_uiti5cdbhf', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (919, '13800000914', '', 'user_8pgku7viy8', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (920, '13800000915', '', 'user_cdafki4cwc', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (921, '13800000916', '', 'user_fyyk2yfpk5', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (922, '13800000917', '', 'user_78e1meevls', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (923, '13800000918', '', 'user_qzwls7m33b', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (924, '13800000919', '', 'user_jxuw8ixefk', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (925, '13800000920', '', 'user_1xye60infx', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (926, '13800000921', '', 'user_gvccna2mni', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (927, '13800000922', '', 'user_tftvpegd2c', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (928, '13800000923', '', 'user_6ihh78vpox', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (929, '13800000924', '', 'user_46qroyojdl', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (930, '13800000925', '', 'user_wwi4i2wb77', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (931, '13800000926', '', 'user_s28l0bryil', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (932, '13800000927', '', 'user_4lgib8jvrx', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (933, '13800000928', '', 'user_fczpz5s31b', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (934, '13800000929', '', 'user_3cvkn9pv9w', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (935, '13800000930', '', 'user_wtvk7gx8ar', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (936, '13800000931', '', 'user_yrel6rbyyd', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (937, '13800000932', '', 'user_hmxjnsbnon', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (938, '13800000933', '', 'user_cuxcl0d2oo', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (939, '13800000934', '', 'user_1ax8x9zw0c', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (940, '13800000935', '', 'user_p7v98oe5nm', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (941, '13800000936', '', 'user_m90rt3bwsz', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (942, '13800000937', '', 'user_xhty5jm1hy', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (943, '13800000938', '', 'user_7h88k22eo0', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (944, '13800000939', '', 'user_5a75z9jcqa', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (945, '13800000940', '', 'user_3t0twwq0nh', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (946, '13800000941', '', 'user_861ywr4gfr', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (947, '13800000942', '', 'user_iwkz8k1zpx', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (948, '13800000943', '', 'user_vzmhyoz1ap', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (949, '13800000944', '', 'user_5tmpddukgq', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (950, '13800000945', '', 'user_h6siyam4hb', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (951, '13800000946', '', 'user_n5yqq6mgka', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (952, '13800000947', '', 'user_an9epa7f2r', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (953, '13800000948', '', 'user_5vr0cdy8sz', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (954, '13800000949', '', 'user_xpanlhqjbq', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (955, '13800000950', '', 'user_3cfykc172m', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (956, '13800000951', '', 'user_1n0jceyzim', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (957, '13800000952', '', 'user_4ixi7efxtr', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (958, '13800000953', '', 'user_5adpp336iy', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (959, '13800000954', '', 'user_mflzjd6e6b', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (960, '13800000955', '', 'user_80bwfj72p7', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (961, '13800000956', '', 'user_i3anusitco', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (962, '13800000957', '', 'user_yj4pcsrkl9', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (963, '13800000958', '', 'user_7v9x6gxjdz', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (964, '13800000959', '', 'user_2ahufmnyzp', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (965, '13800000960', '', 'user_1oel6c441t', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (966, '13800000961', '', 'user_qxzcv0ib6g', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (967, '13800000962', '', 'user_9uyh0i8ykg', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (968, '13800000963', '', 'user_tb01d4d9ql', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (969, '13800000964', '', 'user_hwpkx6ovii', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (970, '13800000965', '', 'user_pqd04q9hq2', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (971, '13800000966', '', 'user_4t7wkgkufh', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (972, '13800000967', '', 'user_834e4vzf0e', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (973, '13800000968', '', 'user_pxk4urlnmo', '', '2022-02-28 10:50:52', '2022-02-28 10:50:52');
INSERT INTO `tb_user` VALUES (974, '13800000969', '', 'user_e3x6n0ff0d', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (975, '13800000970', '', 'user_wxnvsvb5ut', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (976, '13800000971', '', 'user_ehi7k4zpjb', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (977, '13800000972', '', 'user_om0pzyh3z1', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (978, '13800000973', '', 'user_9asdqbe7od', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (979, '13800000974', '', 'user_seuabngxt9', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (980, '13800000975', '', 'user_b0qvb27eiy', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (981, '13800000976', '', 'user_63sjue2tkh', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (982, '13800000977', '', 'user_cc3lvxfr1u', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (983, '13800000978', '', 'user_in37hfw5tk', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (984, '13800000979', '', 'user_jtg0c9tyqn', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (985, '13800000980', '', 'user_qzpipaj50w', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (986, '13800000981', '', 'user_ppnb4ljetq', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (987, '13800000982', '', 'user_zbcui7783k', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (988, '13800000983', '', 'user_ki4dxb9q9b', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (989, '13800000984', '', 'user_27b5dxktn0', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (990, '13800000985', '', 'user_fxvb2av882', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (991, '13800000986', '', 'user_6vp3uflnwm', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (992, '13800000987', '', 'user_7ix7djbg30', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (993, '13800000988', '', 'user_vx8r39tjiu', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (994, '13800000989', '', 'user_l2wdiwule0', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (995, '13800000990', '', 'user_z4qe1up5zx', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (996, '13800000991', '', 'user_bklo4b32lu', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (997, '13800000992', '', 'user_ax0y473ndh', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (998, '13800000993', '', 'user_yx2p44qww3', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (999, '13800000994', '', 'user_bnw9bzib34', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1000, '13800000995', '', 'user_cdj4ojh4pc', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1001, '13800000996', '', 'user_l7o3r96hn3', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1002, '13800000997', '', 'user_zbehzrz279', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1003, '13800000998', '', 'user_tql21zepcx', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1004, '13800000999', '', 'user_jnxnrk8qt0', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1005, '13800001000', '', 'user_8e5twg6q0k', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1006, '13800001001', '', 'user_gfeusukbpp', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1007, '13800001002', '', 'user_sveivfswhn', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1008, '13800001003', '', 'user_qgf4t8jkx0', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');
INSERT INTO `tb_user` VALUES (1009, '13800001004', '', 'user_4qh6bofkol', '', '2022-02-28 10:50:53', '2022-02-28 10:50:53');

-- ----------------------------
-- Table structure for tb_user_info
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_info`;
CREATE TABLE `tb_user_info`  (
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '主键，用户id',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '城市名称',
  `introduce` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '个人介绍，不要超过128个字符',
  `fans` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '粉丝数量',
  `followee` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '关注的人的数量',
  `gender` tinyint(1) UNSIGNED NULL DEFAULT 0 COMMENT '性别，0：男，1：女',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `credits` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '积分',
  `level` tinyint(1) UNSIGNED NULL DEFAULT 0 COMMENT '会员级别，0~9级,0代表未开通会员',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_user_info
-- ----------------------------

-- ----------------------------
-- Table structure for tb_voucher
-- ----------------------------
DROP TABLE IF EXISTS `tb_voucher`;
CREATE TABLE `tb_voucher`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '商铺id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '代金券标题',
  `sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '副标题',
  `rules` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '使用规则',
  `pay_value` bigint(10) UNSIGNED NOT NULL COMMENT '支付金额，单位是分。例如200代表2元',
  `actual_value` bigint(10) NOT NULL COMMENT '抵扣金额，单位是分。例如200代表2元',
  `type` tinyint(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT '0,普通券；1,秒杀券',
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '1,上架; 2,下架; 3,过期',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_voucher
-- ----------------------------
INSERT INTO `tb_voucher` VALUES (1, 1, '50元代金券', '周一至周日均可使用', '全场通用\\n无需预约\\n可无限叠加\\不兑现、不找零\\n仅限堂食', 4750, 5000, 0, 1, '2022-01-04 09:42:39', '2022-01-04 09:43:31');

-- ----------------------------
-- Table structure for tb_voucher_order
-- ----------------------------
DROP TABLE IF EXISTS `tb_voucher_order`;
CREATE TABLE `tb_voucher_order`  (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '下单的用户id',
  `voucher_id` bigint(20) UNSIGNED NOT NULL COMMENT '购买的代金券id',
  `pay_type` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '支付方式 1：余额支付；2：支付宝；3：微信',
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `use_time` timestamp NULL DEFAULT NULL COMMENT '核销时间',
  `refund_time` timestamp NULL DEFAULT NULL COMMENT '退款时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_voucher_order
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
