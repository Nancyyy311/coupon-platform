package com.hmdp.utils;

public class SystemConstants {

    /**
     * 图片上传根目录不再写死成某台机器的绝对路径。
     *
     * 原来这里是课程配套的 Windows 绝对路径（形如 D:\...\nginx\html\hmdp\imgs\）——
     * 对作者本机可用，但对任何 clone 下来的人都是错的：目录不存在、上传即失败，
     * 而且把本机的目录结构一起暴露进了仓库。
     *
     * 现在由配置决定（见 {@code com.hmdp.controller.UploadController}）：
     *   app.upload.dir → 环境变量 HMDP_UPLOAD_DIR → 默认 ./imgs/
     * 部署时把它指向 Nginx 的静态目录即可，代码里不再出现任何绝对路径。
     */
    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;
}
