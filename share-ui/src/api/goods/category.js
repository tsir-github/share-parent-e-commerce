import request from '@/utils/request'

export function listCategory(query) {
  return request({ url: '/goods/category/list', method: 'get', params: query })
}

export function getCategory(id) {
  return request({ url: '/goods/category/' + id, method: 'get' })
}

export function addCategory(data) {
  return request({ url: '/goods/category', method: 'post', data })
}

export function updateCategory(data) {
  return request({ url: '/goods/category', method: 'put', data })
}

export function delCategory(id) {
  return request({ url: '/goods/category/' + id, method: 'delete' })
}

export function treeselect() {
  return request({ url: '/goods/category/treeselect', method: 'get' })
}
