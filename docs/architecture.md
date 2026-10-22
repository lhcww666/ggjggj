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
- Agent Platform：理解意图、选择工具
- Java Tools：权限、校验、草稿、幂等、审计
- MySQL：会话、草稿、幂等记录
- Redis：短期上下文和校验票据

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
