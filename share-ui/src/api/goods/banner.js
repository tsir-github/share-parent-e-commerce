import request from '@/utils/request'

export function listBanner(query) {
  return request({ url: '/goods/banner/list', method: 'get', params: query })
}

export function getBanner(id) {
  return request({ url: '/goods/banner/' + id, method: 'get' })
}

export function addBanner(data) {
  return request({ url: '/goods/banner', method: 'post', data })
}

export function updateBanner(data) {
  return request({ url: '/goods/banner', method: 'put', data })
}

export function updateBannerStatus(data) {
  return request({ url: '/goods/banner/status', method: 'put', data })
}

export function delBanner(ids) {
  return request({ url: '/goods/banner/' + ids, method: 'delete' })
}
