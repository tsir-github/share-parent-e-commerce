import request from '@/utils/request'

const prefix = '/order/api/v1/merchant/after-sale'

export function listAfterSale(query) {
  return request({ url: prefix + '/list', method: 'get', params: query })
}

export function approveAfterSale(id) {
  return request({ url: prefix + '/' + id + '/approve', method: 'post' })
}

export function rejectAfterSale(id, reason) {
  return request({ url: prefix + '/' + id + '/reject', method: 'post', data: { reason } })
}
