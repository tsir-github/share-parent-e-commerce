/**
 * ★ 登录相关 API
 *
 * 前端与后端登录模块（share-auth 服务）的接口定义。
 * 所有请求走统一的 axios 实例（request），拦截器会自动把 Cookie 中的 JWT 塞进 Authorization 请求头。
 *
 * 涉及后端模块：share-auth → AuthController
 */

import request from '@/utils/request'

/**
 * 登录
 *
 * 触发：用户点击"登录"按钮
 * 后端接口：POST /auth/login → share-auth 服务的 AuthController
 * 请求体：{ username, password, code, uuid }
 *
 * 特殊请求头（不携带 token、允许重复提交）：
 *   isToken: false       → 请求拦截器看到这个标记，不会在请求头里塞 JWT（还没登录呢）
 *   repeatSubmit: false  → 不拦截重复提交（防止用户疯狂点登录按钮）
 *
 * 登录成功后，前端拿到 access_token 存到 Cookie（Admin-Token），后续请求都会带它。
 *
 * @param {string} username 用户名
 * @param {string} password 密码（明文）
 * @param {string} code     验证码
 * @param {string} uuid     验证码在 Redis 中的 key
 */
export function login(username, password, code, uuid) {
  return request({
    url: '/auth/login',
    headers: {
      isToken: false,       // ★ 不携带 JWT（还没登录，哪来的 token）
      repeatSubmit: false   // 允许重复提交（防止快速点击被拦截）
    },
    method: 'post',
    data: { username, password, code, uuid }
  })
}

/**
 * 注册
 *
 * 后端接口：POST /auth/register
 * 同样不需要 token，所以 isToken: false
 *
 * @param {Object} data 注册信息（用户名、密码等）
 */
export function register(data) {
  return request({
    url: '/auth/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

/**
 * 刷新 token（续期）
 *
 * 后端接口：POST /auth/refresh
 * 生成一个新的 JWT，延长有效期
 */
export function refreshToken() {
  return request({
    url: '/auth/refresh',
    method: 'post'
  })
}

/**
 * 获取当前登录用户的详细信息
 *
 * 触发：登录成功后 / 页面刷新时（路由守卫 beforeEach 中调用）
 * 后端接口：GET /system/user/getInfo → share-system 服务的 SysUserController
 *
 * 返回数据：{ user: {...}, roles: [...], permissions: [...] }
 *   user        - 用户基本信息（userId, userName, avatar, nickName 等）
 *   roles       - 角色列表，如 ["admin"]
 *   permissions - 权限标识列表，如 ["system:user:list", "system:user:add"]
 */
export function getInfo() {
  return request({
    url: '/system/user/getInfo',
    method: 'get'
  })
}

/**
 * 退出登录
 *
 * 触发：用户点击"退出登录"
 * 后端：DELETE /auth/logout → AuthController
 *
 * 后端做了两件事：
 *   1. 从请求头解析 JWT，取出 user_key
 *   2. 删除 Redis 中对应的 key（login_tokens:xxx）
 *   之后这个 JWT 再到网关，AuthFilter 查 Redis 发现 key 没了 → 401
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'delete'
  })
}

/**
 * 获取验证码图片
 *
 * 触发：登录页面加载时 / 点击验证码刷新时
 * 后端接口：GET /code → share-auth 服务的 CaptchaController
 *
 * 返回数据：{ img: "base64 图片字符串", uuid: "该验证码在 Redis 中的 key" }
 *   img  → base64 图片，直接给 <img src="data:image/png;base64,..." /> 显示
 *   uuid → 用户提交登录时需要带上的，后端用它从 Redis 中查到验证码文本进行校验
 *
 * 超时时间 20 秒（验证码图片可能外部第三方接口较慢）
 * isToken: false → 不需要 JWT
 */
export function getCodeImg() {
  return request({
    url: '/code',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}