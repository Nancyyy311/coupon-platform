package com.hmdp.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 图片上传 / 删除。
 *
 * 改造前这里是全项目最危险的一组接口，三个问题叠在一起：
 *   ① `/upload/**` 在 Security 白名单里 —— 不用登录就能传、能删
 *   ② 删除是 **GET** 请求，还能传 `../`：`name=../xxx.txt` 会删掉 imgs 上一级的文件。
 *      实测用 `../authz-escape-test.txt` 删掉了 nginx html 目录下的文件，返回 200。
 *   ③ 上传不校验后缀，`.html` 也能落到 nginx 的静态目录里，直接变成可访问页面
 *      （存储型 XSS 的经典路径：上传 → 拿到 URL → 诱导别人打开）
 *
 * 修法三件套，缺一件都不算堵住：
 *   1. 权限交给 Security：`/upload/**` 不再进白名单，写操作要求登录
 *   2. 后缀白名单 + 大小限制（yaml 里 multipart 配 5MB），非图片一律拒绝
 *   3. 删除改 POST，名字必须匹配服务端生成的格式，再用 canonicalPath 做一次兜底校验
 */
@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

    /** 只允许这几种图片后缀。不收 svg：它本身能内嵌 script，等于换个马甲继续 XSS */
    private static final Set<String> ALLOWED_SUFFIX = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));

    /**
     * 服务端生成的文件名格式：/blogs/{d1}/{d2}/{uuid}.{后缀}
     *   d1、d2 是 hashCode 的低 8 位拆成两个 0~15 的数（可能是 1 位也可能是 2 位）
     * 校验它的意义：把"用户传什么就删什么"变成"只能删服务端自己生成的文件"。
     * 只挡 `..` 是不够的，`/etc/passwd` 这种绝对路径同样要挡。
     */
    private static final Pattern STORED_NAME = Pattern.compile(
            "^/blogs/\\d{1,2}/\\d{1,2}/[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|gif|webp)$");

    /**
     * 上传根目录，由配置注入，默认落在工作目录下的 ./imgs/（已在 .gitignore 里）。
     *
     * 不再用常量写死绝对路径：换台机器、换个人 clone 都要能直接跑起来。
     * 想让 Nginx 直接托管上传的图片，把 HMDP_UPLOAD_DIR 指到它的静态目录即可：
     *   HMDP_UPLOAD_DIR=/var/www/hmdp/imgs/
     */
    @Value("${app.upload.dir:./imgs/}")
    private String uploadDir;

    /** 统一补上结尾分隔符，避免 "imgs" + "/blogs/..." 这种拼接依赖运气 */
    private File uploadBase() {
        String dir = uploadDir == null || uploadDir.trim().isEmpty() ? "./imgs/" : uploadDir.trim();
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            dir = dir + File.separator;
        }
        return new File(dir);
    }

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        String originalFilename = image.getOriginalFilename();
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        if (suffix == null || !ALLOWED_SUFFIX.contains(suffix.toLowerCase(Locale.ROOT))) {
            // 注意这里不回显用户传的后缀，避免把不可信内容原样写进日志/响应
            log.warn("拒绝非图片上传，原始文件名长度={}", originalFilename == null ? 0 : originalFilename.length());
            return Result.fail("只允许上传 jpg / png / gif / webp 图片");
        }
        try {
            String fileName = createNewFileName(originalFilename);
            image.transferTo(new File(uploadBase(), fileName));
            log.debug("文件上传成功，{}", fileName);
            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除图片。
     *
     * 为什么从 GET 改成 POST：GET 必须是幂等且无副作用的，浏览器预取、爬虫、图片加载
     * 都可能顺手触发一次。一个"删文件"的 GET 接口，等于把删除按钮交给了所有会发 GET 的东西。
     * 前端 `blog-edit.html` 的 deletePic 已同步改成 post。
     */
    @PostMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        if (!isValidStoredName(filename)) {
            log.warn("拒绝非法图片名删除请求，参数长度={}", filename == null ? 0 : filename.length());
            return Result.fail("错误的文件名称");
        }
        try {
            File base = uploadBase();
            File file = new File(base, filename);
            // 双保险：正则已经排除了 `..`，这里再比一次规范化后的路径。
            // 防御的是"正则写漏了某个变体"——校验逻辑本身也可能有 bug。
            if (!file.getCanonicalPath().startsWith(base.getCanonicalPath() + File.separator)) {
                log.warn("拒绝越界删除，规范化路径不在上传目录内");
                return Result.fail("错误的文件名称");
            }
            if (!file.isFile()) {
                return Result.fail("错误的文件名称");
            }
            FileUtil.del(file);
            return Result.ok();
        } catch (IOException e) {
            log.error("删除图片失败", e);
            return Result.fail("删除失败");
        }
    }

    /** 抽成静态方法是为了能直接单测：路径穿越、绝对路径、非法后缀都在这里挡 */
    static boolean isValidStoredName(String filename) {
        return filename != null && STORED_NAME.matcher(filename).matches();
    }

    private String createNewFileName(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = new File(uploadBase(), StrUtil.format("/blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix.toLowerCase(Locale.ROOT));
    }
}
