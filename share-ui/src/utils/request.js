/**
 * ★ Axios 请求封装 —— 整个前端项目的 HTTP 通信中心
 *
 * 所有 API 请求（登录、查用户、拉数据）都经过这个文件。
 * 这里做了三件事：
 *   1. 请求拦截器 —— 发请求前自动带上 JWT
 *   2. 响应拦截器 —— 收到响应后统一处理错误码（401 弹重新登录、500 弹错误提示等）
 *   3. 通用下载方法
 */

import axios from 'axios'
import { ElNotification , ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import useUserStore from '@/store/modules/user'

let downloadLoadingInstance;
// 控制"重新登录"弹窗只显示一次
export let isRelogin = { show: false };

// token 过期统一处理——区分商家/管理员跳转
function handleTokenExpired() {
  if (isRelogin.show) return
  isRelogin.show = true
  const isMerchant = !!localStorage.getItem('merchantInfo')
  ElMessageBox.confirm('登录状态已过期，请重新登录', '系统提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isRelogin.show = false
    if (isMerchant) {
      localStorage.removeItem('merchantInfo')
      location.href = '/merchant/login'
    } else {
      useUserStore().logOut().then(() => { location.href = '/index' })
    }
  }).catch(() => { isRelogin.show = false })
}

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

// 创建 axios 实例
const service = axios.create({
  // baseURL 来自 .env 文件的 VITE_APP_BASE_API，通常是 '/api' 或 'http://localhost:8080'
  baseURL: import.meta.env.VITE_APP_BASE_API,
  // 默认请求超时时间：10秒
  timeout: 10000
})

// ========================================================================
// 请求拦截器
// 每个请求发出前都会经过这里
// ========================================================================
service.interceptors.request.use(config => {

  // ★ 检查该 API 是否需要携带 token
  // 登录、获取验证码等接口在 headers 中设置 isToken: false，跳过此步骤
  const isToken = (config.headers || {}).isToken === false

  // ★ 检查是否需要防重复提交
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false

  // ★ 如果不是白名单接口，且 Cookie 中有 JWT，就塞进请求头
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }

  // ★ GET 请求的参数处理：将 params 对象拼接到 URL 上
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params);
    url = url.slice(0, -1);  // 去掉最后一个多余的 &
    config.params = {};
    config.url = url;
  }

  // ★ POST/PUT 防重复提交机制
  // 在 1 秒内，同样的请求地址 + 同样的请求体 → 视为重复提交，拦截掉
  // 数据量超过 5MB 时跳过此检查
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length;
    const limitSize = 5 * 1024 * 1024;
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config;
    }
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url;
      const s_data = sessionObj.data;
      const s_time = sessionObj.time;
      const interval = 1000;  // ★ 1 秒内的相同请求视为重复
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交';
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  return config
}, error => {
    console.log(error)
    Promise.reject(error)
})

// ========================================================================
// 响应拦截器
// 每个接口返回后都会经过这里
// ========================================================================
service.interceptors.response.use(res => {
    // 后端返回格式：{ code: 200, msg: "操作成功", data: {...} }
    const code = res.data.code || 200;
    const msg = errorCode[code] || res.data.msg || errorCode['default']

    // 文件下载（blob/arraybuffer）直接返回
    if (res.request.responseType ===  'blob' || res.request.responseType ===  'arraybuffer') {
      return res.data
    }

    // ★ code === 401：未登录或 token 过期
    if (code === 401) {
      handleTokenExpired()
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')

    // ★ code === 500：服务端异常。但"无效的token"也应触发重新登录
    } else if (code === 500) {
      if (msg && (msg.includes('无效的token') || msg.includes('令牌') || msg.includes('登录'))) {
        handleTokenExpired()
        return Promise.reject('token已过期')
      }
      ElMessage({ message: msg, type: 'error' })
      return Promise.reject(new Error(msg))

    // ★ code === 601：自定义业务错误（如权限不足）
    } else if (code === 601) {
      ElMessage({ message: msg, type: 'warning' })
      return Promise.reject(new Error(msg))

    // ★ 其他非 200 错误码
    } else if (code !== 200) {
      ElNotification.error({ title: msg })
      return Promise.reject('error')

    // ★ 200：成功，正常返回
    } else {
      return  Promise.resolve(res.data)
    }
  },
  // ★ 网络层面的错误（不是后端业务错误）
  error => {
    console.log('err' + error)
    let { message } = error;
    if (message == "Network Error") {
      message = "后端接口连接异常";
    } else if (message.includes("timeout")) {
      message = "系统接口请求超时";
    } else if (message.includes("Request failed with status code")) {
      message = "系统接口" + message.substr(message.length - 3) + "异常";
    }
    ElMessage({ message: message, type: 'error', duration: 5 * 1000 })
    return Promise.reject(error)
  }
)

/**
 * 通用文件下载方法
 *
 * @param {string} url      下载接口地址
 * @param {Object} params   请求参数
 * @param {string} filename 下载后的文件名
 * @param {Object} config   附加配置
 */
export function download(url, params, filename, config) {
  downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
  return service.post(url, params, {
    transformRequest: [(params) => { return tansParams(params) }],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...config
  }).then(async (data) => {
    const isBlob = blobValidate(data);
    if (isBlob) {
      const blob = new Blob([data])
      saveAs(blob, filename)
    } else {
      // 如果返回的不是 blob（比如服务端返回了 JSON 错误），解析后弹窗提示
      const resText = await data.text();
      const rspObj = JSON.parse(resText);
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      ElMessage.error(errMsg);
    }
    downloadLoadingInstance.close();
  }).catch((r) => {
    console.error(r)
    ElMessage.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance.close();
  })
}

export default service
