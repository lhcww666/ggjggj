# API

本目录是一个 Spring Boot 后端工程，不存放前端页面。

| 端口 | 职责 |
|---:|---|
| 18080 | Agent、多智能体、SSO、条码、单据链、顺丰、RAG |

## 分层方式

每个业务模块按照以下职责组织：

```text
controller/      表现层：接收 HTTP、参数转换、响应
service/         业务层：路由、校验、流程编排
repository/      数据访问层：接口 + 内存/MySQL/Redis 实现
domain/          领域对象：请求、响应、实体
config/          配置和安全
```

`com.example.agentdemo` 和 `com.example.integrations` 是同一 API 中的两个业务包：

```text
com.example
├── EnterpriseApplication
├── config/DataSourceConfig
├── controller/HealthController
├── agentdemo/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── context/
│   ├── tool/
│   └── model/
└── integrations/
    ├── auth/
    ├── sso/
    ├── barcode/
    ├── billtrace/
    ├── sf/
    ├── rag/
    ├── operation/
    ├── repository/
    └── config/
```

## 构建

```powershell
mvn -f api/pom.xml test
```

健康检查：

- `GET http://localhost:18080/api/health`

启动：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-mysql.ps1
```
