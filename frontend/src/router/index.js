import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'parking/in',
        name: 'ParkingIn',
        component: () => import('@/views/parking/in.vue'),
        meta: { title: '车辆入场', icon: 'ArrowRightBold' }
      },
      {
        path: 'parking/out',
        name: 'ParkingOut',
        component: () => import('@/views/parking/out.vue'),
        meta: { title: '车辆出场', icon: 'ArrowLeftBold' }
      },
      {
        path: 'parking/current',
        name: 'ParkingCurrent',
        component: () => import('@/views/parking/current.vue'),
        meta: { title: '在场车辆', icon: 'List' }
      },
      {
        path: 'parking/history',
        name: 'ParkingHistory',
        component: () => import('@/views/parking/history.vue'),
        meta: { title: '出入场历史', icon: 'Clock' }
      },
      {
        path: 'space/area',
        name: 'SpaceArea',
        component: () => import('@/views/space/area.vue'),
        meta: { title: '区域管理', icon: 'Grid', adminOnly: true }
      },
      {
        path: 'space/space',
        name: 'SpaceSpace',
        component: () => import('@/views/space/space.vue'),
        meta: { title: '车位管理', icon: 'Place' }
      },
      {
        path: 'billing/rule',
        name: 'BillingRule',
        component: () => import('@/views/billing/rule.vue'),
        meta: { title: '收费规则', icon: 'PriceTag', adminOnly: true }
      },
      {
        path: 'billing/order',
        name: 'BillingOrder',
        component: () => import('@/views/billing/order.vue'),
        meta: { title: '收费订单', icon: 'Tickets' }
      },
      {
        path: 'membership/index',
        name: 'Membership',
        component: () => import('@/views/membership/index.vue'),
        meta: { title: '会员月卡', icon: 'CreditCard' }
      },
      {
        path: 'blacklist/index',
        name: 'Blacklist',
        component: () => import('@/views/blacklist/index.vue'),
        meta: { title: '黑名单', icon: 'WarnTriangleFilled', adminOnly: true }
      },
      {
        path: 'report/index',
        name: 'Report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '统计报表', icon: 'TrendCharts', adminOnly: true }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', icon: 'User', adminOnly: true }
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/log.vue'),
        meta: { title: '操作日志', icon: 'Document', adminOnly: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.isLoggedIn) {
    next('/login')
    return
  }
  if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
    return
  }
  if (to.meta.adminOnly && !userStore.isAdmin) {
    next('/dashboard')
    ElMessage?.error?.('无权限访问')
    return
  }
  document.title = to.meta.title ? `${to.meta.title} - 停车场管理系统` : '停车场管理系统'
  next()
})

export default router
