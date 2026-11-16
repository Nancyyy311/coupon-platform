package com.hmdp.enums;

/**
 * 用户角色。
 *
 * <p>为什么需要它（改造前的问题，面试可以直接讲）：
 * 原来只有"登录 / 未登录"两种状态，没有身份区分。匿名写接口堵掉之后，
 * 剩下的口子是：<b>任何登录用户都能 {@code POST /shop} 建店、{@code POST /voucher} 发券</b>。
 * 实测用普通账号发一张 10 元券能成功建成 id=11。这不是"忘记加校验"，
 * 而是数据模型里根本不存在"商家"这个概念——{@code tb_user} 连一列角色都没有。
 *
 * <p>设计上的三个取舍：
 * <ol>
 *   <li><b>角色放 token 里，不每请求查库</b>：和 nickName/icon 一样，属于"签发时快照"。
 *       代价是改角色要重新登录才生效（<b>降权也是重新登录后才生效</b>），
 *       收益是写接口零额外查询。降权不及时这一点必须主动交代，不能假装没有。</li>
 *   <li><b>未知角色一律降级为 USER</b>：库里出现脏值（手工改成 'merchant' 大小写、
 *       或将来加了新角色但旧代码不认识）时，宁可少给权限，也不能默认放行。</li>
 *   <li><b>只用两级</b>：USER / MERCHANT。不预先设计 ADMIN，
 *       因为没有后台管理入口，加了就是一个永远没人用的角色。</li>
 * </ol>
 */
public enum Role {

    /** 普通用户：能发笔记、点赞、关注、领券 */
    USER,

    /** 商家：在 USER 的全部能力之上，能建店、改店、发券（含秒杀券） */
    MERCHANT;

    /** 数据库 {@code tb_user.role} 的默认值，也是"角色为空时的兜底" */
    public static final String DEFAULT = "USER";

    /**
     * 把库里的字符串解析成枚举，<b>永不返回 null</b>。
     *
     * <p>这里不用 {@code valueOf}：它对 null 抛 NPE、对未知值抛 IllegalArgumentException，
     * 而"读到脏角色"不应该变成一次 500——那是数据问题，不是请求问题。
     */
    public static Role of(String raw) {
        if (raw == null) {
            return USER;
        }
        String v = raw.trim();
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(v)) {
                return r;
            }
        }
        return USER;
    }

    /** 归一化后的字符串，写库/写 token 前统一走这里，避免大小写和空格两种脏值 */
    public static String normalize(String raw) {
        return of(raw).name();
    }

    /**
     * Spring Security 的 {@code hasRole("MERCHANT")} 实际比对的是 {@code ROLE_MERCHANT}，
     * 前缀写错会变成"永远 403"或"永远放行"，所以统一由这里产出。
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
