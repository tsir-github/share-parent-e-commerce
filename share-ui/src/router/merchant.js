/* Layout */
import Layout from '@/layout'

/**
 * 商家端路由配置
 *
 * 这些路由与管理员路由共存于同一套前端工程中，
 * 通过后端返回的菜单权限控制显示（merchant 角色可见）。
 */

const merchantRoutes = [
  {
    path: '/merchant/login',
    component: () => import('@/views/merchant/login.vue'),
    hidden: true
  },
  {
    path: '/merchant',
    component: Layout,
    redirect: '/merchant/dashboard',
    meta: { title: '商家管理', icon: 'shopping' },
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/merchant/dashboard/index.vue'),
        name: 'MerchantDashboard',
        meta: { title: '控制台', icon: 'dashboard' }
      },
      {
        path: 'product',
        component: () => import('@/views/merchant/product/index.vue'),
        name: 'MerchantProduct',
        meta: { title: '商品管理', icon: 'goods' }
      },
      {
        path: 'product/add',
        component: () => import('@/views/merchant/product/edit.vue'),
        name: 'MerchantProductAdd',
        hidden: true,
        meta: { title: '新增商品', activeMenu: '/merchant/product' }
      },
      {
        path: 'product/edit/:id',
        component: () => import('@/views/merchant/product/edit.vue'),
        name: 'MerchantProductEdit',
        hidden: true,
        meta: { title: '编辑商品', activeMenu: '/merchant/product' }
      },
      {
        path: 'order',
        component: () => import('@/views/merchant/order/index.vue'),
        name: 'MerchantOrder',
        meta: { title: '订单管理', icon: 'list' }
      },
      {
        path: 'order/detail/:id',
        component: () => import('@/views/merchant/order/detail.vue'),
        name: 'MerchantOrderDetail',
        hidden: true,
        meta: { title: '订单详情', activeMenu: '/merchant/order' }
      },
      {
        path: 'after-sale',
        component: () => import('@/views/merchant/afterSale/index.vue'),
        name: 'MerchantAfterSale',
        meta: { title: '售后管理', icon: 'refund' }
      },
      {
        path: 'setting',
        component: () => import('@/views/merchant/setting/index.vue'),
        name: 'MerchantSetting',
        meta: { title: '店铺设置', icon: 'setting' }
      }
    ]
  }
]

export default merchantRoutes
