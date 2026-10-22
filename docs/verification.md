# Verification

## Agent Gateway

已验证：

- 库存查询
- 采购草稿
- 确认创建
- 重复确认幂等
- 直接工具调用

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
- 20/20 断言通过

> MySQL/Redis 模式已提供 Docker Compose 和 SQL 初始化脚本；当前验证环境 Docker daemon 未启动，因此该模式只完成编译和配置校验。
