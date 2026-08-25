import request from '@/utils/request'

export function listMerchantInfo(query) {
  return request({ url: '/merchant/merchantInfo/list', method: 'get', params: query })
}

export function getMerchantInfo(id) {
  return request({ url: '/merchant/merchantInfo/' + id, method: 'get' })
}

export function auditMerchantInfo(data) {
  return request({ url: '/merchant/merchantInfo/audit', method: 'put', data })
}

export function addMerchantInfo(data) {
  return request({ url: '/merchant/merchantInfo', method: 'post', data })
}

export function updateMerchantInfo(data) {
  return request({ url: '/merchant/merchantInfo', method: 'put', data })
}

export function delMerchantInfo(ids) {
  return request({ url: '/merchant/merchantInfo/' + ids, method: 'delete' })
}

export function resetMerchantPassword(merchantId, newPassword) {
  return request({ url: '/merchant/merchantInfo/resetPassword', method: 'put', data: { merchantId, newPassword } })
}
