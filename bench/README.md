# 接口验收脚本

这些脚本不测性能，只回答一个问题：某个缺陷到底修好了没有。

每个脚本都同时校验 HTTP 状态码、业务 success 标志和关键文案，任何 5xx 直接算失败，退出码等于失败项数，可以直接挂到 CI 上。全部四个脚本合计 94 条断言：

| 脚本 | 断言数 | 验什么 |
|---|---|---|
| `probe_input_guard.py` | 15 | 读接口的畸形入参。坐标越界、NaN、负页码、页码整数溢出，以及缺参、类型错、请求体非 JSON |
| `verify_geo_sync.py` | 9 | 店铺增改后 Redis GEO 与数据库是否一致。新增能否查到、改坐标距离是否跟着变、改分类是否从旧集合移走 |
| `probe_write_guard.py` | 24 | 写接口的畸形入参。超长店名、负坐标、负库存、负面额，以及秒杀券缺开始时间、时间倒置 |
| `probe_rbac.py` | 23 | 角色边界。匿名 401、普通用户 403、该能做的没被误伤、降权的已知延迟 |
| `accept_all.py` | 23 | 重启后一键自查，把前面几轮的修复在当前进程上重跑一遍 |
| `smoke_all.py` | 无断言 | 所有 GET 打一遍，只看有没有 5xx |

`smoke_all.py` 不产出断言行，它的价值是把 5xx 数量压到 0。

## 前置条件

- 应用已启动，默认端口 8081
- 数据库已导入 `src/main/resources/db/hmdp.sql`，并执行过 `V1__upgrade.sql`、`V2__role.sql`
- Redis 可连
- Python 3.6 以上，只用标准库，不需要装依赖

## 配置

全部走环境变量，仓库里没有口令。不设也能跑，默认值指向本机最常见的配置。

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_USER` | `root` | |
| `MYSQL_PASSWORD` | 空 | 为空时不带 `-p` 参数，避免 mysql 弹交互式提示 |
| `MYSQL_HOST` | `127.0.0.1` | |
| `MYSQL_DB` | `hmdp` | |
| `REDIS_CLI` | `redis-cli` | 不在 PATH 里就写绝对路径 |
| `REDIS_PASSWORD` | 空 | |
| `APP_PORT` | `8081` | 也可以直接当第一个参数传，如 `python probe_rbac.py 8082` |

Windows 上 `redis-cli` 通常不在 PATH，需要显式指定：

```bash
export MYSQL_PASSWORD=你的密码
export REDIS_PASSWORD=你的密码
export REDIS_CLI="D:/Redis/redis-cli.exe"
```

## 运行

```bash
cd bench
python probe_input_guard.py 8081
python verify_geo_sync.py 8081
python probe_write_guard.py 8081
python probe_rbac.py 8081
python accept_all.py 8081
python smoke_all.py 8081
```

`probe_input_guard.py` 和 `smoke_all.py` 只发 HTTP 请求，配置都不用设。

## 会改数据，但会还原

这几个脚本不是只读的。建店、发券、点赞、关注、改角色都会真的写库，所以每个脚本都在 `finally` 里删自己造的数据，并在最后核对基线：店铺数、券自增、库存、关注数、角色、GEO 成员数。跑完看输出末尾那几行"已还原"就知道有没有收干净。

两点注意：

- 别并发跑。几个脚本共用同一个测试号 `13456789001`，同时跑会互相把对方的角色改回 USER。
- `probe_rbac.py` 会改 `tb_user.role`。脚本结束会还原成 USER，但如果中途 Ctrl+C 打断，手动执行 `UPDATE tb_user SET role='USER' WHERE phone='13456789001'`。
- 测试号在库里不存在时，`rbac.ensure()` 会自动插一行，所以全新库也能直接跑。
