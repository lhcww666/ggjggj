# Agent Gateway Demo

最小企业 Agent 工具网关，演示：

- 短期 AgentContext
- 工具注册表
- 工具 Key 校验
- 库存查询
- 采购草稿
- 用户确认
- 幂等创建
- 规则规划器与可选 LLM Tool Calling
- LLM 故障自动降级
- 主管 Agent、库存专家和采购专家
- 专家级工具白名单
- SSE 流式接口

## 启动

```powershell
cd api/agent-gateway
mvn spring-boot:run
```

默认端口：`8080`

前端位于项目根目录的 `ui/agent-console`，由 `ui/server.mjs` 通过 `/agent-api/**`
代理到 8080。访问 `http://localhost:9000/agent/` 使用控制台。

如果端口被占用：

```powershell
java -jar target\agent-learning-kit-0.0.1-SNAPSHOT.jar --server.port=18080
```

## 工具列表

```http
GET /api/agent/tools
```

## 可选 LLM Tool Calling

默认使用无需模型的规则规划器。配置 OpenAI-Compatible 接口后，模型负责选择工具和参数，
Java 继续负责用户上下文、权限、校验、确认和幂等：

```powershell
$env:AGENT_LLM_API_KEY="你的 Key"
mvn spring-boot:run "-Dspring-boot.run.arguments=--agent.llm.enabled=true"
```

也可以修改 `application.yml`：

```yaml
agent:
  llm:
    enabled: true
    base-url: https://api.openai.com/v1
    api-key: ${AGENT_LLM_API_KEY}
    model: gpt-4o-mini
```

响应中的 `planner` 表示实际使用的规划器：

- `rule`：本地规则规划
- `llm`：模型 Tool Calling
- `rule-fallback`：模型失败，已自动降级
- `explicit-confirmation`：用户通过 `confirmDraftId` 明确确认

`fallback=true` 表示发生过降级。模型不会收到登录 Token，也不能调用
`confirm_purchase_draft`；正式写操作只能由请求中的 `confirmDraftId` 触发。

## 多 Agent 路由

查看专家 Agent：

```http
GET /api/agent/team/agents
```

主管 Agent 接口：

```http
POST /api/agent/team/chat
Content-Type: application/json
```

```json
{
  "userId": "demo-user",
  "query": "采购 A001 5 个"
}
```

返回包含：

- `selectedAgent`：实际接手的专家
- `supervisorReason`：主管路由原因
- `handoffs`：主管到专家的交接记录
- `result`：专家执行后的原始 `ChatResponse`

当前专家：

- `inventory-specialist`：只允许 `query_stock`
- `purchase-specialist`：允许 `prepare_purchase_draft` 和显式确认工具

主管优先识别采购意图，再识别库存意图。请求也可以通过 `agentCode` 明确选择专家；
如果专家尝试调用其他 Agent 的工具，系统返回 `scope-blocked`，不会执行工具。

路由评测：

```http
GET /api/agent/team/evaluate
```

固定用例覆盖库存路由、采购路由、主管兜底和双向工具越权拦截。

## MySQL 持久化

默认 profile 使用内存实现，自动化测试无需数据库。MySQL profile 使用
`agent_gateway` 数据库保存库存、短期上下文、采购草稿和正式订单：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-mysql.ps1
```

其他环境可以先设置：

```powershell
$env:AGENT_DB_USERNAME="agent_gateway"
$env:AGENT_DB_PASSWORD="<本地密码>"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

`schema-mysql.sql` 会在启动时创建：

- `agent_stock`
- `agent_context`
- `agent_purchase_draft`
- `agent_purchase_order`

确认采购草稿使用数据库主键和订单唯一约束保证幂等。页面顶部的 `DATA LAYER` 会显示
当前使用 `MYSQL` 还是 `MEMORY`。

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
- 默认 profile 使用内存；MySQL profile 已持久化库存、上下文、草稿和订单
- 生产环境仍建议把短期上下文迁移到 Redis TTL，并按需要增加组织和租户字段
- 该项目用于学习 Agent 工具网关，不包含真实业务代码
