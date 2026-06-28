# 退款闭环需求质量清单

**Purpose**: 售后/退款闭环 spec.md 需求质量验证（单位测试 for 需求文档）

**Created**: 2026-06-24

## 需求完整性 (Requirement Completeness)

- [x] CHK001 - 退款金额计算公式是否明确？含运费分摊、优惠券分摊等场景是否已定义？[Gap, Spec §FR-002]
- [ ] CHK002 - 退款资金链路是否完整定义？微信原路退回 → 支付单状态更新 → 订单状态同步的完整路径是否覆盖？[Completeness, Spec §FR-004/FR-005]
- [ ] CHK003 - 用户侧小程序退款进度展示需要哪些字段？"退款进度"的具体步骤是否应定义（提交申请→审核中→退款中→已完成）？[Gap, Spec §FR-009]
- [ ] CHK004 - 售后申请记录（after_sale_request）的完整生命周期状态是否定义？包括：待审核、已通过退款中、已拒绝、已撤销等。[Gap, Spec §Key Entities]
- [ ] CHK005 - 部分退款场景下 SKU 级别的退款分摊逻辑是否定义了精确的算法？按实付比例还是按固定金额？[Clarity, Spec §FR-012]
- [ ] CHK006 - 补偿定时任务的扫描范围、频率、重试次数是否明确？[Gap, Spec §FR-007, Assumptions]
- [ ] CHK007 - 管理员退款操作需要哪些权限标识（如 after_sale:refund）？权限模型是否定义？[Gap, Spec §FR-011]

## 需求清晰度 (Requirement Clarity)

- [ ] CHK008 - "订单状态变更为已取消"在 FR-005 中是否区分全额退款 vs 部分退款的订单最终状态？[Clarity, Spec §FR-005]
- [ ] CHK009 - "金额超限"在 FR-003 中的阈值绝对值是否明确？超过实付金额的 100% 拒绝，还是设定浮动比例？[Clarity, Spec §FR-003]
- [ ] CHK010 - "售后中（status=5）"在 US2 中明确为订单状态常量，该常量的名称和语义是否在 spec 中定义？[Clarity, Spec §US2]
- [ ] CHK011 - 部分退款场景下"订单主状态保持不变"是保持"已完成"还是保持当前状态？[Ambiguity, Spec §US3]

## 需求一致性 (Requirement Consistency)

- [x] CHK012 - US1 约定"用户在小程序可看到退款进度"与 Assumptions 中"当前 scope 不包含小程序前端"是否矛盾？[Conflict, Spec §US1 Assumptions]
- [x] CHK013 - FR-003 提到"审核确认流程"但 Edge Cases 没有定义"审核中"生命周期状态，两个 section 是否对齐？[Consistency, Spec §FR-003 / Edge Cases]
- [ ] CHK014 - FR-005 定义全额退款→订单取消，但 US3 的部分退款走不同逻辑，FR-005 是否需要拆分全额/部分退款处理规则？[Consistency, Spec §FR-005 / US3]

## 验收标准可测量性 (Acceptance Criteria Quality)

- [ ] CHK015 - SC-001 的"退款处理完成 < 5 秒"是否覆盖了 MQ 延迟和回调时间？该指标的测量起点和终点是否定义清楚？[Measurability, Spec §SC-001]
- [ ] CHK016 - SC-003 的"2 分钟内被补偿处理"的补偿间隔是否可配置？测量方式是端到端还是仅补偿任务的扫描间隔？[Measurability, Spec §SC-003]
- [ ] CHK017 - SC-004 "幂等性 100%"的定义是否包含并发场景？同一订单同时收到两个退款回调时的行为是否定义？[Measurability, Spec §SC-004]

## 场景覆盖 (Scenario Coverage)

- [ ] CHK018 - 退款途中微信接口超时或返回系统错误的重试和超时策略是否定义？[Coverage, Edge Cases]
- [ ] CHK019 - 用户侧未查看退款进度但管理员已完成退款，是否需要推送通知？[Coverage, Gap]
- [ ] CHK020 - 退款操作中的事务边界是否定义？支付侧退款成功 → MQ 发送失败时的回滚策略是否明确？[Coverage, Spec §FR-004/FR-007]
- [ ] CHK021 - 订单已关闭但支付未退款（系统异常导致的不一致）如何处理？[Coverage, Edge Cases]
- [ ] CHK022 - 管理员多次点击退款按钮的防重复提交措施是否定义？[Coverage, Spec §Edge Cases]

## 非功能性需求 (Non-Functional Requirements)

- [ ] CHK023 - MQ 消费者处理的并发数控制是否定义？退款消息是否需要顺序消费？[Gap, Non-Functional]
- [ ] CHK024 - 退款操作审计日志的保留周期是否定义？[Gap, Spec §SC-005]
- [ ] CHK025 - 微信退款接口的限频和流控要求是否被考虑？[Gap, Non-Functional]

## 依赖与假设验证 (Dependencies & Assumptions)

- [ ] CHK026 - "订单状态机已支持 REFUND 操作"的假设是否经过验证？OrderStatusServiceImpl.transition() 是否确实已实现 AFTER_SALE→CANCELLED 的路径？[Assumption, Spec §Assumptions]
- [ ] CHK027 - "支付侧 refund() + handleRefundCallback 已实现"的假设是否覆盖了当前所有退款需求场景？[Assumption, Spec §Assumptions]
- [ ] CHK028 - 补偿定时任务依赖 Redisson 分布式锁 + 固定间隔扫描，在生产环境的间隔是否满足 SC-003 的 2 分钟要求？[Assumption, Spec §Assumptions]

## 未解决歧义 (Ambiguities & Conflicts)

- [x] CHK029 - US2 中"用户申请售后"是否需要区分"仅退款"和"退货退款"两种模式？[Ambiguity, Spec §US2]
- [x] CHK030 - FR-009 "C 端用户查看退款进度"与 Assumptions "US2/US3 当前 scope 不包含小程序前端"存在 scope 冲突，需要澄清 US2/US3 范围。待澄清。[Conflict, Spec §FR-009 / Assumptions]
- [ ] CHK031 - "退款失败"后是否有自动重试逻辑，还是直接标记人工处理？[Ambiguity, Spec §Edge Cases]
