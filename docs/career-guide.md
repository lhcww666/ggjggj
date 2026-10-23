# 求职与实操手册

这份文档用于回答三个问题：

1. 当前项目到底用了什么技术，AI 模块是怎么实现的；
2. 怎样把项目跑起来、调用工具、增加新的 Agent；
3. Java 全栈实习经历如何真实地写进 AI 应用开发岗位的简历。

## 先明确项目边界

当前仓库是**干净室学习与作品集项目**，不是公司项目源码的公开副本：

- 保留并重新实现了 SSO、条码追溯、单据链追溯、顺丰集成和 Agent 等能力；
- 不包含客户数据、生产密钥、公司内部依赖或真实第三方调用；
- 公开代码可以用于学习和面试演示，但不能把公司线上成果全部说成是该仓库直接产生的。

简历中应区分：

- **公司经历**：你真实参与、交付或维护的工作；
- **作品集复现**：为了证明能力而重新实现、可以现场讲解和运行的项目。

## 一句话项目介绍

> 基于 Spring Boot、Vue 和 MySQL 构建企业 Agent 工具网关与业务集成平台，使用工具调用、多 Agent 路由、RAG 和可靠集成机制，把大模型的意图理解能力接入库存、采购、SSO、条码和物流流程。

## 整体技术栈

| 层次 | 当前项目 | 主要用途 |
|---|---|---|
| 前端 | Vue 2、原生 HTML、Node.js 静态服务与反向代理 | Agent 控制台、业务控制台、统一 `/api/**` 入口 |
| 后端 | Java 8、Spring Boot 2.7.5、Spring MVC、Spring JDBC | REST、SSE、依赖注入、事务、数据访问 |
| 数据 | MySQL 8、可选 Redis、可选 PostgreSQL + pgvector | 业务数据、Agent 上下文、SSO 状态、向量检索 |
| AI 接入 | OpenAI-Compatible Tool Calling 与 Embedding HTTP 协议 | 选择工具、生成参数、向量化 |
| 检索 | 关键词 + 中文双字 n-gram + 哈希向量，可选真实 Embedding | RAG、TopK、权限过滤、引用 |
| 测试 | JUnit 5、Spring Boot Test、Surefire | 单元测试、真实 MySQL 集成测试 |
| 交付 | Maven、GitHub Actions、Docker Compose、PowerShell 脚本 | 构建、CI、数据库和本地一键启动 |

项目没有依赖 Spring AI 或 LangChain4j。它直接按照 OpenAI-Compatible 接口实现模型调用，
因此框架不是重点，真正可迁移的能力是：工具协议、上下文、权限、确认、幂等、降级和评测。

## Agent 模块的真实结构

```text
浏览器
  -> ui/server.mjs（9000，代理 /api）
  -> AgentTeamController（18080）
  -> AgentTeamService（Supervisor 路由）
  -> Inventory Specialist / Purchase Specialist
  -> AgentPlanner（规则规划或 LLM Tool Calling）
  -> ToolRegistry（工具白名单）
  -> Tool.execute
  -> AgentStore / JDBC / MySQL
```

关键设计是“模型负责理解，Java 负责确定性业务”：

1. 模型只能选择一个已注册工具，并生成符合 JSON Schema 的参数；
2. 专家 Agent 只能看到自己的工具白名单；
3. `confirm_purchase_draft` 不暴露给模型，只能通过请求中的 `confirmDraftId` 触发；
4. Java 再次校验参数、权限、库存、草稿状态和幂等；
5. 模型超时、返回非法工具或参数错误时，自动降级到规则规划器；
6. 所有关键步骤以工具轨迹、handoff 和订单记录留下可审计证据。

这比“让模型生成一段 SQL 然后直接执行”安全，也比“模型直接回答库存还有 12 件”可靠，
因为库存和权限等事实始终来自业务系统。

## 工具如何调用

### 方式一：通过 Agent 对话间接调用

```powershell
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:18080/api/agent/team/chat' `
  -ContentType 'application/json' `
  -Body '{"userId":"demo-user","query":"查询 A001 的库存"}'
```

返回结果包含：

- `selectedAgent`：实际接手的专家；
- `supervisorReason`：为什么路由给该专家；
- `handoffs`：从主管到专家的交接记录；
- `result.contextId`：后续工具调用使用的短期上下文；
- `result.toolCalls`：实际调用的工具、参数和结果；
- `result.planner`：`rule`、`llm`、`rule-fallback` 或 `explicit-confirmation`。

### 方式二：直接调用工具

先用一次聊天请求取得 `contextId`，再调用：

```powershell
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:18080/api/tools/query_stock' `
  -Headers @{
    'X-Agent-Tool-Key' = 'local-demo-tool-key'
    'X-Agent-Context-Id' = '<聊天接口返回的 contextId>'
  } `
  -ContentType 'application/json' `
  -Body '{"inventoryCode":"A001"}'
```

工具 Key 和上下文分别回答两个问题：调用方有没有权限，以及当前调用代表哪个用户。
模型本身不接收登录 Token。

### 采购写操作

第一次请求只生成草稿：

```powershell
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:18080/api/agent/team/chat' `
  -ContentType 'application/json' `
  -Body '{"userId":"demo-user","query":"采购 A001 5 个"}'
```

拿到 `draftId` 后，由用户显式确认：

```json
{
  "userId": "demo-user",
  "query": "确认创建",
  "confirmDraftId": "上一步返回的 draftId"
}
```

同一草稿重复确认会返回同一个订单号，数据库中的 `draft_id` 和订单唯一约束负责最终幂等。

## 多智能体方案好不好

项目确实使用了多智能体，但采用可控的 `Supervisor -> Specialist` 路由，不是多个模型自由聊天。

当前有三个专家：

- `inventory-specialist`：只能查询库存；
- `purchase-specialist`：只能准备采购草稿和执行显式确认；
- `logistics-specialist`：只能按顺丰运单号查询物流轨迹。

这种方式的价值：

- 每个专家的工具权限清晰；
- 路由原因、handoff 和工具轨迹可观察；
- 可以针对错误路由建立固定评测集；
- 后续增加物流、售后或财务专家时边界较清楚。

代价也不能忽略：

- 多了路由层，延迟和调用成本会增加；
- 路由本身可能判断错误；
- 小工具集使用一个 Agent 往往更简单；
- 多 Agent 不自动等于更聪明，必须有评测、权限和失败处理。

所以面试中不要回答“多智能体更先进”。更准确的回答是：

> 当任务边界稳定、工具权限需要隔离、团队需要独立评测时，多 Agent 有价值；如果只是两个简单工具，
> 我会优先用单 Agent，避免增加不必要的路由成本和故障点。

## 在现有代码上增加一个新智能体

以“售后专家”为例，推荐按下面顺序做：

1. 新增工具类，实现 `Tool` 的 `name`、`description`、`inputSchema` 和 `execute`；
2. 把工具声明为 Spring Bean，`ToolRegistry` 会自动收集；
3. 在 `SpecialistAgentRegistry` 注册 `after-sales-specialist`，只允许售后相关工具；
4. 在 `AgentTeamService.route` 增加显式选择和意图路由规则；
5. 在 `AgentTeamEvaluationService` 增加正常路由、缺少参数和越权调用用例；
6. 用控制台或 HTTP 请求验证 `selectedAgent`、`handoff`、`toolCalls` 和 `planner`；
7. 最后补测试并运行 `mvn -f api/pom.xml verify`。

优先复用已有接口和注册表，不要为新 Agent 再复制一套 Controller、数据库连接或权限系统。

## Spring AI、LangChain4j、LangChain 的区别

| 技术 | 语言与定位 | 适合场景 | 注意事项 |
|---|---|---|---|
| LangChain | Python/JS 编排生态 | 快速实验、RAG、Agent、工具和数据连接器 | 不等于 Java 技术栈；版本和抽象变化较快 |
| Spring AI | Java，面向 Spring 生态 | Spring Boot 企业应用接入模型、Tool Calling、向量库和可观测性 | 通常要求较新的 Java/Spring Boot，升级前先核对版本兼容性 |
| LangChain4j | Java 第三方 LLM 应用框架 | Java 中实现 Chat、Tools、RAG、记忆和多模型适配 | 与 Spring 可组合，但项目结构与版本需要自己治理 |
| 当前项目 | Spring Boot 自建网关 | 学习底层协议、权限、幂等和业务闭环 | 代码更透明，但模型供应商适配和向量库能力要自己维护 |

从招聘角度看，框架名称是加分项，但面试官更常追问：

- 模型返回错误工具时怎么处理；
- 上下文和用户权限怎么传递；
- 写操作如何人工确认和保证幂等；
- RAG 如何切分、召回、过滤、评测和引用；
- 外部模型超时、限流或不可用时系统怎么降级。

能用 Spring AI 或 LangChain4j 做完整项目当然可以写简历，但前提是你能解释代码和取舍。
只跟着教程生成一个聊天页面，通常不足以证明 AI 应用开发能力。

## 启动与部署

### 开发环境快速启动

前置条件：Java 8、Maven、Node.js、MySQL 8。项目默认连接
`127.0.0.1:3306/agent_gateway`。

```powershell
cd D:\codexTest\CodexModleTest\public-release
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\status-dev.ps1
```

启动后：

```text
统一入口：http://localhost:9000/
Agent 控制台：http://localhost:9000/agent/
业务控制台：http://localhost:9000/business/
API 健康检查：http://localhost:18080/api/health
```

停止：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1
```

本地数据库密码放在 Git 忽略的 `api/.local/mysql.password`，不要把真实密码写进
`application.yml`、README、聊天记录或 Git 提交。

### 用 Docker 准备干净数据库

如果 3306 已被本机 MySQL 占用，需要先停止冲突服务，或修改
`api/docker-compose.yml` 的宿主机端口映射。

```powershell
cd api
$env:AGENT_DB_PASSWORD='替换为新的测试密码'
$env:MYSQL_ROOT_PASSWORD='替换为新的测试 root 密码'
docker compose up -d mysql redis
```

随后把 `AGENT_DB_PASSWORD` 写入 `api/.local/mysql.password`，再执行一键启动脚本。
初始化 SQL 只在全新数据卷中执行；已有数据卷不会被自动重置。

### 构建和测试

默认测试不需要外部模型：

```powershell
mvn -f api/pom.xml test
node --check ui/server.mjs
```

启用本机真实 MySQL 集成测试时，先设置 `AGENT_DB_USERNAME`、
`AGENT_DB_PASSWORD` 和 `RUN_MYSQL_IT=true`，再运行：

```powershell
mvn -f api/pom.xml verify
```

当前验证结果：

- 默认 42 个测试；
- 启用真实 MySQL 后 46 个测试；
- 多 Agent 路由评测 11/11；
- RAG 评测 10/10；
- GitHub Actions 构建、测试、打包和 UI 语法检查。

### 生产部署的边界

当前脚本面向本地开发，不是完整生产部署方案。生产环境至少还需要：

- 独立管理模型、数据库和工具密钥；
- 配置 HTTPS、反向代理、限流和请求体大小限制；
- 使用 Redis 等共享存储保存短期上下文和防重放状态；
- 增加租户隔离、审计、告警、链路追踪和备份；
- 为外部模型与物流接口设置重试、熔断和对账；
- 给前端构建产物配置静态资源缓存和安全响应头。

## 简历怎么写

以下内容只能在真实参与过相应工作时使用，不能把演示项目中的每个模块都包装成公司上线成果。

### 项目条目

**企业 Agent 与业务集成平台｜Java 全栈开发**

> 技术栈：Java 8、Spring Boot、Spring MVC、Spring JDBC、Vue 2、MySQL、Redis、
> OpenAI-Compatible Tool Calling、RAG、Docker、GitHub Actions

可从下面选择 3 到 5 条，按你的真实职责调整：

- 设计并实现 Agent 工具网关，通过工具注册、JSON Schema、短期上下文和规划器降级，
  将模型意图识别与库存、采购等确定性 Java 业务解耦。
- 实现 `Supervisor -> Specialist` 多 Agent 路由与专家级工具白名单，补充 handoff
  审计和固定评测，覆盖正常路由、参数缺失及越权拦截。
- 将 Agent 上下文、采购草稿和订单持久化到 MySQL，利用唯一约束和状态复用实现重复确认幂等；
  使用事务回滚的真实 MySQL 集成测试验证关键持久化场景。
- 实现 SSO 单点登录链路，使用时间戳、nonce、一次性 code 和签名校验降低重放及
  Token 暴露风险，并完成角色菜单与后端接口双重权限控制。
- 实现条码全生命周期追溯和单据上下游关系追溯，使用事件流、递归查询与 visited
  去重处理循环关系。
- 对接顺丰物流场景，完成报文组装、签名、网关层与业务层双成功判断、重复下单幂等、
  运单号回写、轨迹查询和操作日志。
- 实现带权限过滤和引用来源的 RAG 知识库，支持文档切分、混合检索、可选 Embedding、
  模板降级和固定评测集。
- 建立 Maven 多模块单 API 与独立 Vue 前端结构，实现一键启动、健康检查、Maven 测试、
  Spring Boot 打包和 GitHub Actions CI。

公司实习经历中如果没有真实上线数据，不要编造 QPS、用户量或降本比例。可以写已经验证过的
事实，例如“完成 46 个自动化测试”“路由评测 11/11”“RAG 评测 10/10”。

### 面试自我介绍

> 我之前的 Java 全栈工作涉及企业系统集成，包括 SSO、条码追溯、单据链和顺丰对接。
> 后来我把这些能力做成一个可运行的脱敏作品集，并补上 Agent 工具网关、多 Agent 路由和 RAG。
> 项目里模型只负责选择工具和参数，Java 负责权限、上下文、草稿、确认、幂等和降级，
> 这样更适合企业业务。我也能用 MySQL、Docker、Maven 和 GitHub Actions 完成本地运行与持续集成。

## 建议掌握的能力

AI 应用开发岗位不只看模型 API，通常会同时考察：

1. Python 基础，能使用 FastAPI 或类似框架完成服务；
2. LLM API、Prompt、结构化输出、Tool Calling 和流式响应；
3. RAG 全链路：切分、Embedding、向量检索、重排、引用和评测；
4. 权限、防提示词注入、敏感信息保护和人工确认；
5. 缓存、限流、超时、重试、降级、可观测性和成本控制；
6. Java 或 Python 至少一门语言的工程化能力；
7. Docker、CI/CD、日志和基础部署能力。

你现在最值得补的不是再记十个框架名，而是独立完成三次修改：

1. 增加一个查询工具和一个新专家 Agent；
2. 把哈希 Embedding 换成真实 Embedding，并比较检索评测；
3. 用 Python + FastAPI 或 LangChain 做一个等价的最小版本，理解两套生态的差异。

完成这三步后，简历上的“熟悉 Agent、RAG、工具调用”会更有证据。

## 高频面试问答

**为什么不全用 Python？**

模型调用通常通过 HTTP 协议完成，Java 和 Python 都能接入。Java 更适合复用现有 Spring
企业系统、事务和权限；Python 的 AI 生态和实验速度更有优势。选型应看团队和系统边界。

**这个项目用了 Spring AI 吗？**

没有。它直接实现 OpenAI-Compatible Tool Calling 和 Embedding 协议。这样能看清工具注册、
参数校验、降级和上下文等底层机制；如果改用 Spring AI，应重新核对 Java 和 Spring Boot
版本兼容性，不能只替换依赖就宣称完成迁移。

**如何避免模型乱调工具？**

只暴露白名单工具，校验工具名和 JSON 参数，确认类工具不进入模型候选，Java 重新做权限与业务
校验，模型异常时回退到规则规划器。

**如何避免重复下单？**

先创建草稿，用户显式确认后以草稿 ID 作为数据库唯一作用域；重复确认读取已有订单，
而不是再次执行写操作。

**RAG 如何证明有效？**

维护固定问题集，检查期望文档是否进入 TopK，同时记录命中率和每个问题的引用；
没有评测集的 RAG 很难判断改动是否真的有效。
