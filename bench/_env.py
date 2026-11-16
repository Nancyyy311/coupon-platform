# -*- coding: utf-8 -*-
"""验收脚本共用的连接配置。全部从环境变量读，仓库里不放任何口令。

    MYSQL_USER      默认 root
    MYSQL_PASSWORD  默认空。为空时不带 -p 参数，避免 mysql 弹出交互式密码提示
    MYSQL_HOST      默认 127.0.0.1
    MYSQL_DB        默认 hmdp
    REDIS_CLI       redis-cli 可执行文件，默认取 PATH 里的 redis-cli
    REDIS_PASSWORD  默认空
    APP_PORT        默认 8081，对应被测实例端口
    APP_HOST        默认 127.0.0.1

用法：
    import _env
    MYSQL = _env.MYSQL
    REDIS = _env.REDIS
"""

import os
import subprocess


def _env_or(name, default=""):
    return (os.environ.get(name) or default).strip()


def base_url(port=None):
    port = str(port if port else _env_or("APP_PORT", "8081"))
    return "http://%s:%s" % (_env_or("APP_HOST", "127.0.0.1"), port)


def _mysql_args():
    args = [
        "mysql",
        "-u" + _env_or("MYSQL_USER", "root"),
        "-h" + _env_or("MYSQL_HOST", "127.0.0.1"),
    ]
    password = _env_or("MYSQL_PASSWORD")
    if password:
        args.append("-p" + password)
    args += ["-D", _env_or("MYSQL_DB", "hmdp"), "-N"]
    return args


def _redis_args():
    args = [_env_or("REDIS_CLI", "redis-cli")]
    password = _env_or("REDIS_PASSWORD")
    if password:
        args += ["-a", password, "--no-auth-warning"]
    return args


# 脚本里统一按 "MYSQL + [sql]" / "REDIS + args" 的写法拼接
MYSQL = _mysql_args() + ["-e"]
REDIS = _redis_args()
REDIS_CLI = REDIS[0]


def sql(query):
    """执行一条 SQL，返回 stdout 去掉首尾空白。"""
    r = subprocess.run(MYSQL + [query], capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    return r.stdout.strip()


def redis(*args):
    """执行一条 redis-cli 命令，返回 stdout 去掉首尾空白。"""
    r = subprocess.run(REDIS + [str(a) for a in args], capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    return r.stdout.strip()
