# -*- coding: utf-8 -*-
"""全接口冒烟：匿名与登录两种身份把所有 GET 打一遍，任何 5xx 都要报出来。

用法：python smoke_all.py 8082
只读接口，会签到一次（/user/sign 会写 Redis bitmap）并在结束时说明；
其余接口均为查询。写接口不在这里测（已有专项脚本）。
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request

PORT = sys.argv[1] if len(sys.argv) > 1 else "8082"
BASE = "http://127.0.0.1:%s" % PORT
import _env

REDIS = _env.REDIS
OP = urllib.request.build_opener(urllib.request.ProxyHandler({}))

GETS = [
    "/shop-type/list",
    "/shop/1",
    "/shop/999999",
    "/shop/of/type?typeId=1&current=1&x=120.149192&y=30.316078",
    "/shop/of/type?typeId=1&current=1",
    "/shop/of/name?name=%E8%8C%B6&current=1",
    "/voucher/list/1",
    "/blog/hot?current=1",
    "/blog/7",
    "/blog/likes/7",
    "/blog/of/user?id=1&current=1",
    "/user/info/1",
    "/user/1",
    "/actuator/health",
]

AUTH_GETS = [
    "/user/me",
    "/blog/of/me",
    "/blog/of/follow?lastId=9999999999999&offset=0",
    "/follow/or/not/1",
    "/follow/common/1",
    "/voucher-order/my",
    "/voucher-order/result/377477855015075840",
    "/user/sign/count",
]


def http(path, token=None):
    req = urllib.request.Request(BASE + path)
    if token:
        req.add_header("authorization", token)
    try:
        with OP.open(req, timeout=15) as r:
            return r.status, r.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def login():
    subprocess.run(REDIS + ["set", "login:code:13456789001", "246810", "EX", "300"], capture_output=True)
    req = urllib.request.Request(BASE + "/user/login", data=json.dumps(
        {"phone": "13456789001", "code": "246810"}).encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    with OP.open(req, timeout=15) as r:
        return json.loads(r.read().decode())["data"]


bad = []
token = login()

for path in GETS:
    st, body = http(path)
    flag = "" if st < 500 else "  <== 5xx"
    print("匿名  %-3s %s%s" % (st, path, flag))
    if st >= 500:
        bad.append(("匿名", path, st, body[:200]))

for path in AUTH_GETS:
    st, body = http(path, token)
    flag = "" if st < 500 else "  <== 5xx"
    print("登录  %-3s %s%s" % (st, path, flag))
    if st >= 500:
        bad.append(("登录", path, st, body[:200]))

subprocess.run(REDIS + ["del", "login:code:13456789001"], capture_output=True)
print("\n==== 5xx 数量：%d ====" % len(bad))
for role, path, st, body in bad:
    print("[%s] %s -> %s %s" % (role, path, st, body.replace("\n", " ")))
sys.exit(1 if bad else 0)
