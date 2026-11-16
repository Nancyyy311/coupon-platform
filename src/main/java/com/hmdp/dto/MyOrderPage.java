package com.hmdp.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 分页返回体。
 *
 * 为什么不用 Map 或者直接把 MyBatis-Plus 的 Page 序列化出去：
 *   Map 没有契约，前端只能靠"我猜字段叫 total"来写代码；
 *   Page 是框架类型，它的字段（pages、orders、optimizeCountSql…）会随版本变化，
 *   某次升级依赖就可能把接口字段改掉。用自己的类，字段名就是契约。
 */
@Data
@Accessors(chain = true)
public class MyOrderPage {

    private long total;

    private long current;

    private long size;

    private List<MyOrderVO> orders;
}
