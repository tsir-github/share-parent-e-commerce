import request from '@/utils/request'

export function listTemplates() {
  return request({ url: '/coupon/api/v1/merchant/coupon/templates', method: 'get' })
}

export function addTemplate(data) {
  return request({ url: '/coupon/api/v1/merchant/coupon/template', method: 'post', data })
}

export function getTemplateStats(id) {
  return request({ url: '/coupon/api/v1/merchant/coupon/stats/' + id, method: 'get' })
}
