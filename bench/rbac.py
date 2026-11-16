#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
验收脚本共用的"测试账号角色切换"。

为什么需要这个模块：
    角色是写在 JWT 里的（签发时快照），所以想拿到商家 token 必须经历
    「改库 -> 重新登录」两步。顺序反了就拿到 USER token，所有商家接口 403，
    而脚本只看 print 不看断言时，"跑完了"和"跑对了"看起来一模一样。

用法：
    import rbac
    rbac.promote()      # 把测试号改成 MERCHANT（改库，不登录）
    token = login()     # 你现在拿到的 token 带商家权限
    ...
    rbac.demote()       # 还原成 USER，必须在 finally 里调

约定：所有脚本共用同一个测试号，脚本之间不要并发跑。
"""

import subprocess

import _env

MYSQL = _env.MYSQL

# 库里已存在的用户（id=5），不是每次跑都新建账号
PHONE = "13456789001"


def _sql(query):
    r = subprocess.run(MYSQL + [query], capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    return r.stdout.strip()


def ensure(phone=PHONE):
    """测试号不存在就先插一行，保证脚本在全新库上也能跑。

    登录接口本身是"登录即注册"，但 promote() 必须发生在登录之前（角色是签发时的快照），
    顺序不能反。所以这里不能指望登录去建号，否则 UPDATE 影响 0 行，
    拿到的还是 USER token，商家接口全 403。
    """
    _sql("INSERT INTO tb_user (phone, nick_name, icon, role) "
         "SELECT '%s', '验收账号', '', 'USER' FROM DUAL "
         "WHERE NOT EXISTS (SELECT 1 FROM tb_user WHERE phone='%s')" % (phone, phone))


def set_role(role, phone=PHONE):
    ensure(phone)
    _sql("UPDATE tb_user SET role='%s' WHERE phone='%s'" % (role, phone))


def promote(phone=PHONE):
    set_role("MERCHANT", phone)


def demote(phone=PHONE):
    set_role("USER", phone)


def current_role(phone=PHONE):
    return _sql("SELECT role FROM tb_user WHERE phone='%s'" % phone)


def merchant_count():
    v = _sql("SELECT COUNT(*) FROM tb_user WHERE role='MERCHANT'")
    return int(v) if v.isdigit() else -1
