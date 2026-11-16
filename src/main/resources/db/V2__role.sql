-- =====================================================================
-- 黑马点评改造 · 数据库升级脚本 V2：用户角色
--
-- 用法（本项目没有接 Flyway，手工执行一次）：
--   mysql -uroot -p hmdp < src/main/resources/db/V2__role.sql
--
-- 为什么需要这一列（改造前的问题）：
--   项目只有"登录 / 未登录"，没有身份区分。按 HTTP 方法收紧白名单之后，
--   匿名已经建不了店、发不了券；但**任何登录用户**仍然可以——实测普通账号
--   POST /voucher 能建成一张券。根因不在校验，在数据模型：tb_user 没有角色。
--
-- 三条约束（沿用 V1 的规矩）：
--   1) 可重复执行：先查 information_schema，已存在就跳过；
--   2) 不删任何业务数据：只加列；
--   3) 存量用户一律 USER，不给任何账号默认商家权限。
--      要指定商家请显式执行最后的 UPDATE（默认注释掉，避免误执行）。
-- =====================================================================

USE `hmdp`;

DROP PROCEDURE IF EXISTS `hmdp_upgrade_v2`;

DELIMITER $$

CREATE PROCEDURE `hmdp_upgrade_v2`()
BEGIN
    -- ---------------------------------------------------------------
    -- tb_user.role：USER / MERCHANT
    --
    -- 为什么 NOT NULL DEFAULT 'USER' 而不是允许 NULL：
    --   代码里 Role.of(null) 已经会降级成 USER，但库层面也不该留空。
    --   允许 NULL 的话，"这行是故意没有角色"和"迁移漏了一行"看起来一样，
    --   排查时无法区分。默认值让新老数据都明确表示"普通用户"。
    --
    -- 为什么用 varchar(16) 而不是 tinyint：
    --   只有两级角色，tinyint 更省空间，但可读性差——改库、看日志、查问题
    --   都要对着"1 是什么"翻文档。16 字节换一眼看懂，这笔账在这个量级上划算。
    --   真到需要按角色批量查的时候再补索引，现在不需要（数据量千级）。
    -- ---------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'tb_user'
                     AND COLUMN_NAME = 'role') THEN
        ALTER TABLE `tb_user`
            ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT 'USER'
                COMMENT '角色：USER 普通用户 / MERCHANT 商家' AFTER `icon`;
    END IF;

    -- 存量数据兜底：如果历史上有 NULL（比如手工加列没给默认值），补齐成 USER
    UPDATE `tb_user` SET `role` = 'USER' WHERE `role` IS NULL OR `role` = '';

    -- ---------------------------------------------------------------
    -- 指定商家账号（默认不执行，按需手动开）
    --
    -- 反过来验证"降权"也用它：改回 USER 后重新登录，商家接口应立即 403。
    -- 注意：角色写在 JWT 里，改完必须重新登录才生效，旧 token 仍是旧权限。
    -- ---------------------------------------------------------------
    -- UPDATE `tb_user` SET `role` = 'MERCHANT' WHERE `phone` = '13800000000';

    SELECT 'tb_user.role' AS obj, COUNT(*) AS present
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user' AND COLUMN_NAME = 'role'
    UNION ALL
    SELECT 'user_merchant_count', COUNT(*) FROM `tb_user` WHERE `role` = 'MERCHANT'
    UNION ALL
    SELECT 'user_total', COUNT(*) FROM `tb_user`;
END $$

DELIMITER ;

CALL `hmdp_upgrade_v2`();
DROP PROCEDURE `hmdp_upgrade_v2`;
