# Business Integrations Demo

企业系统集成演示，包含：

- SSO 单点登录
- 条码全生命周期追溯
- 单据链上下游追溯
- 顺丰物流下单、取消、轨迹和推送日志
- Vue 2 演示页面
- MySQL/Redis 可选模式

## 启动

```powershell
cd business-integrations-demo
mvn spring-boot:run
```

打开：

```text
http://localhost:18081/
```

默认使用内存存储，不需要 MySQL 和 Redis。

## 演示账号与权限

| 账号 | 密码 | 角色 | 可见菜单 |
|---|---|---|---|
| `admin` | `admin123` | 管理员 | SSO、条码、单据链、顺丰、操作日志 |
| `operator` | `operator123` | 业务员 | SSO、条码、单据链、顺丰 |
| `viewer` | `viewer123` | 查看者 | SSO、条码、单据链 |

登录后：

- 页签按角色动态显示
- 顺丰接口需要 `SF` 权限
- 单据链接口需要 `BILLTRACE` 权限
- 操作日志接口需要 `AUDIT` 权限

登录页也可以点击“SSO 演示登录”，走一次性 code 换取 token 的完整链路。

## MySQL + Redis

```powershell
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

`mysql` profile 使用：

- MySQL 保存条码、单据节点、用户映射和顺丰日志
- Redis 保存 SSO nonce 和一次性 code
- `db/schema.sql` 和 `db/data.sql` 初始化演示数据

## SSO

```text
POST /api/sso/authorize
GET  /api/sso/exchange?code=...
```

设计：

- 参数按名称升序拼接
- HmacSHA256 + Base64
- timestamp 5 分钟时间窗
- nonce 防重放
- code 60 秒有效且只能兑换一次
- Token 不进入 URL

浏览器演示页会通过 `/api/demo/sso/authorize` 在服务端生成签名，避免把 appSecret 暴露给浏览器。

## 条码追溯

```text
GET /api/v1/barcodeFlowRecord/trace/BC001
```

返回打印、来料入库、生产领料、成品入库和销售出库事件。

回填接口：

```text
POST /api/v1/barcodeFlowRecord/backfill
token: demo-token
```

## 单据链追溯

```text
GET /api/v1/billtrace?billType=SALES_ORDER&id=SO-1001
```

演示链路：

```text
销售订单 -> 生产订单 -> 质检单 -> 采购入库单 -> 条码
```

递归时使用 visited 集合防止循环。

## 顺丰

```text
POST /api/sf/orders
POST /api/sf/cancel/{dispatchBillId}
GET  /api/sf/route/{waybillNo}
GET  /api/sf/logs
```

设计：

- 发货单和明细组装嵌套报文
- `msgData + timestamp + checkWord` 做 MD5 + Base64
- 网关层 + 业务层双层成功判断
- 重复发货单返回已有运单号
- 轨迹查询和推送日志

## 边界

- 不调用真实顺丰接口
- 不含客户数据和真实密钥
- SSO token 是演示值
- 生产环境需要 JWT、Redis 原子操作、IP 白名单、限流和审计
