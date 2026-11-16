package com.hmdp.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
    /**
     * 角色，取值见 {@link com.hmdp.enums.Role}。
     *
     * 会随 JWT 一起签发：写接口的鉴权只看 token 里的这个字段，不再查库。
     * 所以改角色后旧 token 仍是旧权限，必须重新登录——降权同理，
     * 这是"无状态认证"的固有代价，不是遗漏。
     */
    private String role;
}
