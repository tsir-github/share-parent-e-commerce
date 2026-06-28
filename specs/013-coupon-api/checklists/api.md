# API 需求 Checklist: 优惠券功能

**Purpose**: 验证优惠券 API 需求的完整性、清晰度、一致性和可测性
**Created**: 2026-06-28
**Feature**: [specs/013-coupon-api/spec.md](spec.md)

## 需求完整性

- [ ] CHK001 - C端可领取模板列表是否指定了返回字段（模板名称、类型、优惠金额、条件金额、有效期）？[Completeness, Spec §FR-001]
- [ ] CHK002 - C端领取优惠券的并发控制策略是否在需求中明确？[Completeness, Spec §FR-002]
- [ ] CHK003 - "我的优惠券"列表是否明确需要 JOIN 模板信息（券名称、类型、金额）？[Completeness, Spec §FR-003]
- [ ] CHK004 - 下单可用券查询是否明确需要按优惠力度排序？[Completeness, Spec §FR-004]
- [ ] CHK005 - 商家创建模板时是否明确需要校验必填字段？[Completeness, Spec §FR-005]
- [ ] CHK006 - Feign 接口扩展（lock/release/countAvailable）的契约定义是否在需求中覆盖？[Completeness, Spec §FR-006]
- [ ] CHK007 - 网关路由配置的变更是否已纳入功能需求？[Completeness, Spec §FR-007]

## 需求清晰度

- [ ] CHK008 - "可领取"的定义是否精确——status=1 且在有效期内？[Clarity, Spec §US1-AC1]
- [ ] CHK009 - "每人限领"的判断标准是否明确——按 coupon_user 表记录计数？[Clarity, Spec §US1-AC3]
- [ ] CHK010 - "下单可用"的条件是否明确描述了 type=0/1/2 三种券的不同校验逻辑？[Clarity, Spec §US3]
- [ ] CHK011 - 商家"只能管理自己的"是否明确定义为 merchant_id 过滤？[Clarity, Spec §US4]
- [ ] CHK012 - 模板删除的"已有用户领取"判断标准是否明确（coupon_user 表有记录即不可删除）？[Clarity, Spec §US4-AC4]

## 需求一致性

- [ ] CHK013 - 优惠券状态的枚举值（coupon_user.status 的 0/1/2）是否与 CouponStatus 常量一致？[Consistency, Spec §DB]
- [ ] CHK014 - 模板状态的枚举值（coupon_template.status 的 0/1）是否与 CouponStatus 常量一致？[Consistency, Spec §DB]
- [ ] CHK015 - "已过期"的处理方式（实时过滤 vs 定时任务）是否在需求和假设中保持一致？[Consistency, Spec §Assumptions]
- [ ] CHK016 - 下单时锁定优惠券的状态变更路径（UNUSED → USED）是否与 coupon_user 的状态定义一致？[Consistency, Spec §US5]

## 验收标准质量

- [ ] CHK017 - US1 的验收场景是否覆盖了所有失败路径（已领满/已抢光/已过期）？[Acceptance Criteria, Spec §US1]
- [ ] CHK018 - US3 "下单可用券"的验收标准是否量化了满减条件的具体金额阈值？[Acceptance Criteria, Spec §US3-AC1/2]
- [ ] CHK019 - US5 的验收标准是否覆盖了优惠券释放后的状态回退验证？[Acceptance Criteria, Spec §US5-AC2]
- [ ] CHK020 - 成功标准 SC-001（1秒内响应）的测量方法是否明确？[Measurability, Spec §SC]

## 场景覆盖率

- [ ] CHK021 - 订单取消后优惠券释放是否覆盖了部分退款场景？[Coverage, Spec §US5]
- [ ] CHK022 - 支付成功后的优惠券消耗流程是否在需求中定义？[Coverage, Gap]
- [ ] CHK023 - 优惠券模板被禁用后已领取但未使用的券如何处理？[Coverage, Spec §Edge Cases]
- [ ] CHK024 - 商家修改优惠券模板时，如果剩余量减少到低于已发放量，行为是否明确？[Coverage, Spec §US4-AC3]

## 边界情况覆盖

- [ ] CHK025 - remain_count = -1（不限量）时的扣减逻辑是否在需求中说明？[Edge Cases]
- [ ] CHK026 - 折扣率 discount_rate 的范围和精度是否在需求中定义？[Edge Cases]
- [ ] CHK027 - type=2（无门槛券）时 condition_amt 为 null 的处理是否明确？[Edge Cases, Spec §US3]
- [ ] CHK028 - 多张优惠券同时使用一个订单的场景是否被考虑？[Edge Cases, Spec §Assumptions]

## 非功能需求

- [ ] CHK029 - 高并发领券场景的性能目标是否量化？[NFR, Spec §SC-001]
- [ ] CHK030 - 商家创建模板的并发安全（同名创建/重复创建）是否有约束？[NFR, Gap]
- [ ] CHK031 - 优惠券释放操作失败（Feign 熔断）的重试策略是否定义？[NFR, Spec §FR-006]

## 依赖与假设

- [ ] CHK032 - 假设"每个订单最多使用一张优惠券"是否与 OrderInfo.couponIds（逗号分隔，支持多张）的设计一致？[Assumption, Spec §Assumptions]
- [ ] CHK033 - "过期券不做定时处理"的假设是否会影响 UI 上展示过期券的时效性？[Assumption, Spec §Assumptions]
- [ ] CHK034 - 商家 ID 硬编码（CouponTemplateServiceImpl.merchantId=1）是否会影响商家功能测试？[Assumption, Spec §Assumptions]
