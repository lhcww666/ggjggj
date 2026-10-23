# Verification

## Agent Gateway

已验证：

- 库存查询
- 采购草稿
- 确认创建
- 重复确认幂等
- 直接工具调用
- 规则规划器支持从自然语言提取物料编码和采购数量
- 可选 OpenAI-Compatible Tool Calling 响应解析
- LLM 服务错误时自动降级到规则规划器
- 自动化测试覆盖模型工具选择和故障降级

本次 Agent Gateway 验证结果：

- `mvn test`：4 个测试全部通过
- 启动端口 `18080` 后，工具列表返回 3 个工具
- 自然语言“查询 A001 的库存”返回库存 `12`，使用 `rule` 规划器
- 自然语言“采购 A001 5 个”成功生成草稿，使用 `rule` 规划器
- 两次使用同一 `confirmDraftId` 确认，返回相同 `orderId`
- 显式确认走 `explicit-confirmation`，不经过模型规划器

多 Agent 路由验证：

- `mvn test`：10 个测试全部通过
- `/api/agent/team/agents` 返回库存专家和采购专家 2 个 Agent
- “A002 还有多少库存”路由到 `inventory-specialist`，返回库存 `3`
- “采购 A002 2 件”路由到 `purchase-specialist` 并生成草稿
- 采购草稿确认仍由 `purchase-specialist` 执行，规划器为 `explicit-confirmation`
- 采购专家尝试查询库存时返回 `scope-blocked`，没有执行工具
- 无法识别的请求留在 `supervisor`，不产生错误 handoff

## Business Integrations

API 验证：

- SSO authorize 成功
- SSO exchange 成功
- 重复 nonce 返回 1003
- 条码 BC001 返回 5 条事件
- 单据链返回销售订单 -> 生产订单
- 顺丰下单成功
- 重复下单 `idempotent=true`
- 顺丰轨迹返回 3 条节点

浏览器验证：

- Vue 页面挂载
- 本地登录
- 管理员菜单
- 条码查询
- 单据链查询
- 顺丰下单和轨迹
- 操作日志查询
- 查看者菜单裁剪
- SSO 登录并进入业务员菜单
- RAG 公开文档检索
- RAG 管理员文档管理
- RAG 权限过滤（查看者看不到 OPERATOR/ADMIN 文档）
- RAG 评测 1.0 命中率
- RAG 评测集扩展到 10 条，命中 10/10
- PDF 上传并提取 1202 字符后删除
- `/api/rag/info` 返回混合检索和 template 模式
- 20/20 断言通过

> LLM 模式尚未接入真实 Key；已验证未配置 Key 时会降级到模板回答。

> 真实 Embedding 模式已完成代码接入，但当前默认使用本地哈希向量；未在本地配置外部 Embedding Key。

> MySQL/Redis 模式已提供 Docker Compose 和 SQL 初始化脚本；当前验证环境 Docker daemon 未启动，因此该模式只完成编译和配置校验。

> pgvector 模式已提供 PostgreSQL + pgvector Compose、建表和 HNSW 检索代码；当前 Docker daemon 未启动，未做真实 pgvector 查询验证。默认 memory 模式已通过 20/20 浏览器验证和 10/10 RAG 评测。
