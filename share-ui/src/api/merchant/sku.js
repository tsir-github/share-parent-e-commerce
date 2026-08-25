import request from '@/utils/request'

const prefix = '/goods/api/v1/merchant/sku'

export function listSkuByProduct(productId) {
  return request({ url: prefix + '/product/' + productId, method: 'get' })
}

export function addSku(data) {
  return request({ url: prefix, method: 'post', data })
}

export function updateSku(id, data) {
  return request({ url: prefix + '/' + id, method: 'put', data })
}

export function delSku(id) {
  return request({ url: prefix + '/' + id, method: 'delete' })
}
