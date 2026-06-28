import request from '@/utils/request'

export function listOrderInfo(query) {
  return request({ url: '/order/orderInfo/list', method: 'get', params: query })
}

export function getOrderInfo(id) {
  return request({ url: '/order/orderInfo/getOrderInfo/' + id, method: 'get' })
}

export function getByOrderNo(orderNo) {
  return request({ url: '/order/orderInfo/getByOrderNo/' + orderNo, method: 'get' })
}
