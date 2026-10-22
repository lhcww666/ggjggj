# Agent Gateway Demo

最小企业 Agent 工具网关，演示：

- 短期 AgentContext
- 工具注册表
- 工具 Key 校验
- 库存查询
- 采购草稿
- 用户确认
- 幂等创建
- SSE 流式接口

## 启动

```powershell
cd agent-gateway-demo
mvn spring-boot:run
```

默认端口：`8080`

如果端口被占用：

```powershell
java -jar target\agent-learning-kit-0.0.1-SNAPSHOT.jar --server.port=18080
```

## 工具列表

```http
GET /api/agent/tools
```

## 库存查询

```http
POST /api/agent/chat
Content-Type: application/json
```

```json
{
  "userId": "demo-user",
  "agentCode": "stock-assistant",
  "query": "查询 A001 的库存",
  "inventoryCode": "A001"
}
```

## 采购草稿

```json
{
  "userId": "demo-user",
  "agentCode": "purchase-assistant",
  "query": "采购 A001 10 个",
  "inventoryCode": "A001",
  "quantity": 10
}
```

返回 `draftId` 后：

```json
{
  "userId": "demo-user",
  "agentCode": "purchase-assistant",
  "query": "确认创建",
  "confirmDraftId": "上一步返回的 draftId"
}
```

同一个草稿重复确认会返回同一个订单号，用于演示幂等。

## 直接调用工具

```http
POST /api/tools/query_stock
Content-Type: application/json
X-Agent-Tool-Key: local-demo-tool-key
X-Agent-Context-Id: <聊天接口返回的 contextId>
```

```json
{
  "inventoryCode": "A001"
}
```

## SSE

```http
POST /api/agent/chat/stream
Content-Type: application/json
```

返回 `message`、`metadata`、`done` 事件。

## 设计边界

- 工具 Key 来自配置，生产环境应使用密钥管理
- 上下文使用内存，生产环境应使用 Redis TTL
- 数据使用内存，生产环境应使用 MySQL 和事务
- 该项目用于学习 Agent 工具网关，不包含真实业务代码
