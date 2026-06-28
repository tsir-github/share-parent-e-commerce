# Implementation Plan: 微信小程序登录注册完善

**Branch**: `009-wx-login-profile` | **Date**: 2026-06-27 | **Spec**: [spec.md](spec.md)

## Summary

完善微信小程序 C端用户登录/注册体验：静默登录+自动注册、新用户引导授权头像昵称、已登录用户编辑个人信息。在现有 wxLogin 基础上增加 `isNewUser` 返回标识，补充 profile 更新接口（昵称修改）。

## Technical Context

**Language/Version**: Java 21, Spring Boot 2.7

**Primary Dependencies**: Spring Cloud Alibaba, MyBatis-Plus, WxMaService (binarywang/weixin-java-miniapp)

**Storage**: MySQL (share-user.user_info)，已有字段 avatar_url / nickname / phone

**Testing**: Postman 联调

**Target Platform**: 后端微服务，前端为微信小程序

**Project Type**: 微服务后端，在 share-user 模块内修改/新增接口

**Performance Goals**: 老用户静默登录 <3s

**Constraints**: 无新增数据库表，不引入新依赖

**Scale/Scope**: 小区级 C端用户

## Constitution Check

宪法为模板无实质性约束。 ✅ PASS

## Project Structure

### Source Code

```text
share-modules/share-user/
├── controller/UserAvatarController.java    # 已有：头像上传
├── controller/UserProfileController.java   # 新增：更新昵称 + 获取用户信息
├── api/UserInfoApiController.java          # 修改：wxLogin 增加 isNewUser 返回
├── service/IUserInfoService.java           # 修改：方法签名
└── service/impl/UserInfoServiceImpl.java   # 修改：wxLogin 逻辑 + updateProfile
```

## Complexity Tracking

无违规项。

