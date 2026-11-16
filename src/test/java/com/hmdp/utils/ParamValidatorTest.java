package com.hmdp.utils;

import com.hmdp.exception.ParamException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 参数校验器本身的用例。
 *
 * 这一层存在的理由：数据库的 varchar(n) 与 unsigned 列不会在应用层拦人，
 * 超长和负数要等 MySQL 报 "Data too long" / "Out of range"，
 * 最后被兜成一句"服务器异常"。这里把"什么算不合法"固定下来，
 * 改阈值时至少有一条用例会拦住。
 */
class ParamValidatorTest {

    @Test
    @DisplayName("必填文本：空白与 null 都拒绝，报的是字段名")
    void requireTextRejectsBlank() {
        assertEquals("店铺名称不能为空",
                assertThrows(ParamException.class,
                        () -> ParamValidator.requireText("   ", 128, "店铺名称")).getMessage());
        assertEquals("店铺名称不能为空",
                assertThrows(ParamException.class,
                        () -> ParamValidator.requireText(null, 128, "店铺名称")).getMessage());
        assertDoesNotThrow(() -> ParamValidator.requireText("正常店名", 128, "店铺名称"));
    }

    @Test
    @DisplayName("长度按字符数算：128 个汉字放行，129 个拒绝")
    void lengthCountsCharacters() {
        String exactly = repeat('长', 128);
        assertDoesNotThrow(() -> ParamValidator.requireText(exactly, 128, "店铺名称"));

        ParamException e = assertThrows(ParamException.class,
                () -> ParamValidator.requireText(repeat('长', 129), 128, "店铺名称"));
        assertEquals("店铺名称最多 128 个字符", e.getMessage());
    }

    @Test
    @DisplayName("emoji 按 1 个字符算，不能因为它是 2 个 UTF-16 单元就被误判超长")
    void emojiCountsAsOneCharacter() {
        // 128 个 emoji：String.length() 是 256，按 UTF-16 单元数会被误判成超长；
        // varchar(128) 在 utf8mb4 下存的就是 128 个字符，应当放行。
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 128; i++) {
            sb.append("\uD83D\uDE00");
        }
        String emojis = sb.toString();
        assertEquals(256, emojis.length(), "前提：UTF-16 单元数是字符数的两倍");
        assertDoesNotThrow(() -> ParamValidator.requireText(emojis, 128, "店铺名称"));
    }

    @Test
    @DisplayName("选填文本：null 放行，显式空串拒绝（那是把数据改坏，不是没改）")
    void optionalTextAllowsNullButNotBlank() {
        assertDoesNotThrow(() -> ParamValidator.optionalText(null, 32, "营业时间"));
        assertEquals("营业时间不能为空",
                assertThrows(ParamException.class,
                        () -> ParamValidator.optionalText("", 32, "营业时间")).getMessage());
        assertDoesNotThrow(() -> ParamValidator.optionalText("10:00-22:00", 32, "营业时间"));
    }

    @Test
    @DisplayName("无符号列：负数拒绝，0 与正数放行")
    void notNegativeRejectsNegative() {
        assertDoesNotThrow(() -> ParamValidator.notNegative(0L, "支付金额"));
        assertDoesNotThrow(() -> ParamValidator.notNegative((Long) null, "支付金额"));
        assertEquals("支付金额不能为负数",
                assertThrows(ParamException.class,
                        () -> ParamValidator.notNegative(-1L, "支付金额")).getMessage());
        assertEquals("销量不能为负数",
                assertThrows(ParamException.class,
                        () -> ParamValidator.notNegative(-5, "销量")).getMessage());
    }

    @Test
    @DisplayName("区间：评分 0-255，越界两侧都拒绝")
    void rangeChecksBothEnds() {
        assertDoesNotThrow(() -> ParamValidator.range(0, 0, 255, "评分"));
        assertDoesNotThrow(() -> ParamValidator.range(255, 0, 255, "评分"));
        assertEquals("评分需在 0-255 之间",
                assertThrows(ParamException.class,
                        () -> ParamValidator.range(256, 0, 255, "评分")).getMessage());
        assertEquals("评分需在 0-255 之间",
                assertThrows(ParamException.class,
                        () -> ParamValidator.range(-1, 0, 255, "评分")).getMessage());
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
