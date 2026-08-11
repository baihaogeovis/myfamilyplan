import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', requiresAuth: true }
      },
      {
        path: 'workbench',
        name: 'Workbench',
        component: () => import('@/views/Workbench.vue'),
        meta: { title: '每日工作台', requiresAuth: true }
      },
      {
        path: 'fixed-expense',
        name: 'FixedExpense',
        component: () => import('@/views/FixedExpense.vue'),
        meta: { title: '固定支出', requiresAuth: true }
      },
      {
        path: 'life-plan',
        name: 'LifePlan',
        component: () => import('@/views/LifePlan.vue'),
        meta: { title: '美好生活计划', requiresAuth: true }
      },
      {
        path: 'growth-plan',
        name: 'GrowthPlan',
        component: () => import('@/views/GrowthPlan.vue'),
        meta: { title: '儿童成长计划', requiresAuth: true }
      },
      {
        path: 'income',
        name: 'Income',
        component: () => import('@/views/Income.vue'),
        meta: { title: '收入管理', requiresAuth: true }
      },
      {
        path: 'expense',
        name: 'Expense',
        component: () => import('@/views/Expense.vue'),
        meta: { title: '支出管理', requiresAuth: true }
      },
      {
        path: 'budget',
        name: 'Budget',
        component: () => import('@/views/Budget.vue'),
        meta: { title: '预算规划', requiresAuth: true }
      },
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/Approval.vue'),
        meta: { title: '资金审批', requiresAuth: true }
      },
      {
        path: 'family',
        name: 'Family',
        component: () => import('@/views/Family.vue'),
        meta: { title: '家庭管理', requiresAuth: true }
      },
      {
        path: 'members',
        name: 'Members',
        component: () => import('@/views/Members.vue'),
        meta: { title: '成员管理', requiresAuth: true }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计报表', requiresAuth: true }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/Notifications.vue'),
        meta: { title: '提醒中心', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
