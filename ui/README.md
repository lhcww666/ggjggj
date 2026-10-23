# UI

前端通过独立的 Node 静态服务运行，不依赖 Spring Boot 托管页面。

```powershell
npm run dev --prefix ui
```

访问 `http://localhost:9000/`。

健康检查：`GET http://localhost:9000/health`

代理规则：

| 前端路径 | 后端 |
|---|---|
| `/agent-api/**` | `http://127.0.0.1:18080/**` |
| `/business-api/**` | `http://127.0.0.1:18081/**` |

如后端端口不同，可设置 `AGENT_API_URL` 和 `BUSINESS_API_URL`，例如：

```powershell
$env:AGENT_API_URL="http://127.0.0.1:19080"
npm run dev --prefix ui
```

页面目录：

- `agent-console/`：多智能体路由、工具轨迹和评测
- `business-console/`：SSO、条码、单据链、顺丰和 RAG
