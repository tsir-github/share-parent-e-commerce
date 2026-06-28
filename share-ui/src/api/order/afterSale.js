import request from '@/utils/request'

// 查询售后申请列表
export function listAfterSale(query) {
  return request({ url: '/order/after-sale/list', method: 'get', params: query })
}

// 商家审核售后申请
export function auditAfterSale(data) {
  return request({ url: '/order/after-sale/audit', method: 'post', params: data })
}

// 客服处理售后申请
export function adminAuditAfterSale(data) {
  return request({ url: '/order/after-sale/admin-audit', method: 'post', params: data })
}
