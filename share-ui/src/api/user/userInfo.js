import request from '@/utils/request'

export function listUserInfo(query) {
  return request({ url: '/user/userInfo/list', method: 'get', params: query })
}

export function getUserInfo(id) {
  return request({ url: '/user/userInfo/' + id, method: 'get' })
}

export function addUserInfo(data) {
  return request({ url: '/user/userInfo', method: 'post', data })
}

export function updateUserInfo(data) {
  return request({ url: '/user/userInfo', method: 'put', data })
}

export function delUserInfo(ids) {
  return request({ url: '/user/userInfo/' + ids, method: 'delete' })
}
