import request from '@/utils/request'

export function getDashboard() {
  return request({ url: '/merchant/api/v1/merchant/dashboard', method: 'get' })
}

export function getProfile() {
  return request({ url: '/merchant/api/v1/merchant/profile', method: 'get' })
}

export function updateProfile(data) {
  return request({ url: '/merchant/api/v1/merchant/profile', method: 'put', data })
}

export function updatePassword(data) {
  return request({ url: '/merchant/api/v1/merchant/profile/password', method: 'put', data })
}

export function updateUserInfo(data) {
  return request({ url: '/merchant/api/v1/merchant/profile/user', method: 'put', data })
}

export function getUserInfo() {
  return request({ url: '/merchant/api/v1/merchant/profile/user', method: 'get' })
}

export function saveLogo(logoUrl) {
  return request({ url: '/merchant/api/v1/merchant/profile/logo', method: 'post', data: { logoUrl } })
}
