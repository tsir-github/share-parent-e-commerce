import request from '@/utils/request'

export function listMerchantInfo(query) {
  return request({ url: '/merchant/merchant/list', method: 'get', params: query })
}

export function getMerchantInfo(id) {
  return request({ url: '/merchant/merchant/' + id, method: 'get' })
}

export function auditMerchantInfo(data) {
  return request({ url: '/merchant/merchant/audit', method: 'put', data })
}

export function addMerchantInfo(data) {
  return request({ url: '/merchant/merchant', method: 'post', data })
}

export function updateMerchantInfo(data) {
  return request({ url: '/merchant/merchant', method: 'put', data })
}

export function delMerchantInfo(ids) {
  return request({ url: '/merchant/merchant/' + ids, method: 'delete' })
}
