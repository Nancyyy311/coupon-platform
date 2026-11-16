#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
角色体系（RBAC）回归：谁能在什么接口上做什么。

用法:
    python probe_rbac.py            # 默认 8081
    python probe_rbac.py 8082

为什么单独一个脚本:
    accept_all.py 验的是"匿名能不能写"（401），probe_write_guard.py 验的是
    "商家写坏了参数报什么文案"。中间还缺一层：**登录了但身份不对，应该是 403**。
    这三层是三个不同的失败语义，混在一起测的话，401/403/200 会互相掩盖。

覆盖:
    A. 匿名      -> 401（未认证）
    B. 普通用户   -> 403（已认证但角色不够）+ 文案例子含"商家"
    C. 普通用户   -> 该能做的仍然能做（发笔记/签到/查详情），不能因为加校验误伤
    D. 商家       -> 能建店、能发券
    E. 降权的已知代价：改库后旧 token 仍是旧权限，重新登录才生效
                   —— 这条是**故意验的**，属于无状态认证的固有取舍，不是 bug

数据:
    建的店铺/券/笔记都会删掉，自增回退，测试账号角色还原成 USER。
"""

import json
import subprocess
import sys
import urllib.error
import urllib.request

import rbac

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8081
BASE = "http://127.0.0.1:%d" % PORT
import _env

MYSQL = _env.MYSQL
REDIS_CLI = _env.REDIS
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

PHONE = rbac.PHONE

OK, FAIL, FIVEXX = [], [], []


def http(path, method="GET", token=None, body=None):
    req = urllib.request.Request(BASE + path, method=method)
    if token:
        req.add_header("authorization", token)
    data = None
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        req.add_header("Content-Type", "application/json")
    try:
        with OPENER.open(req, data=data, timeout=20) as r:
            raw = r.read().decode("utf-8", "replace")
            if r.status >= 500:
                FIVEXX.append((path, r.status))
            try:
                return r.status, json.loads(raw)
            except ValueError:
                return r.status, raw
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        if e.code >= 500:
            FIVEXX.append((path, e.code))
        try:
            return e.code, json.loads(raw)
        except ValueError:
            return e.code, raw[:200]
    except Exception as e:
        FIVEXX.append((path, -1))
        return -1, str(e)


def sql(q):
    r = subprocess.run(MYSQL + [q], capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout.strip()


def redis(*args):
    r = subprocess.run(REDIS_CLI + list(args), capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    return r.stdout.strip()


def login():
    """种一条验证码直接登录（不改角色，用当前库里的角色换 token）"""
    redis("set", "login:code:" + PHONE, "246810", "EX", "300")
    st, resp = http("/user/login", "POST", body={"phone": PHONE, "code": "246810"})
    redis("del", "login:code:" + PHONE)
    if st == 200 and isinstance(resp, dict) and resp.get("success"):
        return resp["data"]
    raise RuntimeError("登录失败: %s %s" % (st, resp))


def check(name, ok, detail=""):
    (OK if ok else FAIL).append(name)
    print(("  PASS  " if ok else "  FAIL  ") + name + (("   | " + detail) if detail else ""))


def msg_of(resp):
    return (resp.get("errorMsg") if isinstance(resp, dict) else str(resp)) or ""


def expect_status(name, st, resp, want, keyword=None):
    ok = st == want
    if ok and keyword:
        ok = keyword in msg_of(resp)
    check(name, ok, "http=%s msg=%s" % (st, msg_of(resp)[:60]))


def overlong_shop():
    """合法结构、只有名字超长：用来区分"被鉴权挡下"和"被参数校验挡下"。
    商家调用它应该 200 + 长度文案；普通用户调用它应该 403（连反序列化都到不了校验那一步）。"""
    return {"name": "长" * 200, "typeId": 1, "images": "rbac.jpg", "address": "x",
            "x": 120.14930, "y": 30.31610, "sold": 0, "comments": 0, "score": 40}


def main():
    print("目标实例: %s" % BASE)
    st, _ = http("/shop-type/list")
    if st != 200:
        print("实例不可用，退出")
        return 1

    created_shop = created_voucher = created_blog = None
    before = {t: sql("SELECT COUNT(*) FROM %s" % t) for t in
              ("tb_shop", "tb_voucher", "tb_blog")}

    try:
        rbac.demote(PHONE)
        user_token = login()

        print("\n== A. 匿名：商家写接口应该 401（未认证） ==")
        for path, method, body in (("/shop", "POST", overlong_shop()),
                                   ("/shop", "PUT", {"id": 1, "name": "匿名改名"}),
                                   ("/voucher", "POST", {"title": "匿名券", "payValue": 1, "actualValue": 1}),
                                   ("/voucher/seckill", "POST", {"title": "匿名秒杀", "payValue": 1,
                                                                 "actualValue": 1, "stock": 1})):
            st, resp = http(path, method, body=body)
            expect_status("匿名 %s %s -> 401" % (method, path), st, resp, 401, "请先登录")

        print("\n== B. 普通用户：已登录但角色不够，应该 403 ==")
        for path, method, body in (("/shop", "POST", overlong_shop()),
                                   ("/shop", "PUT", {"id": 1, "name": "普通人改名"}),
                                   ("/voucher", "POST", {"title": "普通人发券", "payValue": 1, "actualValue": 1}),
                                   ("/voucher/seckill", "POST", {"title": "普通人秒杀", "payValue": 1,
                                                                 "actualValue": 1, "stock": 1,
                                                                 "beginTime": "2026-11-09T20:00:00",
                                                                 "endTime": "2026-11-09T22:00:00"})):
            st, resp = http(path, method, token=user_token, body=body)
            expect_status("普通用户 %s %s -> 403" % (method, path), st, resp, 403, "商家")

        print("\n== C. 普通用户不被误伤（加校验不能把正常功能一起关掉） ==")
        st, resp = http("/shop/1")
        check("游客/用户都能看店铺详情", st == 200 and isinstance(resp, dict) and resp.get("success") is True)
        st, resp = http("/voucher/list/1")
        check("能看券列表", st == 200 and isinstance(resp, dict) and resp.get("success") is True)
        st, resp = http("/user/sign", "POST", token=user_token)
        check("能签到（普通用户写操作）", st == 200 and isinstance(resp, dict) and resp.get("success") is True)
        st, resp = http("/blog", "POST", token=user_token,
                        body={"shopId": 1, "title": "RBAC回归笔记",
                              "images": "rbac.jpg", "content": "验证普通用户发笔记不受角色校验影响"})
        created_blog = resp.get("data") if isinstance(resp, dict) else None
        check("能发笔记（普通用户写操作）",
              st == 200 and isinstance(resp, dict) and resp.get("success") is True,
              "id=%s" % created_blog)

        print("\n== D. 商家：同样的接口应该放行 ==")
        rbac.promote(PHONE)
        merchant_token = login()
        st, resp = http("/shop", "POST", token=merchant_token, body=overlong_shop())
        # 注意断言的是"过了鉴权、进到参数校验"，不是创建成功——
        # 这条同时证明了两件事：403 消失了，且参数校验仍然在把关
        expect_status("商家 POST /shop 能通过鉴权（被参数校验拦下）", st, resp, 200, "最多 128 个字符")

        st, resp = http("/shop", "POST", token=merchant_token,
                        body={"name": "RBAC验证店", "typeId": 1, "images": "rbac.jpg", "area": "西湖区",
                              "address": "文三路 2 号", "x": 120.14931, "y": 30.31611,
                              "avgPrice": 50, "sold": 0, "comments": 0, "score": 45, "openHours": "10-22"})
        created_shop = resp.get("data") if isinstance(resp, dict) else None
        check("商家能建店", st == 200 and created_shop is not None, "id=%s" % created_shop)

        st, resp = http("/voucher", "POST", token=merchant_token,
                        body={"title": "RBAC验证券", "payValue": 1, "actualValue": 10,
                              "type": 0, "status": 1, "shopId": 1})
        created_voucher = resp.get("data") if isinstance(resp, dict) else None
        check("商家能发券", st == 200 and created_voucher is not None, "id=%s" % created_voucher)

        print("\n== E. 已知代价：降权要重新登录才生效（不是 bug，是要交代的取舍） ==")
        rbac.demote(PHONE)
        st, resp = http("/shop", "POST", token=merchant_token, body=overlong_shop())
        check("改库降权后，旧 token 仍是旧权限（仍能过鉴权）", st == 200 and "商家" not in msg_of(resp),
              "http=%s msg=%s" % (st, msg_of(resp)[:40]))
        fresh_token = login()
        st, resp = http("/shop", "POST", token=fresh_token, body=overlong_shop())
        expect_status("重新登录后降权立即生效 -> 403", st, resp, 403, "商家")

    finally:
        print("\n== F. 还原 ==")
        if created_shop:
            sql("DELETE FROM tb_shop WHERE id=%s" % created_shop)
            redis("zrem", "shop:geo:1", str(created_shop))
            redis("del", "cache:shop:%s" % created_shop)
        if created_voucher:
            sql("DELETE FROM tb_voucher WHERE id=%s" % created_voucher)
        if created_blog:
            sql("DELETE FROM tb_blog WHERE id=%s" % created_blog)
            redis("del", "blog:liked:%s" % created_blog)
        sql("ALTER TABLE tb_shop AUTO_INCREMENT=15")
        sql("ALTER TABLE tb_voucher AUTO_INCREMENT=11")
        rbac.demote(PHONE)

        for t in ("tb_shop", "tb_voucher", "tb_blog"):
            after = sql("SELECT COUNT(*) FROM %s" % t)
            check("%s 行数已还原" % t, after == before[t], "%s -> %s" % (before[t], after))

        check("测试账号角色已还原为 USER", rbac.current_role(PHONE) == "USER",
              rbac.current_role(PHONE))
        check("库里没有遗留商家账号", rbac.merchant_count() == 0,
              "MERCHANT=%d" % rbac.merchant_count())
        check("全程没有 5xx", not FIVEXX, str(FIVEXX[:3]))

    print("\n" + "-" * 56)
    print("通过 %d 项 / 失败 %d 项 / 5xx %d 次" % (len(OK), len(FAIL), len(FIVEXX)))
    print("结论: %s" % ("全部通过" if not FAIL else "有失败项"))
    return len(FAIL)


if __name__ == "__main__":
    sys.exit(main())
