-- =====================================================================
-- 黑马点评改造 · 数据库升级脚本 V1
--
-- 用法（本项目没有接 Flyway，这是手工执行一次的脚本）：
--   mysql -uroot -p hmdp < src/main/resources/db/V1__upgrade.sql
--
-- 三条约束：
--   1) 可重复执行：每一步都先查 information_schema，已存在的直接跳过；
--   2) 不删任何业务数据：只加列、加索引、建历史表；
--   3) init_stock 必须回填：老数据没有这一列，回填值取当前 stock，
--      否则对账任务会把所有券当成"初始库存 0"从而误报。
--
-- 为什么这些对象不在 hmdp.sql 里：
--   hmdp.sql 是课程原版基线，保持不动，方便对照"我改了哪些"；
--   改造引入的结构统一放这里，评审时一眼能看出增量。
-- =====================================================================

USE `hmdp`;

DROP PROCEDURE IF EXISTS `hmdp_upgrade_v1`;

DELIMITER $$

CREATE PROCEDURE `hmdp_upgrade_v1`()
BEGIN
    -- ---------------------------------------------------------------
    -- 1. 订单表：active_flag（一人一单的唯一索引需要一个"可置空"的列）
    --
    --    为什么是 NULL 而不是 0：
    --    MySQL 的唯一索引不约束 NULL，多行 NULL 视为互不相同。
    --    所以 active_flag = 1 时受唯一约束，取消/退款后置 NULL 就能再买一张，
    --    历史订单也不会挡住新订单。用 0 反而会让"退过一次就再也买不了"。
    -- ---------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_voucher_order'
                     AND COLUMN_NAME = 'active_flag') THEN
        ALTER TABLE `tb_voucher_order`
            ADD COLUMN `active_flag` TINYINT NULL DEFAULT 1
                COMMENT '有效标记：1 有效；NULL 已取消/已退款（NULL 不参与唯一约束）'
                AFTER `update_time`;
    END IF;

    -- 2. 一人一单的最终防线。Redis 的 Set 会在重启/主从切换时丢，索引不会
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_voucher_order'
                     AND INDEX_NAME = 'uk_user_voucher') THEN
        ALTER TABLE `tb_voucher_order`
            ADD UNIQUE KEY `uk_user_voucher` (`user_id`, `voucher_id`, `active_flag`);
    END IF;

    -- 3. 超时关单每分钟扫一次 status=1 AND create_time < ?, 没有这个索引就是全表扫
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_voucher_order'
                     AND INDEX_NAME = 'idx_status_create') THEN
        ALTER TABLE `tb_voucher_order`
            ADD KEY `idx_status_create` (`status`, `create_time`);
    END IF;

    -- ---------------------------------------------------------------
    -- 4. 秒杀券：init_stock（对账基准）
    --    只有 stock 的话，对账只能看到"现在还剩多少"，
    --    判断不了"本来有多少"，也就分不清少卖和超卖。
    -- ---------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_seckill_voucher'
                     AND COLUMN_NAME = 'init_stock') THEN
        ALTER TABLE `tb_seckill_voucher`
            ADD COLUMN `init_stock` INT NOT NULL DEFAULT 0
                COMMENT '初始库存，对账基准（新建券时写入）'
                AFTER `stock`;
    END IF;

    -- 老数据回填：只在"还没回填过"时执行，重复跑不会覆盖已扣减的 stock
    UPDATE `tb_seckill_voucher`
    SET `init_stock` = `stock`
    WHERE `init_stock` = 0
      AND `stock` > 0;

    -- ---------------------------------------------------------------
    -- 5. 历史订单表（冷热分离的冷库）
    --    LIKE 复制结构，包含主键和唯一索引：
    --    归档重复执行时，主键冲突会直接暴露"上次归档没删主表"，而不是悄悄插重复数据。
    -- ---------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_voucher_order_history') THEN
        CREATE TABLE `tb_voucher_order_history` LIKE `tb_voucher_order`;
    END IF;

    -- ---------------------------------------------------------------
    -- 6. 关注关系：唯一索引（重复关注只能有一条）
    --
    --    原表只有主键，重复点"关注"会插入多条 (user_id, follow_user_id)，
    --    表现是粉丝数虚高、共同关注列表里同一个人出现多次。
    --    加了唯一索引之后，重复关注由索引挡住，代码侧按"幂等成功"处理。
    --
    --    为什么不是先查再插：查和插之间仍有并发窗口，两个请求可能都查到"不存在"。
    --    唯一索引是数据库层的最终保证，代码只负责把冲突翻译成"已经关注过了"。
    -- ---------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_follow'
                     AND INDEX_NAME = 'uk_user_follow') THEN
        ALTER TABLE `tb_follow`
            ADD UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`);
    END IF;

    -- ---------------------------------------------------------------
    -- 7. 读路径索引：三条高频查询在原表上都是全表扫
    --
    --    ① 热度榜        ORDER BY liked DESC LIMIT 10   -> tb_blog 上没有 liked 索引，走 filesort
    --    ② 我的/他人笔记  WHERE user_id = ?              -> tb_blog 上没有 user_id 索引
    --    ③ 店铺券列表    WHERE shop_id = ? AND status=1  -> tb_voucher 上没有 shop_id 索引
    --
    --    为什么现在才加：课程数据只有 5 篇笔记、2 张券，全表扫比走索引还快，
    --    优化器本来就会忽略索引。数据量上来后这三条会随表线性变慢，
    --    属于"不加不会报错、加了才不慌"的那一类。
    -- ---------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_blog'
                     AND INDEX_NAME = 'idx_blog_user') THEN
        ALTER TABLE `tb_blog` ADD KEY `idx_blog_user` (`user_id`);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_blog'
                     AND INDEX_NAME = 'idx_blog_liked') THEN
        ALTER TABLE `tb_blog` ADD KEY `idx_blog_liked` (`liked`);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_voucher'
                     AND INDEX_NAME = 'idx_voucher_shop') THEN
        ALTER TABLE `tb_voucher` ADD KEY `idx_voucher_shop` (`shop_id`, `status`);
    END IF;
END$$

DELIMITER ;

CALL `hmdp_upgrade_v1`();

DROP PROCEDURE IF EXISTS `hmdp_upgrade_v1`;

-- ---------------------------------------------------------------------
-- 自检：执行完应当看到 8 个对象都在
-- ---------------------------------------------------------------------
SELECT 'active_flag' AS object_name, COUNT(*) AS hit
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_voucher_order' AND COLUMN_NAME = 'active_flag'
UNION ALL
SELECT 'uk_user_voucher', COUNT(DISTINCT INDEX_NAME)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_voucher_order' AND INDEX_NAME = 'uk_user_voucher'
UNION ALL
SELECT 'init_stock', COUNT(*)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_seckill_voucher' AND COLUMN_NAME = 'init_stock'
UNION ALL
SELECT 'tb_voucher_order_history', COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_voucher_order_history'
UNION ALL
SELECT 'uk_user_follow', COUNT(DISTINCT INDEX_NAME)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_follow' AND INDEX_NAME = 'uk_user_follow'
UNION ALL
SELECT 'idx_blog_user', COUNT(DISTINCT INDEX_NAME)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_blog' AND INDEX_NAME = 'idx_blog_user'
UNION ALL
SELECT 'idx_blog_liked', COUNT(DISTINCT INDEX_NAME)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_blog' AND INDEX_NAME = 'idx_blog_liked'
UNION ALL
SELECT 'idx_voucher_shop', COUNT(DISTINCT INDEX_NAME)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_voucher' AND INDEX_NAME = 'idx_voucher_shop';
