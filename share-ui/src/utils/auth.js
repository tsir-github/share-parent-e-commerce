/**
 * ★ Token 存储工具
 *
 * 登录成功后，后端返回的 JWT 存在哪里？—— 存在浏览器的 Cookie 里。
 *
 * Cookie 的 key 是 "Admin-Token"，value 是整个 JWT 字符串。
 * 后续每个请求都会从 Cookie 中读出这个 JWT，塞进 Authorization 请求头。
 *
 * 存 Cookie 而不是 localStorage 的原因：
 *   1. Cookie 可以设置 HttpOnly（虽然这里没设，但生产环境可以）
 *   2. 刷新页面后 Cookie 不会丢失
 *
 * 存储示例：
 *   Cookie: Admin-Token = eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2tleSI6ImExYjJjM2Q0...
 */

import Cookies from 'js-cookie'

// ★ Cookie 中存储 JWT 的键名
const TokenKey = 'Admin-Token'

// Cookie 中存储过期时间的键名
const ExpiresInKey = 'Admin-Expires-In'

/**
 * 从 Cookie 中取出 JWT
 * 每个请求发出前，请求拦截器会调用此方法
 *
 * @returns {string} JWT 字符串
 */
export function getToken() {
  return Cookies.get(TokenKey)
}

/**
 * 将登录成功后返回的 JWT 存入 Cookie
 *
 * 在 user.js 的 login() action 中调用
 *
 * @param {string} token 后端返回的 access_token（JWT）
 */
export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

/**
 * 退出登录时，从 Cookie 中移除 JWT
 * 在 user.js 的 logOut() action 中调用
 */
export function removeToken() {
  return Cookies.remove(TokenKey)
}

/**
 * 获取 Cookie 中存储的过期时间
 */
export function getExpiresIn() {
  return Cookies.get(ExpiresInKey) || -1
}

/**
 * 设置 Cookie 中的过期时间
 */
export function setExpiresIn(time) {
  return Cookies.set(ExpiresInKey, time)
}

/**
 * 移除 Cookie 中的过期时间
 */
export function removeExpiresIn() {
  return Cookies.remove(ExpiresInKey)
}
