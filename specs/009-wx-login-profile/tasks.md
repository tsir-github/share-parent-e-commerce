# Tasks: 微信小程序登录注册完善

**Input**: Design documents from `specs/009-wx-login-profile/`

**Prerequisites**: [spec.md](spec.md), [plan.md](plan.md), [contracts/](contracts/)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel
- **[Story]**: User story this task belongs to

---

## Phase 1: Setup

**Purpose**: 无需额外初始化，项目已存在。

- [ ] T001 确认 `user_info` 表 `nickname`、`avatar_url`、`phone` 字段可用

---

## Phase 2: User Story 1+2 — 登录注册流程（P1）🎯 MVP

**Goal**: 新用户首次登录自动注册并返回 `isNewUser` 标识，老用户静默登录返回已有信息

**Independent Test**: 用未注册的 openid 调 wxLogin，返回 isNewUser=true；再用同一 openid 调，返回 isNewUser=false

### Implementation

- [ ] T002 [P] [US1] 创建 WxLoginResultDTO 在 `share-modules/share-user/src/main/java/com/share/user/domain/dto/WxLoginResultDTO.java`，字段：token(String)、userInfo(UserInfo)、isNewUser(boolean)
- [ ] T003 [US1] 修改 `IUserInfoService.wxLogin()` 返回类型从 `UserInfo` 改为 `WxLoginResultDTO`（`share-modules/share-user/src/main/java/com/share/user/service/IUserInfoService.java`）
- [ ] T004 [US1] 修改 `UserInfoServiceImpl.wxLogin()` 实现：生成 JWT token、设置 isNewUser 标记、返回 DTO（`share-modules/share-user/src/main/java/com/share/user/service/impl/UserInfoServiceImpl.java`）
- [ ] T005 [US1] 修改 `UserInfoApiController.wxLogin()` 返回类型适配新 DTO（`share-modules/share-user/src/main/java/com/share/user/api/UserInfoApiController.java`）

**Checkpoint**: 调 `/api/v1/user/wxLogin/{code}` 返回 token + userInfo + isNewUser

---

## Phase 3: User Story 3 — 编辑个人信息（P2）

**Goal**: 已登录用户可修改昵称、查看个人信息；头像上传复用已有接口

### Implementation

- [ ] T006 [US3] 创建 UserProfileController 在 `share-modules/share-user/src/main/java/com/share/user/controller/UserProfileController.java`，接口：`GET /api/v1/user/profile` + `PUT /api/v1/user/profile/nickname`
- [ ] T007 [US3] 在 `IUserInfoService` 中新增 `updateNickname(String nickname)` 方法 + 实现（使用 LambdaUpdateWrapper，校验 1-30 字符）

**Checkpoint**: 获取个人信息 + 修改昵称 均可用

---

## Phase 4: Polish

- [ ] T008 校验昵称长度 1-30，空值拒绝
- [ ] T009 编译 share-user 模块验证
- [ ] T010 按 quickstart.md 逐条验证

---

## Dependencies & Execution Order

| Phase | 依赖 |
|---|---|
| P1 Setup | 无 |
| P2 US1+US2 | P1 |
| P3 US3 | P2（注册逻辑必须先完成） |
| P4 Polish | P3 |

**并行机会**: P2 内 T002 可与其他任务并行

### MVP Scope

**Phase 2（US1+US2）**即为 MVP — wxLogin 返回 `isNewUser` + token，前端就可以做新用户引导了。

## Notes

- 无需新建数据库表或字段
- 仅修改 share-user 模块，不涉及其他服务
- 头像上传接口（UserAvatarController）已在前序功能完成，直接复用
- US4 手机号绑定标记为 P3，本次不做
