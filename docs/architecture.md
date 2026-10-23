# Architecture

## Agent Gateway

```text
Web / Agent UI
  -> Java Agent Gateway
  -> LLM / Agent Platform
  -> Java Tools
  -> MySQL / Redis
```

职责：

- 前端：流式展示、文件、人工确认
- Gateway：会话、SSE、用户上下文、工具鉴权
- Agent Planner：规则规划或 OpenAI-Compatible Tool Calling
- Java Tools：权限、校验、草稿、幂等、审计
- MySQL：会话、草稿、幂等记录
- Redis：短期上下文和校验票据

Planner 只接收用户文本、物料编码和数量等最小业务信息，不接触登录 Token。
模型返回的工具名必须存在于 Java ToolRegistry；`confirm_purchase_draft` 不会暴露给模型，
只能通过独立的 `confirmDraftId` 请求字段触发。LLM 超时或返回非法工具时降级到规则规划器。

### Multi-Agent Team

```text
用户
  -> Supervisor Agent
  -> Inventory Specialist（query_stock）
  -> Purchase Specialist（prepare_purchase_draft / explicit confirmation）
  -> Java ToolRegistry
```

主管负责可解释的意图路由，并记录 `supervisor -> specialist` handoff。
每个专家拥有独立的工具白名单，Planner 只能看到白名单内工具。专家越权调用时返回
`scope-blocked`，业务写操作仍必须经过 `confirmDraftId` 人工确认。

## Business Integrations

### SSO

```text
第三方后端
  -> authorize（签名 + timestamp + nonce）
  -> 一次性 code
  -> 浏览器打开 loginUrl
  -> exchange
  -> token + target
```

### Barcode

```text
条码 -> 事件流（打印/入库/领料/出库）
单据 -> 上下游关系图（递归 + visited）
```

### SF Express

```text
发货单
  -> 报文映射 + 明细组装
  -> MD5 + Base64 签名
  -> 网关层校验
  -> 业务层校验
  -> 运单号回写
  -> 轨迹 / 推送日志
```

## Auth and Permissions

```text
登录 / SSO exchange
  -> AuthSession token
  -> 角色
  -> 菜单权限
  -> 后端接口拦截
  -> 操作日志
```

演示角色：

- ADMIN：全部菜单和操作日志
- OPERATOR：SSO、条码、单据链、顺丰
- VIEWER：SSO、条码、单据链

接口权限：

- `/api/sf/**` -> `SF`
- `/api/v1/billtrace` -> `BILLTRACE`
- `/api/admin/**` -> `AUDIT`

## RAG

```text
文档
  -> 固定窗口切分（带重叠）
  -> 关键词 + 中文双字 n-gram + 哈希 embedding
  -> 按角色过滤可见文档
  -> TopK 检索
  -> 引用片段 + 模板答案
```

默认使用本地哈希 embedding，不依赖外部向量库；可以切换为 OpenAI-Compatible
Embedding，后续再接入 pgvector / Milvus + Rerank，保留相同的权限过滤和引用协议。

已提供 pgvector 模式：

```text
RagChunk -> PostgreSQL rag_chunk.embedding(vector)
查询 -> HNSW cosine -> 权限过滤 -> 引用片段
```

回答生成支持两种模式：

- 默认模板模式：无外部 Key，可解释
- LLM 模式：配置 OpenAI-Compatible `/chat/completions` 后启用；失败时回退模板
