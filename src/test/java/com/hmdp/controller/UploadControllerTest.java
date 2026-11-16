package com.hmdp.controller;

import com.hmdp.dto.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传接口的边界测试。
 *
 * 只测"拒绝"分支：通过分支会把文件真的写到 nginx 的 imgs 目录里，
 * 单测不应该产生磁盘副作用（那属于集成测试的活，已经在真机上验过）。
 *
 * 这里守的是一条线：**文件名参数来自请求，任何时候都不能直接拼进 File**。
 * 修复前 `new File(IMAGE_UPLOAD_DIR, filename)` 对 `../` 完全不做处理，
 * 实测能删掉 imgs 上一级的文件。
 */
class UploadControllerTest {

    private final UploadController controller = new UploadController();

    @Test
    @DisplayName("合法文件名：服务端生成格式的才放行")
    void acceptsServerGeneratedName() {
        assertTrue(UploadController.isValidStoredName(
                "/blogs/12/13/cac38c28-79a3-439b-9805-74f51353f6b3.png"));
        assertTrue(UploadController.isValidStoredName(
                "/blogs/1/2/00000000-0000-0000-0000-000000000000.jpg"));
    }

    @Test
    @DisplayName("路径穿越：../ 与绝对路径一律拒绝")
    void rejectsTraversal() {
        assertFalse(UploadController.isValidStoredName("../authz-escape-test.txt"));
        assertFalse(UploadController.isValidStoredName("/blogs/1/2/../../pom.xml"));
        assertFalse(UploadController.isValidStoredName("D:\\data\\pom.xml"));
        assertFalse(UploadController.isValidStoredName("/etc/passwd"));
        assertFalse(UploadController.isValidStoredName(""));
        assertFalse(UploadController.isValidStoredName(null));
    }

    @Test
    @DisplayName("非图片后缀：即使格式对也不给删")
    void rejectsNonImageSuffix() {
        assertFalse(UploadController.isValidStoredName(
                "/blogs/12/13/cac38c28-79a3-439b-9805-74f51353f6b3.html"));
        assertFalse(UploadController.isValidStoredName(
                "/blogs/12/13/cac38c28-79a3-439b-9805-74f51353f6b3.svg"));
    }

    @Test
    @DisplayName("上传 .html：直接拒绝，不落盘")
    void rejectsHtmlUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "xss.html", "text/html", "<script>alert(1)</script>".getBytes());

        Result result = controller.uploadImage(file);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMsg().contains("只允许上传"));
    }

    @Test
    @DisplayName("上传空文件：拒绝")
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        Result result = controller.uploadImage(file);

        assertFalse(result.getSuccess());
    }
}
