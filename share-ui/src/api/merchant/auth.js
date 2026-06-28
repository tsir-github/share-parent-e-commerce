/**
 * 商家登录相关 API
 *
 * 涉及后端模块：share-auth → MerchantTokenController
 */

import request from '@/utils/request'

/**
 * 商家登录
 *
 * @param {string} username 用户名
 * @param {string} password 密码
 */
export function merchantLogin(username, password) {
  return request({
    url: '/auth/merchant/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: { username, password }
  })
}

/**
 * 商家登出
 */
export function merchantLogout() {
  return request({
    url: '/auth/merchant/logout',
    method: 'post'
  })
}

/**
 * 获取当前商家信息
 */
export function merchantGetInfo() {
  return request({
    url: '/auth/merchant/getInfo',
    method: 'get'
  })
}
