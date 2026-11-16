#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
重启后一键自查：把所有轮次的修复在"当前进程"上验一遍。

用法:
    python accept_all.py            # 默认 8081（IDEA 跑的实例）
    python accept_all.py 8082       # 指定端口

设计原则:
 1. 只读为主；必须写入的用例（点赞/关注/注册）跑完立刻还原，并核对还原结果。
 2. 不吞异常：任何一项 5xx 都算失败。
 3. 退出码 = 失败项数，方便 CI 或手动判断。
"""

import json
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8081
BASE = "http://127.0.0.1:%d" % PORT
import _env

MYSQL = _env.MYSQL
REDIS = _env.REDIS
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

OK, FAIL = [], []
CODES = []          # 记录所有见过的 HTTP 状态码，最后检查有无 5xx


def http(path, method="GET", token=None, body=None):
    req = urllib.request.Request(BASE + path, method=method)
    if token:
        req.add_header("authorization", token)
    data = None
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        req.add_header("Content-Type", "application/json")
    try:
        with OPENER.open(req, data=data, timeout=15) as r:
            CODES.append(r.status)
            raw = r.read().decode("utf-8", "replace")
            try:
                return r.status, json.loads(raw)
            except ValueError:
                return r.status, raw
    except urllib.error.HTTPError as e:
        CODES.append(e.code)
        return e.code, e.read().decode("utf-8", "replace")[:200]
    except Exception as e:                       # 连不上等
        CODES.append(-1)
        return -1, str(e)


def sql(q):
    r = subprocess.run(MYSQL + [q], capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout.strip()


def redis(*args):
    r = subprocess.run(REDIS + list(args), capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout.strip()


def check(name, cond, detail=""):
    (OK if cond else FAIL).append(name)
    tag = "  PASS  " if cond else "  FAIL  "
    print(tag + name + (("   | " + detail) if detail else ""))


def section(title):
    print("\n== %s ==" % title)


def login(phone, code="246810"):
    """直接往 Redis 塞一个码，换 token。返回 (token, status)"""
    redis("set", "login:code:" + phone, code, "EX", "300")
    st, resp = http("/user/login", "POST", body={"phone": phone, "code": code})
    if st == 200 and isinstance(resp, dict) and resp.get("success"):
        return resp["data"], st
    return None, st


def main():
    print("目标实例: %s" % BASE)
    st, _ = http("/shop-type/list")
    if st != 200:
        print("实例不可用，退出")
        return 1

    # ---------------- A. 可观测端点的暴露面 ----------------
    section("A. actuator 暴露面")
    st, _ = http("/actuator/health")
    check("health 探针可访问", st == 200, "http=%s" % st)
    st, _ = http("/actuator/env")
    check("env 未暴露（不能读到数据库密码）", st == 404, "http=%s" % st)
    st, _ = http("/actuator/heapdump")
    check("heapdump 未暴露", st == 404, "http=%s" % st)

    # ---------------- B. 游客可读（不能修过头） ----------------
    section("B. 游客可读接口")
    for p in ["/shop/1", "/voucher/list/1", "/shop-type/list", "/blog/hot",
              "/shop/of/type?typeId=1&current=1&x=120.149192&y=30.316078"]:
        st, resp = http(p)
        ok = st == 200 and isinstance(resp, dict) and resp.get("success")
        check("游客 GET %s" % p.split("?")[0], ok, "http=%s" % st)

    # ---------------- C. 匿名写必须全拦 ----------------
    section("C. 匿名写接口（按方法收紧白名单）")
    anon = [
        ("POST", "/voucher", {"shopId": 1, "title": "应被拦", "payValue": 1, "actualValue": 1, "type": 0}),
        ("PUT", "/shop", {"id": 1, "name": "应被拦"}),
        ("POST", "/upload/blog", None),
        ("POST", "/upload/blog/delete?name=../x.txt", None),
    ]
    for m, p, b in anon:
        st, _ = http(p, m, body=b)
        check("匿名 %s %s -> 401" % (m, p.split("?")[0]), st == 401, "http=%s" % st)

    # ---------------- D. 登录链路 ----------------
    section("D. 登录加固")
    p1 = "13456780001"
    redis("del", "login:code:" + p1, "login:code:limit:" + p1, "login:code:daily:" + p1)
    st1, r1 = http("/user/code?phone=" + p1, "POST")
    st2, r2 = http("/user/code?phone=" + p1, "POST")
    ok1 = st1 == 200 and isinstance(r1, dict) and r1.get("success")
    ok2 = st2 == 200 and isinstance(r2, dict) and not r2.get("success")
    check("发码第 1 次成功 / 第 2 次被限流", ok1 and ok2,
          "第2次=%s" % (r2.get("errorMsg") if isinstance(r2, dict) else r2))

    # 错 5 次烧码
    p2 = "13456780002"
    redis("set", "login:code:" + p2, "111111", "EX", "300")
    redis("del", "login:code:fail:" + p2)
    last_msg = ""
    for _ in range(5):
        _, r = http("/user/login", "POST", body={"phone": p2, "code": "000000"})
        last_msg = r.get("errorMsg") if isinstance(r, dict) else str(r)
    st, r = http("/user/login", "POST", body={"phone": p2, "code": "111111"})
    burned = st == 200 and isinstance(r, dict) and not r.get("success")
    check("错 5 次后正确码也失效（码被烧掉）", burned, "第5次提示=%s / 正确码=%s"
          % (last_msg, r.get("errorMsg") if isinstance(r, dict) else r))

    # 并发同码注册
    p3 = "13456780003"
    redis("set", "login:code:" + p3, "222222", "EX", "300")
    results, statuses = [], []
    lock = threading.Lock()

    def one():
        s, rp = http("/user/login", "POST", body={"phone": p3, "code": "222222"})
        with lock:
            statuses.append(s)
            results.append(bool(isinstance(rp, dict) and rp.get("success")))

    ts = [threading.Thread(target=one) for _ in range(8)]
    for t in ts:
        t.start()
    for t in ts:
        t.join()
    check("8 并发同码登录只成功 1 个", sum(results) == 1, "成功=%d" % sum(results))
    check("并发注册无 5xx", all(s < 500 for s in statuses), "状态码=%s" % sorted(set(statuses)))

    # ---------------- E. 写入原子性 ----------------
    section("E. 点赞 / 关注 幂等")
    token, st = login("13456789001")
    check("登录拿到 token", token is not None, "http=%s" % st)

    if token:
        blog_id, user_id, follow_target = 7, 5, 10
        liked0 = int(sql("select liked from tb_blog where id=%d" % blog_id) or 0)
        had_key = redis("exists", "blog:liked:%d" % blog_id) == "1"
        snap = redis("zscore", "blog:liked:%d" % blog_id, str(user_id))

        likes = []
        def do_like():
            s, rp = http("/blog/like/%d" % blog_id, "PUT", token)
            with lock:
                likes.append(s)
        ts = [threading.Thread(target=do_like) for _ in range(5)]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
        liked1 = int(sql("select liked from tb_blog where id=%d" % blog_id) or 0)

        # 断言用"状态变化数"，不是固定 +1。
        # 这个接口是 toggle：5 个请求并发进来，可能有点上又被另一个请求取消，
        # 净变化就是 0。真正要保证的是 **DB 计数与 Redis 状态一一对应**，
        # 而不是"点了 5 次只能涨 1"。原版的问题是计数和状态脱钩（1 人点出 +5）。
        had_before = bool(snap and snap.strip())
        has_after = redis("zscore", "blog:liked:%d" % blog_id, str(user_id)).strip() != ""
        state_delta = (1 if has_after else 0) - (1 if had_before else 0)
        db_delta = liked1 - liked0
        check("5 并发点赞：计数与状态严格对应", db_delta == state_delta and abs(db_delta) <= 1 and liked1 >= 0,
              "liked %d -> %d（Δ=%+d），Redis 状态 %s -> %s（Δ=%+d）"
              % (liked0, liked1, db_delta, "已赞" if had_before else "未赞",
                 "已赞" if has_after else "未赞", state_delta))

        # 还原
        sql("update tb_blog set liked=%d where id=%d" % (liked0, blog_id))
        if snap and snap != "":
            redis("zadd", "blog:liked:%d" % blog_id, snap, str(user_id))
        elif not had_key:
            redis("del", "blog:liked:%d" % blog_id)
        else:
            redis("zrem", "blog:liked:%d" % blog_id, str(user_id))

        follow0 = int(sql("select count(*) from tb_follow") or 0)
        fstatus = []
        def do_follow():
            s, _ = http("/follow/%d/true" % follow_target, "PUT", token)
            with lock:
                fstatus.append(s)
        ts = [threading.Thread(target=do_follow) for _ in range(8)]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
        rows = int(sql("select count(*) from tb_follow where user_id=%d and follow_user_id=%d"
                       % (user_id, follow_target)) or 0)
        check("8 并发重复关注只落 1 行", rows == 1, "行数=%d，状态码=%s" % (rows, sorted(set(fstatus))))
        check("重复关注无 5xx", all(s < 500 for s in fstatus), "状态码=%s" % sorted(set(fstatus)))

        sql("delete from tb_follow where user_id=%d and follow_user_id=%d" % (user_id, follow_target))
        # DB 删了还要把 Redis 的关注集合回滚，否则下次跑"共同关注"会把脏数据当缓存用
        redis("srem", "follows:%d" % user_id, str(follow_target))
        if redis("scard", "follows:%d" % user_id) in ("0", ""):
            redis("del", "follows:%d" % user_id)
        follow1 = int(sql("select count(*) from tb_follow") or 0)
        check("关注数据已还原", follow1 == follow0, "%d -> %d" % (follow0, follow1))

    # ---------------- F. 读路径 N+1 ----------------
    section("F. /blog/hot 的 SQL 条数")
    try:
        sql("SET GLOBAL log_output='TABLE'")
        sql("SET GLOBAL general_log=ON")
        sql("TRUNCATE TABLE mysql.general_log")
        http("/blog/hot", token=token)
        time.sleep(0.4)
        rows = sql("SELECT argument FROM mysql.general_log WHERE command_type='Query'")
        # general_log.argument 是 BLOB，SQL 里的 LIKE 是字节比较（区分大小写），
        # 所以过滤放在 Python 里做，不能写成 LIKE 'select%'
        app_sql = []
        for line in rows.split("\n"):
            s = line.strip()
            u = s.upper()
            if not u.startswith("SELECT"):
                continue
            if "MYSQL.GENERAL_LOG" in u or "@@VERSION" in u:
                continue
            if " TB_" in u or "`TB_" in u:
                app_sql.append(s)
        check("列表固定 3 条 SQL（不随条数放大）", 1 <= len(app_sql) <= 4,
              "实测 %d 条: %s" % (len(app_sql), " / ".join(x[:38] for x in app_sql)))
    except Exception as e:
        check("列表 SQL 条数可测", False, str(e)[:60])
    finally:
        try:
            sql("SET GLOBAL general_log=OFF")
            sql("TRUNCATE TABLE mysql.general_log")
            sql("SET GLOBAL log_output='FILE'")
        except Exception:
            pass

    # ---------------- G. 数据一致性 ----------------
    section("G. 库存一致性")
    db_stock = sql("select stock from tb_seckill_voucher where voucher_id=7")
    rd_stock = redis("get", "seckill:stock:7")
    check("DB 与 Redis 库存一致", db_stock == (rd_stock or ""), "db=%s redis=%s" % (db_stock, rd_stock))

    # ---------------- 清理 ----------------
    for p in ["13456780001", "13456780002", "13456780003"]:
        redis("del", "login:code:" + p, "login:code:limit:" + p,
              "login:code:daily:" + p, "login:code:fail:" + p)
    sql("delete from tb_user where phone in ('13456780001','13456780002','13456780003')")

    # ---------------- 汇总 ----------------
    bad_5xx = [c for c in CODES if c >= 500]
    print("\n" + "-" * 56)
    print("通过 %d 项 / 失败 %d 项 / 5xx %d 次" % (len(OK), len(FAIL), len(bad_5xx)))
    if FAIL:
        print("失败项: " + ", ".join(FAIL))
    print("结论: " + ("全部通过" if not FAIL and not bad_5xx else "有未通过项"))
    return len(FAIL) + len(bad_5xx)


if __name__ == "__main__":
    sys.exit(main())
