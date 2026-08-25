import request from '@/utils/request'

// 查询SKU列表
export function listSku(query) {
  return request({ url: '/goods/sku/list', method: 'get', params: query })
}

// 查询SKU详情
export function getSku(id) {
  return request({ url: '/goods/sku/' + id, method: 'get' })
}

// 新增SKU
export function addSku(data) {
  return request({ url: '/goods/sku', method: 'post', data })
}

// 修改SKU
export function updateSku(data) {
  return request({ url: '/goods/sku', method: 'put', data })
}

// 删除SKU
export function delSku(ids) {
  return request({ url: '/goods/sku/' + ids, method: 'delete' })
}
