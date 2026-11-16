# -*- coding: utf-8 -*-
"""
GEO 索引与 DB 同步验证（可 repeated 跑，结束时还原全部数据）。

验证三件事：
  1. 新增店铺后，能否在"附近商户"里查到（走 Redis GEO）
  2. 修改店铺坐标后，附近商户的返回和距离是否跟着变
  3. 修改店铺分类后，是否从旧分类的 GEO 集合里移走、进到新分类

用法：
  python verify_geo_sync.py [port]      默认 8081
修复前预期：1 不出现 / 2 距离不变 / 3 两边都在（都是 bug）
修复后预期：1 出现 / 2 距离变化 / 3 旧分类没有、新分类有
"""
import json
import subprocess

import rbac
import sys
import urllib.error
import urllib.request

OP = urllib.request.build_opener(urllib.request.ProxyHandler({}))
import _env

REDIS = _env.REDIS_CLI
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8081

# 测试中心点：店 1 的原始坐标
CX, CY = 120.149192, 30.316078
# 新店坐标：离中心点约 20 米
NX, NY = 120.14930, 30.31610
# 店 1 的"搬走"坐标：杭州湾外，离中心 100 公里以上
FX, FY = 121.500000, 31.400000
TEST_NAME = "GEO验证店"

# 写店铺需要登录态（第 2 轮把 /shop/** 的写方法移出了白名单）。
# 用已存在的测试手机号，直接种一条验证码，不会新建用户。
LOGIN_PHONE = "13456789001"
TOKEN = None
FAILS = []


def call(path, method="GET", body=None):
    url = "http://127.0.0.1:%d%s" % (PORT, path)
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"}
    if TOKEN:
        headers["authorization"] = TOKEN
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with OP.open(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        return e.code, (json.loads(raw) if raw else {})


def login():
    """种验证码 -> 登录拿 token。返回 token，失败直接退出。

    建店/改店属于商家接口，普通账号会 403。角色写在 JWT 里，
    所以必须先把库里的 role 改成 MERCHANT 再登录，顺序不能反。
    """
    rbac.promote(LOGIN_PHONE)
    subprocess.run(_env.REDIS + [
                    "set", "login:code:" + LOGIN_PHONE, "246810", "EX", "300"],
                   capture_output=True)
    st, resp = call("/user/login", "POST", {"phone": LOGIN_PHONE, "code": "246810"})
    token = resp.get("data")
    if st != 200 or not token:
        print("[FATAL] 登录失败 http=%s %s" % (st, resp.get("errorMsg")))
        sys.exit(2)
    subprocess.run(_env.REDIS + [
                    "del", "login:code:" + LOGIN_PHONE], capture_output=True)
    return token


def check(name, condition):
    """统一断言：既打印，也累计失败数（脚本最后用退出码表达结论）。"""
    print("    %s %s" % ("PASS" if condition else "FAIL", name))
    if not condition:
        FAILS.append(name)


def redis(*args):
    out = subprocess.run(_env.REDIS + list(args),
                         capture_output=True, text=True)
    return out.stdout.strip()


def mysql(sql):
    out = subprocess.run(_env.MYSQL + [sql],
                         capture_output=True, text=True)
    return out.stdout.strip()


def nearby(type_id):
    st, resp = call("/shop/of/type?typeId=%d&current=1&x=%s&y=%s" % (type_id, CX, CY))
    if st != 200 or not resp.get("data"):
        return {}
    return {int(s["id"]): s.get("distance") for s in resp["data"]}


def geo_members(type_id):
    out = redis("zrange", "shop:geo:%d" % type_id, "0", "-1")
    return set(out.split()) if out else set()


def cleanup(new_id):
    """还原：删测试店、坐标改回、GEO 按 DB 全量重建、自增复位、清缓存。

    注意：GEO 的 key 本质是 ZSET，score 是 52 位 geohash 编码，**不能用 ZADD 写坐标**。
    用 ZADD 塞一个经纬度数值进去，GEOPOS 解出来是 (-179.99, -85.05) 这种垃圾值，
    而且不报错、成员还在——比成员丢失更难发现。（第一版脚本就踩了这个坑。）
    """
    mysql("DELETE FROM tb_shop WHERE name='%s'" % TEST_NAME)
    mysql("UPDATE tb_shop SET x=%s, y=%s, type_id=1 WHERE id=1" % (CX, CY))
    rebuild_geo_from_db()
    if new_id:
        redis("del", "cache:shop:%s" % new_id)
    redis("del", "cache:shop:1")
    mysql("ALTER TABLE tb_shop AUTO_INCREMENT = 15")


def rebuild_geo_from_db():
    """以 DB 为准重建 GEO 索引（GEO 是派生数据，DB 才是唯一真相）"""
    rows = mysql("SELECT id, type_id, x, y FROM tb_shop").splitlines()
    redis("del", "shop:geo:1", "shop:geo:2")
    for line in rows:
        parts = line.split()
        if len(parts) != 4:
            continue
        sid, tid, x, y = parts
        redis("geoadd", "shop:geo:%s" % tid, x, y, sid)


def main():
    global TOKEN
    print("=== 端口 %d ===" % PORT)
    TOKEN = login()
    print("[登录] 已获取 token，长度 %d" % len(TOKEN))

    base = nearby(1)
    print("[0] 基线 type=1 附近店数=%d，店1距离=%s" % (len(base), base.get(1)))

    new_id = None
    try:
        # 1) 新增店铺
        st, resp = call("/shop", "POST", {
            "name": TEST_NAME, "typeId": 1, "images": "geo-test.jpg",
            "area": "测试区", "address": "测试地址", "x": NX, "y": NY,
            "avgPrice": 50, "sold": 0, "comments": 0, "score": 40, "openHours": "10-22",
        })
        new_id = resp.get("data")
        print("[1] 新增 id=%s http=%s" % (new_id, st))
        geos = geo_members(1)
        got = nearby(1)
        check("新增店铺写入了 GEO 集合", new_id is not None and str(new_id) in geos)
        check("新增店铺出现在附近列表", new_id is not None and int(new_id) in got)

        # 2) 改店 1 坐标到 100 公里外
        st, resp = call("/shop", "PUT", {"id": 1, "x": FX, "y": FY})
        print("[2] 改店1坐标 http=%s %s" % (st, resp.get("errorMsg") or ""))
        got = nearby(1)
        check("搬走 100 公里后查不到店1", 1 not in got)

        # 3) 搬回中心点 + 改分类 1 -> 2（两条变更一起走：迁移 key 且坐标落回可查范围）
        st, resp = call("/shop", "PUT", {"id": 1, "typeId": 2, "x": CX, "y": CY})
        print("[3] 改店1分类为 2 并搬回中心点 http=%s" % st)
        g1, g2 = geo_members(1), geo_members(2)
        got1, got2 = nearby(1), nearby(2)
        check("旧分类 geo:1 已移走店1", "1" not in g1)
        check("旧分类附近列表不含店1", 1 not in got1)
        check("新分类 geo:2 已加入店1", "1" in g2)
        check("新分类附近列表含店1", 1 in got2)
    finally:
        cleanup(new_id)
        rbac.demote(LOGIN_PHONE)
        db_ids = set(mysql("SELECT id FROM tb_shop").split())
        geo_ids = geo_members(1) | geo_members(2)
        print("[9] 已还原：店1 坐标/分类归位，测试店删除，GEO 按 DB 重建")
        consistent = db_ids == geo_ids
        print("    DB 店铺数=%d，GEO 成员数=%d，一致=%s"
              % (len(db_ids), len(geo_ids), consistent))
        check("DB 与 GEO 成员一致", consistent)
        check("测试账号角色已还原", rbac.current_role(LOGIN_PHONE) == "USER")

    if FAILS:
        print("\n==== 失败 %d 项：%s ====" % (len(FAILS), "、".join(FAILS)))
        sys.exit(1)
    print("\n==== 全部通过 ====")


if __name__ == "__main__":
    main()
