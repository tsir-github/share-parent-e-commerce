import request from '@/utils/request'

export function listTemplate(query) {
  return request({ url: '/coupon/couponTemplate/list', method: 'get', params: query })
}

export function getTemplate(id) {
  return request({ url: '/coupon/couponTemplate/' + id, method: 'get' })
}

export function addTemplate(data) {
  return request({ url: '/coupon/couponTemplate', method: 'post', data })
}

export function updateTemplate(data) {
  return request({ url: '/coupon/couponTemplate', method: 'put', data })
}

export function delTemplate(id) {
  return request({ url: '/coupon/couponTemplate/' + id, method: 'delete' })
}
