# API

本目录只包含后端服务，不存放前端页面。

| 模块 | 端口 | 职责 |
|---|---:|---|
| `agent-gateway` | 18080 | Agent 上下文、工具调用、多智能体路由、评测、MySQL 持久化 |
| `business-integrations` | 18081 | 登录权限、SSO、条码、单据追溯、顺丰、RAG |

## 分层方式

每个业务模块按照以下职责组织：

```text
controller/      表现层：接收 HTTP、参数转换、响应
service/         业务层：路由、校验、流程编排
repository/      数据访问层：接口 + 内存/MySQL/Redis 实现
domain/          领域对象：请求、响应、实体
config/          配置和安全
```

`agent-gateway` 中 `repository` 和 `context` 包承担数据访问职责，
`service`、`team`、`service/planner` 承担应用服务职责，`controller` 是 REST 入口。

## 构建

```powershell
mvn -f api/pom.xml test
```

分别启动：

```powershell
mvn -f api/agent-gateway/pom.xml spring-boot:run
mvn -f api/business-integrations/pom.xml spring-boot:run
```
