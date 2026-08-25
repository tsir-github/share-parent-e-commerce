import request from '@/utils/request'

export function listAfterSale(query) {
  return request({ url: '/order/after-sale/list', method: 'get', params: query })
}

export function auditAfterSale(data) {
  return request({ url: '/order/after-sale/audit', method: 'post', params: data })
}

export function adminAuditAfterSale(data) {
  return request({ url: '/order/after-sale/admin-audit', method: 'post', params: data })
}
