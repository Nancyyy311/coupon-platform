# 到店消费服务与优惠券营销平台

一个本地生活服务的后端，Spring Boot 写的。能查店铺、按距离找附近商户、抢限时秒杀券、发探店笔记、关注别人、签到领积分，接口分用户端和商户端两套。

业务代码来自黑马点评课程案例。我没有继续加功能，而是把它当成一套已经在跑的老系统来改：从并发、权限、入参、监控四个方向找出真实存在的问题，一个个修掉，一共 15 个主题、25 处缺陷。每处都有修改前后的对比数据，仓库里的 173 个单测可以直接跑，不依赖任何中间件。

---

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 / 框架 | Java 8，Spring Boot 2.3.12，Spring MVC |
| 持久层 | MyBatis-Plus 3.4.3，MySQL 8 |
| 缓存 / 并发 | Redis 5，Lua 脚本，GEO，ZSet，BitMap |
| 消息 | RabbitMQ 4.4，异步落库，手动 ACK，死信 |
| 安全 | Spring Security 5.3，JWT（jjwt 0.11.5），角色授权 |
| 可观测 | Actuator，Micrometer |
| 测试 | JUnit 5，Mockito，173 个单测，不依赖外部中间件 |

---

## 改了什么

| 主题 | 做法 | 结果 |
|---|---|---|
| 秒杀主链路 | `seckill.lua` 一次原子完成时间窗校验、判库存、一人一单、扣减，返回码区分 6 种结果；MQ 异步落库，带死信和幂等回补 | 重复下单场景 QPS 提升 13 倍，P99 从 50ms 降到 4ms |
| 两级限流 | 本地固定窗口零 IO 粗筛，Redis 令牌桶精筛，fail-open | 拒绝吞吐从 2,806 提到 41,041 QPS，Redis 请求从 40 万降到 76 次 |
| 缓存一致性 | 事务提交后删缓存，广播清各实例 L1，再延迟双删 | 双实例 L1 失效延迟 306ms |
| 权限边界 | 白名单从按路径改成按 HTTP 方法，新增 role 列和 hasRole | 堵掉 4 个匿名可达的写接口 |
| 入参守卫 | 坐标 NaN 与越界、页码整数溢出、unsigned 负值、超长文本都在入口拦掉 | 12 类畸形入参从 500 变成字段级提示，0 个 5xx |
| 读路径 | 压平 N+1，补 3 个索引 | 列表 SQL 从 7 条降到 3 条，不随列表长度增长；10 万行影子表上热度榜从 22.7ms 降到 0.043ms |
| GEO 写路径 | 补上原项目缺的索引同步，包括跨分类迁移和只改名不写 | DB 与 GEO 成员数一致 |
| 多实例 | 雪花 workerId 注入，任务锁，幂等 | 同毫秒争锁、跨实例不超卖、L1 广播失效三项都实测过 |

---

## 快速开始

### 1. 环境要求

- JDK 8，Maven 3.6+
- MySQL 8，默认 3306
- Redis 5，默认 6379
- RabbitMQ 4.4，默认 5672

RabbitMQ 可以用 Docker 起：

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```

### 2. 建库

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS hmdp DEFAULT CHARSET utf8mb4;"
mysql -uroot -p hmdp < src/main/resources/db/hmdp.sql        # 基线表 + 初始数据
mysql -uroot -p hmdp < src/main/resources/db/V1__upgrade.sql # 索引 / 列 / 表升级，可重复执行
mysql -uroot -p hmdp < src/main/resources/db/V2__role.sql    # 角色列，可重复执行
```

### 3. 配置

仓库里没有真实口令。`application.yaml` 的敏感项都写成 `${环境变量:默认值}` 的形式，本机真实值放在 `application-local.yaml`，这个文件在 `.gitignore` 里。

```bash
cp src/main/resources/application-local.example.yaml \
   src/main/resources/application-local.yaml
```

然后编辑它，填上本机的 MySQL、Redis 密码和上传目录。

不想用这个文件也可以，直接注入环境变量，优先级更高，适合容器和 CI：

```bash
export MYSQL_HOST=127.0.0.1  MYSQL_PORT=3306  MYSQL_DATABASE=hmdp
export MYSQL_USERNAME=root  MYSQL_PASSWORD=your_password
export REDIS_HOST=127.0.0.1  REDIS_PORT=6379  REDIS_PASSWORD=your_password
export RABBITMQ_USERNAME=guest  RABBITMQ_PASSWORD=guest
export JWT_SECRET="$(openssl rand -base64 48)"
export HMDP_UPLOAD_DIR=./imgs/
export WORKER_ID=0
```

`JWT_SECRET` 至少要 32 字节，否则启动时会被 jjwt 直接拒掉。`WORKER_ID` 在多实例部署时每个副本必须不一样，取值 0 到 31。

### 4. 启动与测试

```bash
mvn spring-boot:run     # 默认 8081
mvn test                # 173 个单测，不需要任何中间件
```

---

## 几个关键文件

| 文件 | 看点 |
|---|---|
| `resources/seckill.lua` | 30 行讲清楚这里为什么不需要分布式锁 |
| `cache/CacheClient.java` | 二级缓存、锁身份 UUID、Lua CAS 释放 |
| `service/impl/VoucherOrderServiceImpl.java` | 五态查询、归属校验、雪花时间反解 |
| `mq/OrderConsumer.java` | 手动 ACK，确定性失败和临时失败分开处理 |
| `cache/ShopGeoIndexer.java` | GEO 写路径，跨分类要先 ZREM 旧 key |
| `utils/ParamValidator.java` | 按数据库列的实际长度校验，不拍脑袋定 |

---

## 还没做的部分

- 不是集群。只起了两个实例，验证 workerId 隔离、广播清 L1、跨实例不超卖这三件事，负载均衡和故障转移没做。
- 订单只是领券记录，没有支付链路，`pay_time`、`use_time`、`refund_time` 三列一直是 NULL。
- 角色写在 JWT 里，改角色要重新登录才生效，降权有延迟。
- 上传目录的路径从配置读，默认指向本地 Nginx 的静态目录。
- 短信没接真实服务商，验证码打在日志里模拟。

---

## 关于数字

性能数据都是同机 A/B 对照：4 进程、16 并发、跑 10 秒，预热数据丢掉，ABBA 三轮取中位数。

41,041 QPS 指的是限流拒绝的吞吐，不是下单成功的 TPS。成功路径没压测，因为库存只有 100 件。换台机器数字会差很多，所以这里只写倍数和百分比，不报绝对值。

---

## 安全相关

仓库里没有任何凭据。数据库、Redis、MQ 的口令和 JWT 密钥都走环境变量，真实值只在本地 `application-local.yaml` 里。

`hmdp.sql` 里的种子用户手机号原来是真实号段格式，我统一换成了 `138000000xx` 测试号段，保持唯一，不影响索引和登录流程。

JWT 的默认密钥是公开占位串，只保证 clone 下来能跑通。谁拿到代码谁就能伪造 token，任何对外部署前必须换掉。

验证码目前打在日志里，只用于本地联调。上生产要接真实短信通道，下发频控也得保留。

上传目录默认是 `./imgs/`，不再写死某台机器的绝对路径。指向 Nginx 静态目录时注意这个目录只放图片，别放能执行的东西。

---

## 来源

业务代码和前端页面来自黑马程序员《黑马点评》课程案例。上面列的改动都是在这个基础上做的，逐次看提交记录能对上。
