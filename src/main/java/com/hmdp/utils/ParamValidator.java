package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import com.hmdp.exception.ParamException;

/**
 * 写入入口的参数校验：按**数据库列的口径**提前拦。
 *
 * 为什么需要这一层（实测出来的，不是假设）：
 *   tb_shop.name 是 varchar(128)、x/y 是 double unsigned、sold 是 int unsigned，
 *   MySQL 又开着 STRICT_TRANS_TABLES。于是：
 *     店名 200 字  -> "Data too long for column 'name'"
 *     坐标为 -1    -> "Out of range value for column 'x'"
 *     销量 -5      -> "Out of range value for column 'sold'"
 *   三条都通过了 controller（那边一个字都没校验），到数据库才炸，
 *   最后被全局异常处理器兜成 HTTP 500 + "服务器异常"。
 *   写多了几个字、填错一个数字，得到的是一句无从下手的话。
 *
 * 为什么返回方式是抛 ParamException 而不是 Result：
 *   save() 的签名是 MyBatis-Plus 的 boolean，返回值里放不下错误信息。
 *   抛异常后由 WebExceptionAdvice 统一翻成 200 + 具体文案，
 *   与"缺参数""请求体格式不正确"这些入口的错误形态保持一致。
 *
 * 字符数口径：varchar(n) 在 utf8mb4 下数的是**字符**不是字节，也不等于
 * Java 的 String.length()（后者数 UTF-16 单元，一个 emoji 算 2）。
 * 所以统一用 codePointCount，避免"50 个 emoji 被自己的校验判成 100 个字"。
 */
public final class ParamValidator {

    private ParamValidator() {
    }

    /** 必填对象（枚举值、金额、时间这类） */
    public static void requirePresent(Object value, String field) {
        if (value == null) {
            throw new ParamException(field + "不能为空");
        }
    }

    /** 必填文本：不能是空白，且不超过列宽 */
    public static void requireText(String value, int maxLength, String field) {
        if (StrUtil.isBlank(value)) {
            throw new ParamException(field + "不能为空");
        }
        checkLength(value, maxLength, field);
    }

    /**
     * 选填文本：没传就跳过；传了就必须合法。
     * "显式传空串"也算错——部分更新时把名称改成空串，是把数据改坏，不是"没改"。
     */
    public static void optionalText(String value, int maxLength, String field) {
        if (value == null) {
            return;
        }
        if (StrUtil.isBlank(value)) {
            throw new ParamException(field + "不能为空");
        }
        checkLength(value, maxLength, field);
    }

    private static void checkLength(String value, int maxLength, String field) {
        if (value.codePointCount(0, value.length()) > maxLength) {
            throw new ParamException(field + "最多 " + maxLength + " 个字符");
        }
    }

    /** 无符号列：负数不会"被忽略"，MySQL 会直接报 Out of range */
    public static void notNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw new ParamException(field + "不能为负数");
        }
    }

    public static void notNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new ParamException(field + "不能为负数");
        }
    }

    /** 有明确取值区间的数字（比如评分列是 tinyint unsigned） */
    public static void range(Integer value, int min, int max, String field) {
        if (value == null) {
            return;
        }
        if (value < min || value > max) {
            throw new ParamException(field + "需在 " + min + "-" + max + " 之间");
        }
    }
}
