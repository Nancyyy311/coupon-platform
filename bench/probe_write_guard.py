#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
写入口的参数守卫回归：畸形入参必须被"明确拒绝"，正常入参必须不被误伤。

用法:
    python probe_write_guard.py            # 默认 8081
    python probe_write_guard.py 8082

为什么单独一个脚本：
    读接口的畸形入参（页码、坐标）在 probe_input_guard.py 里，走 GET，匿名就能打。
    这里全部是**写接口**（POST/PUT /shop、/voucher、/voucher/seckill），需要登录，
    而且会产生数据——所以每条正常写入都在 finally 里删干净，并核对基线回到原值。

判定口径（与项目约定一致）:
    调用方错 -> HTTP 200 + success=false + 能指导修改的文案
    服务端意外异常 -> HTTP 5xx（本项目里一律是"服务器异常"）
    所以下面每条都要同时看状态码、success、文案，任何 5xx 直接算失败。
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
REDIS = _env.REDIS
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

# 登录用固定测试号：它是库里已存在的用户（id=5），不会因此新建账号
PHONE = "13456789001"

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
    r = subprocess.run(REDIS + list(args), capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout.strip()


def login():
    # 角色写在 JWT 里，所以必须"先改库、再登录"。
    # 顺序反了拿到的是 USER token，下面 12 条畸形入参全部变成 403，
    # 脚本会误报成"校验没生效"。
    rbac.promote(PHONE)
    redis("set", "login:code:" + PHONE, "246810", "EX", "300")
    st, resp = http("/user/login", "POST", body={"phone": PHONE, "code": "246810"})
    if st == 200 and isinstance(resp, dict) and resp.get("success"):
        return resp["data"]
    raise RuntimeError("登录失败: %s %s" % (st, resp))


def check(name, ok, detail=""):
    (OK if ok else FAIL).append(name)
    print(("  PASS  " if ok else "  FAIL  ") + name + (("   | " + detail) if detail else ""))


def expect_reject(token, name, path, body, keyword):
    """写接口的畸形入参：必须 200 + success=false + 文案含 keyword"""
    st, resp = http(path, "POST" if not path.startswith("PUT") else "PUT", token=token, body=body)
    msg = resp.get("errorMsg") if isinstance(resp, dict) else str(resp)
    ok = st == 200 and isinstance(resp, dict) and resp.get("success") is False and keyword in (msg or "")
    check(name, ok, "http=%s msg=%s" % (st, msg))


def base_shop(name="守卫测试店"):
    return {
        "name": name, "typeId": 1, "images": "guard.jpg", "area": "西湖区",
        "address": "文三路 1 号", "x": 120.14930, "y": 30.31610,
        "avgPrice": 50, "sold": 0, "comments": 0, "score": 45, "openHours": "10:00-22:00",
    }


def main():
    print("目标实例: %s" % BASE)
    st, _ = http("/shop-type/list")
    if st != 200:
        print("实例不可用，退出")
        return 1

    token = login()
    before_shop = sql("SELECT COUNT(*) FROM tb_shop")
    before_voucher = sql("SELECT COUNT(*) FROM tb_voucher")
    created_shop = created_voucher = None

    try:
        print("\n== A. 店铺写入（9 类畸形入参） ==")
        bad = base_shop()
        bad["name"] = "长" * 200
        expect_reject(token, "店名 200 字", "/shop", bad, "店铺名称最多 128 个字符")

        bad = base_shop(); bad["x"] = -1
        expect_reject(token, "坐标为负（列是 unsigned）", "/shop", bad, "坐标不能为负数")

        bad = base_shop(); bad["x"] = 999
        expect_reject(token, "坐标越界（Web Mercator）", "/shop", bad, "坐标不合法")

        bad = base_shop(); bad["sold"] = -5
        expect_reject(token, "销量为负", "/shop", bad, "销量不能为负数")

        bad = base_shop(); bad["score"] = 300
        expect_reject(token, "评分超出 tinyint 范围", "/shop", bad, "评分需在 0-255 之间")

        bad = base_shop(); bad.pop("address")
        expect_reject(token, "缺必填地址", "/shop", bad, "店铺地址不能为空")

        bad = base_shop(); bad["openHours"] = "10" * 40
        expect_reject(token, "营业时间超 32 字", "/shop", bad, "营业时间最多 32 个字符")

        print("\n== B. 券写入（5 类畸形入参） ==")
        expect_reject(token, "券标题 300 字", "/voucher",
                      {"title": "券" * 300, "payValue": 1, "actualValue": 10}, "券标题最多 255 个字符")
        expect_reject(token, "支付金额为负", "/voucher",
                      {"title": "负金额券", "payValue": -1, "actualValue": 10}, "支付金额不能为负数")
        expect_reject(token, "秒杀券缺开始时间", "/voucher/seckill",
                      {"title": "秒杀券", "payValue": 1, "actualValue": 10, "stock": 10,
                       "endTime": "2026-11-09T22:00:00"}, "活动开始时间不能为空")
        expect_reject(token, "秒杀券时间倒置", "/voucher/seckill",
                      {"title": "秒杀券", "payValue": 1, "actualValue": 10, "stock": 10,
                       "beginTime": "2026-11-09T22:00:00", "endTime": "2026-11-09T21:00:00"},
                      "活动结束时间必须晚于开始时间")
        expect_reject(token, "秒杀券库存为负", "/voucher/seckill",
                      {"title": "秒杀券", "payValue": 1, "actualValue": 10, "stock": -1,
                       "beginTime": "2026-11-09T20:00:00", "endTime": "2026-11-09T22:00:00"},
                      "秒杀库存不能为负数")

        print("\n== C. 正常写入不被误伤 ==")
        st, resp = http("/shop", "POST", token=token, body=base_shop())
        created_shop = resp.get("data") if isinstance(resp, dict) else None
        check("正常新增店铺成功", st == 200 and isinstance(resp, dict) and resp.get("success") is True
              and created_shop is not None, "id=%s" % created_shop)

        if created_shop:
            st, detail = http("/shop/%s" % created_shop)
            check("新店详情可查", st == 200 and isinstance(detail, dict) and detail.get("success") is True)
            st, nearby = http("/shop/of/type?typeId=1&current=1&x=120.149192&y=30.316078")
            hit = isinstance(nearby, dict) and any(str(s["id"]) == str(created_shop)
                                                   for s in (nearby.get("data") or []))
            check("新店已进 GEO，附近能查到", hit)

            st, resp = http("/shop", "PUT", token=token, body={"id": created_shop, "name": "守卫测试店-改名"})
            check("只改名字能通过（坐标用库里的补齐）",
                  st == 200 and isinstance(resp, dict) and resp.get("success") is True)
            st, detail = http("/shop/%s" % created_shop)
            check("改名立刻生效（缓存已失效）",
                  isinstance(detail, dict) and (detail.get("data") or {}).get("name") == "守卫测试店-改名")

        st, resp = http("/voucher", "POST", token=token,
                        body={"title": "守卫测试券", "subTitle": "满10减1", "rules": "仅限堂食",
                              "payValue": 1, "actualValue": 10, "type": 0, "status": 1, "shopId": 1})
        created_voucher = resp.get("data") if isinstance(resp, dict) else None
        check("正常新增券成功", st == 200 and isinstance(resp, dict) and resp.get("success") is True
              and created_voucher is not None, "id=%s" % created_voucher)

    finally:
        print("\n== D. 还原与基线核对 ==")
        if created_shop:
            sql("DELETE FROM tb_shop WHERE id=%s" % created_shop)
            redis("zrem", "shop:geo:1", str(created_shop))
            redis("del", "cache:shop:%s" % created_shop)
        if created_voucher:
            sql("DELETE FROM tb_voucher WHERE id=%s" % created_voucher)
        # 自增回退，保证重复跑不会把 id 越推越大
        sql("ALTER TABLE tb_shop AUTO_INCREMENT=15")
        sql("ALTER TABLE tb_voucher AUTO_INCREMENT=11")

        after_shop = sql("SELECT COUNT(*) FROM tb_shop")
        after_voucher = sql("SELECT COUNT(*) FROM tb_voucher")
        check("店铺数已还原", after_shop == before_shop, "%s -> %s" % (before_shop, after_shop))
        check("券数已还原", after_voucher == before_voucher, "%s -> %s" % (before_voucher, after_voucher))

        db_ids = set(sql("SELECT id FROM tb_shop").split())
        # GEO 是**按分类分 key** 的（shop:geo:{typeId}），所以要比的是所有分类的并集。
        # 只比 shop:geo:1 会把分类 2 的店当成"缺失"，这个断言本身写错过一次。
        geo_ids = set()
        for type_id in sql("SELECT DISTINCT type_id FROM tb_shop").split():
            geo_ids |= set(redis("zrange", "shop:geo:%s" % type_id, "0", "-1").split())
        check("GEO 成员与数据库一致", db_ids == geo_ids,
              "DB=%d GEO=%d 差集=%s" % (len(db_ids), len(geo_ids),
                                       sorted(db_ids ^ geo_ids)[:5]))

        check("全程没有 5xx", not FIVEXX, str(FIVEXX[:3]))

        # 角色是本脚本自己改的，必须自己还原——否则下次跑 accept_all
        # 会看到一个"意外的商家"，而且库里会长期留下越权账号
        rbac.demote(PHONE)
        check("测试账号角色已还原", rbac.current_role(PHONE) == "USER",
              "phone=%s role=%s" % (PHONE, rbac.current_role(PHONE)))
        check("库里没有遗留商家账号", rbac.merchant_count() == 0,
              "MERCHANT=%d" % rbac.merchant_count())

    print("\n" + "-" * 56)
    print("通过 %d 项 / 失败 %d 项 / 5xx %d 次" % (len(OK), len(FAIL), len(FIVEXX)))
    print("结论: %s" % ("全部通过" if not FAIL else "有失败项"))
    return len(FAIL)


if __name__ == "__main__":
    sys.exit(main())
