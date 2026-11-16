package com.hmdp.config;

import com.hmdp.dto.Result;
import com.hmdp.exception.ParamException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局异常出口的单元测试。
 *
 * 这里守的是一条纪律：**每一种异常都要说清是谁的错**。
 * 全部落到"服务器异常"的后果不是文案难看，而是排查方向从第一步就错了——
 * 参数写错的人会去查服务端日志，真正崩溃的那次反而因为 HTTP 200 而没人告警。
 */
class WebExceptionAdviceTest {

    private final WebExceptionAdvice advice = new WebExceptionAdvice();

    @Test
    @DisplayName("缺必填参数：报出参数名，而不是一句服务器异常")
    void missingParameter() {
        Result r = advice.handleMissingParameter(
                new MissingServletRequestParameterException("typeId", "Integer"));

        assertFalse(r.getSuccess());
        assertTrue(r.getErrorMsg().contains("typeId"), "错误信息要能直接指出缺了哪个参数");
    }

    @Test
    @DisplayName("请求体不是合法 JSON：归为请求体格式问题")
    void unreadableBody() {
        Result r = advice.handleUnreadableBody(
                new HttpMessageNotReadableException("Unexpected character '{'", (org.springframework.http.HttpInputMessage) null));

        assertFalse(r.getSuccess());
        assertEquals("请求体格式不正确", r.getErrorMsg());
    }

    @Test
    @DisplayName("Content-Type 不支持：回显实际类型，便于调用方改请求头")
    void mediaTypeNotSupported() {
        Result r = advice.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, java.util.Collections.emptyList()));

        assertFalse(r.getSuccess());
        assertTrue(r.getErrorMsg().contains("text/plain"));
    }

    @Test
    @DisplayName("上传超过 5MB：给的是大小提示，不是服务器异常")
    void uploadTooLarge() {
        Result r = advice.handleUploadTooLarge(new MaxUploadSizeExceededException(5 * 1024 * 1024));

        assertFalse(r.getSuccess());
        assertEquals("图片不能超过 5MB", r.getErrorMsg());
    }

    @Test
    @DisplayName("上传请求不是 multipart：提示改用 form-data，而不是服务器异常")
    void multipartFormatWrong() {
        Result r = advice.handleMultipart(new org.springframework.web.multipart.MultipartException(
                "Current request is not a multipart request"));

        assertFalse(r.getSuccess());
        assertTrue(r.getErrorMsg().contains("multipart/form-data"));
    }

    @Test
    @DisplayName("方法不支持：报出不支持的方法名")
    void methodNotSupported() {
        Result r = advice.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("GET"));

        assertFalse(r.getSuccess());
        assertTrue(r.getErrorMsg().contains("GET"));
    }

    /**
     * 这条是整个类里最关键的断言：兜底分支必须是 5xx。
     *
     * 如果它还是 200，/actuator 的 http.server.requests 里"崩溃"和"正常返回"
     * 完全相同，任何基于状态码的告警都不会触发——那前面的可观测改造就白做了。
     */
    @Test
    @DisplayName("兜底的运行时异常：返回 500，而不是 200")
    void runtimeExceptionIsServerError() throws NoSuchMethodException {
        Result r = advice.handleRuntimeException(new IllegalStateException("boom"));
        assertFalse(r.getSuccess());
        assertEquals("服务器异常", r.getErrorMsg());

        ResponseStatus status = WebExceptionAdvice.class
                .getMethod("handleRuntimeException", RuntimeException.class)
                .getAnnotation(ResponseStatus.class);
        assertNotNull(status, "兜底分支必须显式声明 HTTP 状态码");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status.value());
    }

    /**
     * 参数校验失败要"借用"上面的兜底分支，但不能变成 5xx。
     *
     * 两条断言缺一不可：
     *   ① 文案原样透出（"店铺名称最多 128 个字符"），否则用户改不动；
     *   ② 这个 handler 上**不能有** @ResponseStatus —— 一旦被加上 500，
     *      所有"你参数写错了"都会重新变成告警，把真故障淹掉。
     */
    @Test
    @DisplayName("参数校验失败：200 + 原样文案，且不能标成 5xx")
    void paramExceptionKeepsMessageAndStatus() throws NoSuchMethodException {
        Result r = advice.handleParamException(new ParamException("店铺名称最多 128 个字符"));

        assertFalse(r.getSuccess());
        assertEquals("店铺名称最多 128 个字符", r.getErrorMsg());

        ResponseStatus status = WebExceptionAdvice.class
                .getMethod("handleParamException", ParamException.class)
                .getAnnotation(ResponseStatus.class);
        assertNull(status, "参数错误的出口不能声明 5xx，否则 5xx 指标会被用户输入污染");
    }
}
