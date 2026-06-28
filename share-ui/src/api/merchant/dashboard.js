import request from '@/utils/request'

export function getDashboard() {
  return request({ url: '/api/v1/merchant/dashboard', method: 'get' })
}

export function getProfile() {
  return request({ url: '/api/v1/merchant/profile', method: 'get' })
}

export function updateProfile(data) {
  return request({ url: '/api/v1/merchant/profile', method: 'put', data })
}

export function updatePassword(data) {
  return request({ url: '/api/v1/merchant/profile/password', method: 'put', data })
}
