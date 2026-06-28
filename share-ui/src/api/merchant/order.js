import request from '@/utils/request'

const prefix = '/api/v1/merchant/order'

export function listOrder(query) {
  return request({ url: prefix + '/list', method: 'get', params: query })
}

export function getOrder(id) {
  return request({ url: prefix + '/' + id, method: 'get' })
}

export function deliverOrder(data) {
  return request({ url: prefix + '/deliver', method: 'post', data })
}
