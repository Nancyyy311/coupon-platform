# -*- coding: utf-8 -*-
"""入参守卫回归：每个畸形入参都要给出"明确拒绝"，而不是 5xx 或一句"服务器异常"。

判定标准（本项目约定）：
  能归因到调用方的错误 -> HTTP 200 + success=false + 能指导修改的文案
  意料之外的服务端异常 -> HTTP 5xx
所以下面每一条都同时校验 http 状态、success 标志与关键文案。

用法：python probe_input_guard.py 8082
只读，不写任何业务数据。
"""
import json
import sys
import urllib.error
import urllib.request

PORT = sys.argv[1] if len(sys.argv) > 1 else "8081"
BASE = "http://127.0.0.1:%s" % PORT
OP = urllib.request.build_opener(urllib.request.ProxyHandler({}))

GOOD_X, GOOD_Y = "120.149192", "30.316078"

# (场景, method, path, body, 期望: ok / 文案关键字)
CASES = [
    ("附近:经度越界",    "GET", "/shop/of/type?typeId=1&x=999&y=999", None, "坐标不合法"),
    ("附近:纬度越界",    "GET", "/shop/of/type?typeId=1&x=120&y=90", None, "坐标不合法"),
    ("附近:NaN",        "GET", "/shop/of/type?typeId=1&x=NaN&y=NaN", None, "坐标不合法"),
    ("附近:Infinity",   "GET", "/shop/of/type?typeId=1&x=Infinity&y=30", None, "坐标不合法"),
    ("附近:负页码",      "GET", "/shop/of/type?typeId=1&current=-3&x=%s&y=%s" % (GOOD_X, GOOD_Y), None, "OK"),
    ("附近:零页码",      "GET", "/shop/of/type?typeId=1&current=0&x=%s&y=%s" % (GOOD_X, GOOD_Y), None, "OK"),
    ("附近:正常查询",    "GET", "/shop/of/type?typeId=1&current=1&x=%s&y=%s" % (GOOD_X, GOOD_Y), None, "OK"),
    # 超大页码：current*每页条数 在 int 下会溢出成负数，Redis 的 LIMIT 收到负值直接报错。
    # 429496730*5 = 2147483650 > Integer.MAX_VALUE，正是溢出点。
    ("附近:页码溢出",     "GET", "/shop/of/type?typeId=1&current=429496730&x=%s&y=%s" % (GOOD_X, GOOD_Y), None, "OK"),
    ("附近:页码取int上限", "GET", "/shop/of/type?typeId=1&current=2147483647&x=%s&y=%s" % (GOOD_X, GOOD_Y), None, "OK"),
    ("附近:退化为数据库分页", "GET", "/shop/of/type?typeId=1&current=0", None, "OK"),
    ("缺必填参数",       "GET", "/shop/of/type?current=1", None, "缺少必要参数"),
    ("参数类型错误",     "GET", "/shop/of/type?typeId=abc&current=1", None, "格式不正确"),
    ("路径参数溢出",     "GET", "/shop/999999999999999999999", None, "格式不正确"),
    ("请求体非 JSON",    "POST", "/user/login", "{bad", "请求体格式不正确"),
    ("Content-Type 错误", "POST", "/user/login", '{"phone":"1"}', "不支持的 Content-Type", "text/plain"),
]


def call(method, path, body, ctype="application/json"):
    data = body.encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", ctype)
    try:
        with OP.open(req, timeout=20) as r:
            return r.status, r.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


bad = []
for name, method, path, body, expect, *rest in CASES:
    st, raw = call(method, path, body, rest[0] if rest else "application/json")
    try:
        payload = json.loads(raw)
    except Exception:
        payload = {}
    success = payload.get("success")
    msg = payload.get("errorMsg") or ""

    if expect == "OK":
        ok = st == 200 and success is True
        hint = "应成功"
    else:
        # 拒绝类：必须是 200 + success=false + 文案命中，且不能是"服务器异常"
        ok = (st == 200 and success is False and expect in msg and "服务器异常" not in msg)
        hint = "应拒绝且含「%s」" % expect

    print("%-4s %-5s %-18s %s" % (st, "PASS" if ok else "FAIL", name, hint))
    if not ok:
        bad.append((name, path, st, raw[:160]))

print("\n==== 通过 %d / %d ====" % (len(CASES) - len(bad), len(CASES)))
for name, path, st, raw in bad:
    print("[%s] %s -> %s %s" % (name, path[:70], st, raw.replace("\n", " ")))
sys.exit(1 if bad else 0)
