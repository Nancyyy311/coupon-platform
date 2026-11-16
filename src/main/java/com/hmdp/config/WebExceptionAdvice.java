package com.hmdp.config;

import com.hmdp.dto.Result;
import com.hmdp.exception.ParamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * 全局异常出口。
 *
 * 这个类要解决的不是"把异常吞掉"，而是**不要把所有异常都说成"服务器异常"**。
 * 改造前只有一条 `@ExceptionHandler(RuntimeException)`，于是下面这些全都被归成同一句话：
 *   参数类型不对、请求方法不对、请求体不是合法 JSON、缺必填参数、图片超过 5MB……
 * 用户看到的永远是"服务器异常"，而日志里只有一行栈，排查方向从一开始就是错的。
 *
 * 现在按"谁的责任"分类，并且统一走 Result 响应体：
 *
 *   调用方的错（参数/方法/请求体/超限）  -> HTTP 200 + success=false + 具体原因
 *   服务端自己的错（意料之外的运行时异常）-> HTTP 500 + success=false
 *
 * 为什么前一类不返回 4xx：前端 axios 是按 `res.data.success` 解析的，
 * 改成 4xx 会被 error 分支截走，用户就只能看到一个笼统的"请求失败"，
 * 反而丢失了"参数「current」格式不正确"这种能直接指导修改的信息。
 * 但**服务端异常必须是 5xx**——否则 /actuator 的 http.server.requests 里
 * 一次崩溃和一次正常响应长得一模一样，监控与告警全部失效，改造成本极高的可观测性等于零。
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 参数类型不对，比如 ?current=abc 传给 Integer。
     *
     * 不单独处理的话它会落到下面的 RuntimeException 分支，返回"服务器异常"——
     * 那等于把"调用方传错了参数"说成"服务端挂了"，排查时第一反应会去翻服务端日志，
     * 而日志里除了一行类型转换异常什么都没有。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[参数格式错误] name={} value={} 期望类型={}",
                e.getName(), e.getValue(), e.getRequiredType() == null ? "?" : e.getRequiredType().getSimpleName());
        return Result.fail("参数「" + e.getName() + "」格式不正确");
    }

    /**
     * 少了必填参数，比如 GET /shop/of/type 没带 typeId。
     *
     * 不接住的话它会走 Spring 默认的 /error，返回
     * `{"timestamp":...,"status":400,"error":"Bad Request","path":...}` ——
     * 字段名和 Result 完全不同，前端按 success 取值只会拿到 undefined，
     * 于是"参数没传"在页面上表现成"没有数据"。这比报错更难查。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("[缺少必填参数] name={} type={}", e.getParameterName(), e.getParameterType());
        return Result.fail("缺少必要参数「" + e.getParameterName() + "」");
    }

    /**
     * 请求体不是合法 JSON（多一个逗号、少一个引号、body 为空都会命中）。
     *
     * 这是调用方的问题，不该打 ERROR 级别的完整堆栈：堆栈只会指向
     * Jackson 的内部类，对定位没有帮助，却会在日志里制造大量噪声。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("[请求体解析失败] {}", e.getMessage());
        return Result.fail("请求体格式不正确");
    }

    /**
     * Content-Type 不支持，比如把 JSON 用 text/plain 发上来。
     * 不接住会返回 Spring 默认的 415，响应体同样不是 Result 结构。
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("[Content-Type 不支持] contentType={} supported={}", e.getContentType(), e.getSupportedMediaTypes());
        return Result.fail("不支持的 Content-Type：" + e.getContentType());
    }

    /**
     * 上传文件超过 multipart 上限（yaml 里配的 5MB）。
     *
     * 这条最容易被忽略：异常在 MultipartResolver 解析阶段就抛了，
     * 根本走不到 UploadController 的后缀白名单，
     * 所以用户在"传了张 6MB 的照片"时看到的是"服务器异常"。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleUploadTooLarge(MaxUploadSizeExceededException e) {
        log.warn("[上传超限] maxUploadSize={}", e.getMaxUploadSize());
        return Result.fail("图片不能超过 5MB");
    }

    /**
     * 上传请求本身不是 multipart，比如把 Content-Type 写成 application/json 就往上发。
     *
     * 这是 MultipartException 的父类分支，必须和 MaxUploadSizeExceededException 分开：
     * 后者是"文件太大"，前者是"请求格式根本不对"，两者给用户的处置完全不同。
     * 注意别把这个分支写成 5xx —— 它依然是调用方的问题。
     */
    @ExceptionHandler(MultipartException.class)
    public Result handleMultipart(MultipartException e) {
        log.warn("[上传请求格式错误] {}", e.getMessage());
        return Result.fail("上传请求格式不正确，请使用 multipart/form-data");
    }

    /**
     * 请求方法不对，比如把 POST /user/login 写成了 GET。
     *
     * 这一条同样不该报"服务器异常"：它是 4xx 的调用错误，不是 5xx。
     * 而且前端把 POST 写成 GET 时，看到"服务器异常"会去查后端，
     * 实际改一行 axios 就解决了。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[请求方法不支持] method={} supported={}", e.getMethod(), e.getSupportedHttpMethods());
        return Result.fail("不支持 " + e.getMethod() + " 请求");
    }

    /**
     * 写入参数不合法（店名超长、坐标为负、金额为负……），见 ParamValidator。
     *
     * 这些值本来会一路传到 MySQL，由 STRICT_TRANS_TABLES 报
     * "Data too long" / "Out of range value"，再被下面的兜底分支翻成 500 + "服务器异常"。
     * 用户写多了几个字却看到"服务器异常"，排查的人第一反应也会去翻服务端日志。
     *
     * 单独接住之后有两个收益：
     *   ① 文案能直接指导修改（"店铺名称最多 128 个字符"）；
     *   ② 它不再以 5xx 的形式出现在 /actuator 的指标里，把 5xx 留给真正的故障。
     *
     * 用独立异常类型而不是 IllegalArgumentException：后者在框架内部也会抛，
     * 共用一条 handler 会把框架故障伪装成"参数错误"，等于给 5xx 开了个后门。
     */
    @ExceptionHandler(ParamException.class)
    public Result handleParamException(ParamException e) {
        log.warn("[参数校验失败] {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 兜底：走到这里说明是代码没预料到的问题，属于真正的服务端故障。
     *
     * 与上面几条的区别只有一点，但很关键：**这里返回 5xx**。
     * 200 会让所有基于状态码的监控看不见它，而这条分支恰恰是唯一需要告警的分支。
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }
}
