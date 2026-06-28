import request from '@/utils/request'

export function listProduct(query) {
  return request({ url: '/goods/product/list', method: 'get', params: query })
}

export function getProduct(id) {
  return request({ url: '/goods/product/' + id, method: 'get' })
}

export function addProduct(data) {
  return request({ url: '/goods/product', method: 'post', data })
}

export function updateProduct(data) {
  return request({ url: '/goods/product', method: 'put', data })
}

export function delProduct(ids) {
  return request({ url: '/goods/product/' + ids, method: 'delete' })
}
