import request from '@/utils/request'

const prefix = '/goods/api/v1/merchant/product'

export function listProduct(query) {
  return request({ url: prefix + '/list', method: 'get', params: query })
}

export function getProduct(id) {
  return request({ url: prefix + '/' + id, method: 'get' })
}

export function addProduct(data) {
  return request({ url: prefix, method: 'post', data })
}

export function updateProduct(id, data) {
  return request({ url: prefix + '/' + id, method: 'put', data })
}

export function delProduct(id) {
  return request({ url: prefix + '/' + id, method: 'delete' })
}
