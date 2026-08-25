import request from '@/utils/request'

export function getOverview(params) {
  return request({ url: '/order/data-report/overview', method: 'get', params })
}

export function getTrend(params) {
  return request({ url: '/order/data-report/trend', method: 'get', params })
}

export function getProductRanking(params) {
  return request({ url: '/order/data-report/product-ranking', method: 'get', params })
}

export function getMerchantRanking(params) {
  return request({ url: '/order/data-report/merchant-ranking', method: 'get', params })
}

export function getPaymentStats(params) {
  return request({ url: '/order/data-report/payment-stats', method: 'get', params })
}
