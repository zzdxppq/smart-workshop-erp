# 14. 给后端 / 前端 / APP 团队的 Handoff 提示（V1.3.7 升级版）

## 14.1 后端工程师

1. **代码生成**：MyBatis Generator 按 schema 生成
2. **统一异常**：`@RestControllerAdvice` 统一处理
3. **统一日志**：`@ApiLog` 注解
4. **统一审计**：`@AuditLog` 注解
5. **统一鉴权**：`@PreAuthorize` 或 `@DataScope`
6. **Stream 消费**：使用 `RedisStreamTemplate`，**必须**实现幂等键
7. **分布式锁**：`DistributedLock`
8. **金额**：**永远用 `BigDecimal`，禁止 `double` / `float`**
9. **V1.3.7 新增**：
   - 工序分配职责 API 严格分离：`OutsubAllocationService`（生管）和 `OutsubOrderService`（采购）**不互相依赖**
   - 163 邮箱调用统一走 `Email163Client`，**不要在业务代码里直接 new JavaMailSender**
   - 字段级加密统一走 `@EncryptedField` + `AesGcmTypeHandler`，**不要在 Service 里手动 encrypt/decrypt**
   - 委外状态转换统一走 `StateMachine.transition()`，**不要 if-else 判断状态**

## 14.2 前端工程师

1-7 不变
8. **V1.3.7 新增**：
   - 对账模块：采购生成对账单 → 发邮件 → 上传签字扫描件 三步走，**不要写"采购带纸去厂商处"等线下流程**
   - 邮件配置后台：163 邮箱授权码字段（input type=password）+ 测试连接按钮
   - 委外面板：7 状态机高亮（即将逾期/返修≥2/待检/已完成）
   - 料号成本检索：5 Tab 切换（价格/材料/工时/外协/总成本）

## 14.3 Android 工程师

1-8 不变
9. **V1.3.7 新增**：
   - APP "到货扫码"入口：扫 WW- 委外单码（**V1.3.5 新增**）
   - APP 消息中心：返修次数 ≥ 2 时强提醒（**V1.3.4**）
   - APP 离线缓存：物料码/工单码/委外单码 三类统一缓存

---
