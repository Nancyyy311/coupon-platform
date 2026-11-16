# 黑马点评 · 本地生活服务平台（可靠性改造版）

一个基于 Spring Boot 的本地生活类后端系统：商户检索（含附近商户）、优惠券与限时秒杀、
探店笔记与关注流、签到积分，分用户端与商户端。

代码源自**黑马点评课程配套案例**。我没有继续加业务功能，而是把它当成一套**遗留系统**来治理：
按「并发正确性 → 安全边界 → 边界输入 → 可观测」四条线做改造，一共 **15 个主题、25 处真实缺陷**，
每一处都留有改造前后的实测对照与可复跑的验证脚本。

> 一句话：**把"能跑通"变成"能自证正确"。**

---

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 / 框架 | Java 8 · Spring Boot 2.3.12 · Spring MVC |
| 持久层 | MyBatis-Plus 3.4.3 · MySQL 8（`STRICT_TRANS_TABLES`） |
| 缓存 / 并发 | Redis 5 · 5 段 Lua 脚本 · GEO · ZSet · BitMap · Pipeline |
| 消息 | RabbitMQ 3.13（异步落库、手动 ACK、死信处置、幂等消费） |
| 安全 | Spring Security 5.3 · JWT（jjwt 0.11.5）+ jti 黑名单 · 角色授权 |
| 可观测 | Actuator · Micrometer（9 类秒杀指标、liveness/readiness 探针） |
| 测试 | JUnit 5 + Mockito（173 个单测，零外部依赖） |

---

## 核心改造

| 主题 | 做法 | 结果 |
|---|---|---|
| 秒杀主链路 | `seckill.lua` 一次原子完成「时间窗校验 → 判库存 → 一人一单 → 扣减」，返回码区分 6 种结果；MQ 异步落库 + 死信 + 幂等回补 | 重复下单场景 QPS **+13 倍**，P99 **50ms → 4ms** |
| 两级限流 | 本地固定窗口粗筛（零 IO）+ Redis 令牌桶精筛，fail-open | 拒绝吞吐 2,806 → **41,041 QPS**；Redis 请求 40 万 → **76 次**（−99.8%） |
| 缓存一致性 | 事务提交后删 + 广播清各实例 L1 + 延迟双删 | 双实例 L1 失效延迟 **306ms** |
| 权限边界 | 白名单从「按路径」改为「按 HTTP 方法」；新增 `role` + `hasRole` | 堵掉 4 个匿名可达的写接口；匿名 401 / 普通用户 403 / 商家放行 |
| 入参守卫 | 坐标含 `NaN` / 越界、页码整数溢出、unsigned 负值、超长文本全部入口拦截 | 12 类畸形入参从 500 变为字段级文案，**0 个 5xx** |
| 读路径 | N+1 压平 + 补 3 个索引 | 列表 SQL **7 → 3 条**且不随长度增长；10 万行影子表热度榜 **22.7ms → 0.043ms** |
| GEO 写路径 | 补上原项目缺失的索引同步（跨分类迁移、只改名不写） | DB 与 GEO 成员数保持一致（14:14） |
| 多实例 | 雪花 workerId 注入 + 任务锁 + 幂等 | 同毫秒争锁、跨实例不超卖、L1 广播失效三项实测 |

完整改造清单（15 个主题 / 25 处缺陷的逐条现象、根因、改法与验证）另行维护在本地文档中，未随仓库提交。

---

## 快速开始

### 1. 环境要求

- JDK 8、Maven 3.6+
- MySQL 8（本机 3306）
- Redis 5（本机 6379）
- RabbitMQ 3.13（本机 5672）

```bash
# RabbitMQ 可用 Docker 一键起
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```

### 2. 建库

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS hmdp DEFAULT CHARSET utf8mb4;"
mysql -uroot -p hmdp < src/main/resources/db/hmdp.sql        # 基线表 + 初始数据
mysql -uroot -p hmdp < src/main/resources/db/V1__upgrade.sql # 索引 / 列 / 表升级（可重复执行）
mysql -uroot -p hmdp < src/main/resources/db/V2__role.sql    # 角色列（可重复执行）
```

### 3. 配置

**仓库里不含任何真实口令。** `application.yaml` 中的敏感项一律写成 `${环境变量:默认值}`，
本机真实值放在 `application-local.yaml`（已被 `.gitignore` 忽略，不会提交）：

```bash
cp src/main/resources/application-local.example.yaml \
   src/main/resources/application-local.yaml
# 然后编辑 application-local.yaml，填入本机的 MySQL / Redis 密码和上传目录
```

也可以完全不用这个文件，直接注入环境变量（优先级更高，适合容器与 CI）：

```bash
export MYSQL_HOST=127.0.0.1  MYSQL_PORT=3306  MYSQL_DATABASE=hmdp
export MYSQL_USERNAME=root  MYSQL_PASSWORD=your_password
export REDIS_HOST=127.0.0.1  REDIS_PORT=6379  REDIS_PASSWORD=your_password
export RABBITMQ_USERNAME=guest  RABBITMQ_PASSWORD=guest
export JWT_SECRET="$(openssl rand -base64 48)"   # 必须 >= 32 字节
export HMDP_UPLOAD_DIR=./imgs/                   # 图片上传根目录
export WORKER_ID=0                               # 多实例时每个副本必须不同（0~31）
```

> `JWT_SECRET` 的默认值是一个**公开的占位串**，仅用于"clone 下来就能跑通"。
> 它意味着谁拿到这份代码谁就能伪造 token —— 任何对外部署前必须换掉。

### 4. 启动与测试

```bash
mvn spring-boot:run     # 默认 8081
mvn test                # 173 个单测，不需要任何中间件
```

---

## 值得一看的文件

| 文件 | 为什么值得看 |
|---|---|
| `resources/seckill.lua` | 约 30 行讲清楚"为什么这里不需要分布式锁" |
| `cache/CacheClient.java` | 二级缓存 + 锁身份 UUID + Lua CAS 释放 |
| `service/impl/VoucherOrderServiceImpl.java` | 五态查询 + 归属校验 + 雪花时间反解 |
| `mq/OrderConsumer.java` | 手动 ACK、确定性失败与临时失败分开处理 |
| `cache/ShopGeoIndexer.java` | GEO 写路径：跨分类要先 `ZREM` 旧 key |
| `utils/ParamValidator.java` | 按数据库列口径校验，而不是"拍脑袋定长度" |

---

## 已知边界（主动说明，不是遗漏）

- **不是集群**：只起了两个实例验证 workerId 隔离、广播清 L1、跨实例不超卖三项行为；负载均衡与故障转移未做。
- **订单是领券记录**：没有支付链路，`pay_time / use_time / refund_time` 三列恒为 NULL。
- **角色存在 JWT 中**：改角色需重新登录才生效，降权不及时。
- **评论功能是空壳**：`BlogCommentsController` 无任何端点，不在改造范围。
- **上传目录**：路径来自配置，默认指向本地 Nginx 静态目录。
- **短信通道**：验证码走日志输出模拟，未接真实服务商。

---

## 数字口径

所有性能数字均为**同机 A/B 对照**（4 进程 × 16 并发 × 10 秒，预热丢弃，ABBA 三轮取中位数）。
其中 41,041 QPS 是**限流拒绝**的吞吐，不是下单成功 TPS；成功路径未压测（库存仅 100）。
绝对值强依赖机器，因此对外只引用倍数与百分比。

---

## 安全边界（README 也是承诺，写清楚比藏着好）

- **仓库内零凭据**：数据库 / Redis / MQ 口令与 JWT 密钥全部走环境变量，真实值只在本地 `application-local.yaml` 里，该文件不提交。
- **演示数据已脱敏**：`hmdp.sql` 里的种子用户手机号原本是真实号段格式，已统一替换为 `138000000xx` 测试号段（保持唯一，不影响手机号唯一索引与登录流程）。
- **JWT 默认密钥是公开占位串**：只保证"clone 下来能跑"，**任何公网部署前必须替换**，否则等于把签发权交出去。
- **验证码走日志输出**：`log.debug` 打印，仅用于本地联调；生产必须接真实短信通道，并保留下发的频控。
- **上传目录默认 `./imgs/`**：不再写死某台机器的绝对路径；生产应指向 Nginx 静态目录，并注意该目录**不可执行**（只放图片，禁止脚本）。
- **不要提交 `application-local.yaml`**：若已误提交，`.gitignore` 对已跟踪文件无效，需 `git rm --cached` 后再清理历史。

---

## 声明

原始业务代码与前端页面来自黑马程序员《黑马点评》课程案例，仅用于学习。
本项目在其基础上完成的是工程化改造（并发正确性、安全边界、参数校验、可观测与测试），
改造范围与原始代码的差异见提交记录。
