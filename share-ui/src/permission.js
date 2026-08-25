import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import merchantRoutes from '@/router/merchant'

NProgress.configure({ showSpinner: false });

const whiteList = ['/login', '/register', '/merchant/login'];

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      // 商家路由：跳过管理员 getInfo / generateRoutes，手动加载商家菜单
      if (to.path.startsWith('/merchant')) {
        const userStore = useUserStore()
        if (userStore.roles.length === 0) {
          userStore.roles = ['merchant']
          userStore.permissions = ['*:*:*']
          // 刷新后从 localStorage 恢复商家信息（Navbar依赖）
          const mi = localStorage.getItem('merchantInfo')
          if (mi) {
            try {
              const info = JSON.parse(mi)
              if (info.name) userStore.name = info.name
              if (info.logo) userStore.avatar = info.logo
            } catch(e) { /* ignore */ }
          }
        }
        // 加载商家侧边栏菜单
        const permStore = usePermissionStore()
        if (permStore.sidebarRouters.length === 0) {
          const sidebarRoutes = merchantRoutes.filter(r => !r.hidden)
          permStore.setSidebarRouters(sidebarRoutes)
        }
        next()
        NProgress.done()
        return
      }
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        useUserStore().getInfo().then(() => {
          isRelogin.show = false
          usePermissionStore().generateRoutes().then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            accessRoutes.forEach(route => {
              if (!isHttp(route.path)) {
                router.addRoute(route) // 动态添加可访问路由表
              }
            })
            next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
          })
        }).catch(err => {
          useUserStore().logOut().then(() => {
            ElMessage.error(err)
            next({ path: '/' })
          })
        })
      } else {
        next()
      }
    }
  } else {
    // 没有token
    if (whiteList.indexOf(to.path) !== -1) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
