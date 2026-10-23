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

- `mvn test`：11 个测试全部通过
- `/api/agent/team/agents` 返回库存专家和采购专家 2 个 Agent
- “A002 还有多少库存”路由到 `inventory-specialist`，返回库存 `3`
- “采购 A002 2 件”路由到 `purchase-specialist` 并生成草稿
- 采购草稿确认仍由 `purchase-specialist` 执行，规划器为 `explicit-confirmation`
- 采购专家尝试查询库存时返回 `scope-blocked`，没有执行工具
- 无法识别的请求留在 `supervisor`，不产生错误 handoff
- 固定路由评测 8/8 通过

浏览器验证：

- Agent Team Console 正常加载 2 个专家和工具白名单
- 页面发起库存任务后显示 `inventory-specialist`、handoff 和库存 `3`
- 页面发起采购任务后显示 `purchase-specialist` 和草稿 ID
- 选择采购专家查询库存时显示 `scope-blocked`，工具调用为空
- 页面运行评测后显示 8/8、100% 和 9 行表格（含表头）
- 浏览器断言 12/12 通过，无 JavaScript 异常，接口请求均为 HTTP 200

MySQL 持久化验证：

- MySQL profile 启动成功，Hikari 连接 `agent_gateway`
- `/api/agent/team/runtime` 返回 `storage=mysql`、`contextStore=mysql`
- 自动初始化 3 条库存数据
- 库存查询、采购草稿、显式确认和重复确认均成功
- 数据库实际写入 `agent_context`、`agent_purchase_draft`、`agent_purchase_order`
- 重启服务后，用数据库中的历史 `draftId` 再次确认，仍返回相同 `orderId`
- `bishe_schema` 原有 56 张表未修改；Agent 数据只写入独立 `agent_gateway`

新版 MySQL 控制台浏览器验证：

- 页面显示 `MYSQL` 数据层和 `MYSQL online`
- 库存专家工具轨迹显示 `query_stock`，返回库存 `3`
- 采购专家显示 `prepare_purchase_draft` 和草稿 ID
- 越权操作显示 `scope-blocked` 且工具轨迹为 0
- 路由评测显示 8/8、100% 和 8 条用例
- 浏览器断言 12/12 通过，无 JavaScript 异常

前后端分离与单 API 项目结构验证：

- `mvn -f api/pom.xml test`：单一 API 编译通过，11 个测试通过
- `ui/server.mjs` 独立运行在 9000，不依赖 Spring Boot 托管静态页面
- `/api/**` 统一代理到 18080 单一 API
- Agent 库存查询返回 `12`
- 管理员登录成功，条码 BC001 返回 5 条事件
- 未登录访问 `/api/auth/me` 返回预期的 401
- 统一首页、Agent 控制台、企业集成控制台均真实打开
- 浏览器断言 4/4 通过，无 JavaScript 异常

一键启动与健康检查：

- `scripts/start-dev.ps1` 同时启动 UI 和单一 API
- `scripts/status-dev.ps1` 显示两个服务均为 `UP`
- UI 健康检查返回 `enterprise-agent-ui`
- 单一 API 健康检查返回 Agent MySQL 和业务 JDBC 存储模式
- `scripts/stop-dev.ps1` 只关闭本次记录的 PID
- 停止后 9000、18080 全部释放

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
