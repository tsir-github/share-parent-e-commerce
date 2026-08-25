import request from '@/utils/request'

export function listSeckill(query) {
  return request({ url: '/goods/seckillActivity/list', method: 'get', params: query })
}

export function getSeckill(id) {
  return request({ url: '/goods/seckillActivity/' + id, method: 'get' })
}

export function addSeckill(data) {
  return request({ url: '/goods/seckillActivity', method: 'post', data })
}

export function updateSeckill(data) {
  return request({ url: '/goods/seckillActivity', method: 'put', data })
}

export function updateSeckillStatus(data) {
  return request({ url: '/goods/seckillActivity/status', method: 'put', data })
}

export function delSeckill(ids) {
  return request({ url: '/goods/seckillActivity/' + ids, method: 'delete' })
}

export function getSeckillStats(id) {
  return request({ url: '/goods/seckillActivity/stats/' + id, method: 'get' })
}
