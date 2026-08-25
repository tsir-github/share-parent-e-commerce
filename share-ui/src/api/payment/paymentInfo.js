import request from '@/utils/request'

export function listPaymentInfo(query) {
  return request({ url: '/payment/paymentInfo/list', method: 'get', params: query })
}

export function getPaymentInfo(id) {
  return request({ url: '/payment/paymentInfo/' + id, method: 'get' })
}

export function refundPayment(data) {
  return request({ url: '/payment/paymentInfo/refund', method: 'post', params: data })
}
