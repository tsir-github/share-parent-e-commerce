import request from '@/utils/request'

export function getOrderStats() {
  return request({ url: '/order/orderInfo/dashboard', method: 'get' })
}

export function getUserStats() {
  return request({ url: '/user/userInfo/dashboard', method: 'get' })
}

export function getMerchantStats() {
  return request({ url: '/merchant/merchantInfo/dashboard', method: 'get' })
}
