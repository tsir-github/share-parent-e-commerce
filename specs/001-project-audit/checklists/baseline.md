# Baseline Checklist: 项目全面审计与基线

**Purpose**: 验证规格文档中已实现/部分实现/未实现功能的描述是否完整、清晰、一致
**Created**: 2026-06-23
**Feature**: specs/001-project-audit/spec.md

## Requirement Completeness

- [ ] CHK001 是否所有 7 个业务模块（goods/order/payment/coupon/user/merchant/file）的成熟度评估都有明确的标准？[Completeness, Spec §模块成熟度总览]
- [ ] CHK002 是否每个功能的实现状态（✅/⚠️/❌）都有可验证的依据？[Completeness, Spec §Functional Requirements]
- [ ] CHK003 是否所有已知缺陷都有对应的修复建议或替代方案？[Completeness, Spec §已知缺陷清单]
- [ ] CHK004 是否所有缺失的 Feign 接口（RemoteCouponService/RemoteMerchantService/RemotePaymentService）都记录在案？[Gap, Spec §Key Entities]
- [ ] CHK005 是否所有数据库已建表但无 Service 的实体都列入了缺失清单？[Completeness, Spec §Key Entities]

## Requirement Clarity

- [ ] CHK006 "部分实现"和"骨架"的判定标准是否清晰可区分？[Clarity, Spec §模块成熟度总览]
- [ ] CHK007 P0/P1/P2 的优先级划分是否有明确的业务影响说明？[Clarity, Spec §已知缺陷清单]
- [ ] CHK008 "核心链路可用"与"核心链路不可用"的定义是否准确？[Clarity]
- [ ] CHK009 是否所有 TODO 和 ponytail 注释都关联到具体的缺陷条目？[Traceability, Spec §已知缺陷清单]

## Coverage & Edge Cases

- [ ] CHK010 是否记录了高并发场景下的并发控制现状（乐观锁/Redisson锁/无控制）？[Coverage, Spec §Edge Cases]
- [ ] CHK011 是否覆盖了分布式事务场景（Seata 可用但未使用）？[Coverage, Spec §Assumptions]
- [ ] CHK012 是否记录了支付回调超时/重复通知的幂等性设计？[Coverage, Spec §Edge Cases]
- [ ] CHK013 是否记录了 RocketMQ 消费者幂等性设计（业务唯一键去重）？[Coverage]
- [ ] CHK014 是否记录了订单超时取消与手动取消并发触发时状态机如何保证正确性？[Coverage, Spec §Edge Cases]

## Dependencies & Assumptions

- [ ] CHK015 "C 端应用在独立项目"的假设是否明确记载？[Assumption, Spec §Assumptions]
- [ ] CHK016 "现有乐观锁方案足够"的假设是否记录了升级条件？[Assumption, Spec §Assumptions]
- [ ] CHK017 "Mock 模式用于开发"的假设是否说明了生产环境切换方案？[Assumption, Spec §Assumptions]
- [ ] CHK018 "数据库表结构已冻结"的假设是否考虑了未来表结构变更的流程？[Assumption, Spec §Assumptions]

## Acceptance Criteria Quality

- [ ] CHK019 SC-001（90% P0 功能可运行）的衡量方法是否明确？[Measurability, Spec §SC-001]
- [ ] CHK020 SC-002（订单创建 < 500ms）的测试环境和取样方法是否说明？[Measurability, Spec §SC-002]
- [ ] CHK021 SC-003（优惠券无超发）的验证场景是否覆盖了高并发边界？[Measurability, Spec §SC-003]
- [ ] CHK022 SC-004（取消订单 100% 回滚）的验收标准是否包含库存和优惠券两方面？[Measurability, Spec §SC-004]

## Notes

- Check items off as completed: `[x]`
- 本清单验证 spec.md 的需求描述质量，非验证实现正确性
- 所有 CHK 编号全局唯一，后续可追加
