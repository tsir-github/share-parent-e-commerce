import request from '@/utils/request'

export function listPaymentInfo(query) {
  return request({ url: '/payment/payment/list', method: 'get', params: query })
}

export function getPaymentInfo(id) {
  return request({ url: '/payment/payment/' + id, method: 'get' })
}

export function refundPayment(data) {
  return request({ url: '/payment/payment/refund', method: 'post', params: data })
}
