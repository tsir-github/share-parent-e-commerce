/**
 * ★ 用户状态管理（Pinia Store）
 *
 * 前端全局的用户信息仓库。整个应用的任意组件都可以从这里获取当前登录用户的信息。
 * Pinia 是 Vue3 官方推荐的状态管理，替代 Vuex。
 *
 * 这个 store 管理了：
 *   - token（JWT，存在 Cookie 中，同时缓存在 state 中方便读取）
 *   - 用户基本信息（id, name, avatar）
 *   - 角色和权限列表（用于前端路由鉴权和按钮级别的权限控制）
 *
 * 生命周期：
 *   登录         → login()    → JWT 存 Cookie + state
 *   页面刷新      → 路由守卫   → getInfo() → 从后端拉用户信息
 *   退出 / 401   → logOut()   → 清 Cookie + state
 */

import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import defAva from '@/assets/images/profile.jpg'

const useUserStore = defineStore(
  'user',   // store 的唯一 ID，在组件中用 useUserStore() 调用
  {
    state: () => ({
      // ★ token 初始化时从 Cookie 中读
      // 这样刷新浏览器时 state 会重置，但 Cookie 还在，所以 token 不会丢
      token: getToken(),
      id: '',
      name: '',
      avatar: '',
      roles: [],          // 角色列表，如 ["admin"]
      permissions: []     // 权限标识列表，如 ["system:user:list"]
    }),
    actions: {
      /**
       * ★ 登录
       *
       * 调用后端登录接口 → 把返回的 access_token 存 Cookie + state
       *
       * 流程：
       *   1. 调 this.login() → api/login.js 的 login()
       *      → POST /auth/login → 后端返回 { access_token: "eyJ..." }
       *   2. 把 access_token 存到 Cookie 中（auth.js 的 setToken）
       *   3. 同时存到 state.token 中
       *
       * @param {Object} userInfo { username, password, code, uuid }
       */
      login(userInfo) {
        const username = userInfo.username.trim()
        const password = userInfo.password
        const code = userInfo.code
        const uuid = userInfo.uuid
        return new Promise((resolve, reject) => {
          login(username, password, code, uuid).then(res => {
            let data = res.data
            setToken(data.access_token)    // 存到 Cookie（持久化）
            this.token = data.access_token // 存到 state（运行时快速访问）
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      },

      /**
       * ★ 获取当前登录用户的信息
       *
       * 触发时机：登录成功后 / 页面刷新时
       *   路由守卫（router.beforeEach）中调用，确保进入任何页面之前用户信息已就绪
       *
       * 调后端 /system/user/getInfo，拿到：
       *   - 用户基本信息（userId, userName, avatar, nickName 等）
       *   - 角色列表
       *   - 权限标识列表（用于按钮级权限控制）
       */
      getInfo() {
        return new Promise((resolve, reject) => {
          getInfo().then(res => {
            const user = res.user
            const avatar = (user.avatar == "" || user.avatar == null) ? defAva : user.avatar;

            // 角色和权限：如果有则存起来，没有则给个默认角色
            if (res.roles && res.roles.length > 0) {
              this.roles = res.roles
              this.permissions = res.permissions
            } else {
              this.roles = ['ROLE_DEFAULT']
            }
            this.id = user.userId
            this.name = user.userName
            this.avatar = avatar
            resolve(res)
          }).catch(error => {
            reject(error)
          })
        })
      },

      /**
       * ★ 退出登录
       *
       * 触发时机：用户手动点击退出 / 401 弹窗确认重新登录
       *
       * 流程：
       *   1. 调后端 DELETE /auth/logout → 后端删除 Redis 中的登录缓存
       *   2. 清空 state 中的 token / roles / permissions
       *   3. 删除 Cookie 中的 JWT
       */
      logOut() {
        return new Promise((resolve, reject) => {
          logout(this.token).then(() => {
            this.token = ''
            this.roles = []
            this.permissions = []
            removeToken()  // 从 Cookie 中删除 JWT
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      }
    }
  })

export default useUserStore
