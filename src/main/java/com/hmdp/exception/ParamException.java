package com.hmdp.exception;

/**
 * 调用方传了不合法的参数。
 *
 * 与 RuntimeException 的区别只有一点，但决定了用户看到什么：
 *   这类异常是"调用方的错"，WebExceptionAdvice 把它翻成 200 + 具体原因；
 *   其余 RuntimeException 是"服务端的错"，必须是 5xx，否则监控看不见。
 *
 * 为什么不用 IllegalArgumentException：它是 JDK 里语义最模糊的异常之一，
 * 很多框架内部也会抛它（反射、类型转换）。共用一条 handler 会把框架内部的
 * 故障伪装成"参数错误"，把真正的 5xx 洗成 200。这里刻意用独立类型，
 * 保证只有我们自己主动抛出的校验失败才会走到那个出口。
 */
public class ParamException extends RuntimeException {

    public ParamException(String message) {
        super(message);
    }
}
