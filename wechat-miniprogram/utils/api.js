/**
 * 后端接口集合
 * 统一封装 REST 接口, 对应后端 Spring Boot 控制器
 */
const request = require('./request')

module.exports = {
  /** 认证 */
  auth: {
    login: (data) => request.post('/auth/login', data),
    register: (data) => request.post('/auth/register', data),
    logout: () => request.post('/auth/logout'),
    info: () => request.get('/auth/info')
  },

  /** 家庭成员 */
  member: {
    list: () => request.get('/member/list'),
    add: (data) => request.post('/member', data),
    update: (data) => request.put('/member', data),
    remove: (id) => request.del(`/member/${id}`)
  },

  /** 家庭信息 */
  family: {
    me: () => request.get('/family/me'),
    update: (data) => request.put('/family/me', data),
    changePassword: (data) => request.put('/family/password', data)
  },

  /** 收入 */
  income: {
    page: (params) => request.get('/income/page', params),
    add: (data) => request.post('/income', data),
    update: (data) => request.put('/income', data),
    remove: (id) => request.del(`/income/${id}`)
  },

  /** 支出 */
  expense: {
    page: (params) => request.get('/expense/page', params),
    add: (data) => request.post('/expense', data),
    update: (data) => request.put('/expense', data),
    remove: (id) => request.del(`/expense/${id}`)
  },

  /** 预算 */
  budget: {
    page: (params) => request.get('/budget/page', params),
    add: (data) => request.post('/budget', data),
    update: (data) => request.put('/budget', data),
    remove: (id) => request.del(`/budget/${id}`)
  },

  /** 审批 */
  approval: {
    page: (params) => request.get('/approval/page', params),
    add: (data) => request.post('/approval', data),
    approve: (id, params) => request.post(`/approval/${id}/approve`, params),
    reject: (id, params) => request.post(`/approval/${id}/reject`, params),
    pendingCount: (approverId) => request.get('/approval/pending/count', { approverId }),
    remove: (id) => request.del(`/approval/${id}`)
  },

  /** 通知 */
  notification: {
    list: (params) => request.get('/notification/list', params),
    add: (data) => request.post('/notification', data),
    read: (id) => request.put(`/notification/${id}/read`),
    readAll: () => request.post('/notification/read/all'),
    remove: (id) => request.del(`/notification/${id}`),
    unreadCount: () => request.get('/notification/unread/count')
  },

  /** 仪表盘 */
  dashboard: {
    overview: () => request.get('/dashboard/overview'),
    trend: (month) => request.get('/dashboard/trend', { month }),
    recent: (limit) => request.get('/dashboard/recent', { limit })
  },

  /** 统计 */
  statistics: {
    category: (month) => request.get('/statistics/category', { month }),
    monthly: (year) => request.get('/statistics/monthly', { year })
  },

  /** 每日任务 */
  dailyTask: {
    list: (params) => request.get('/daily-task/list', params),
    page: (params) => request.get('/daily-task/page', params),
    add: (data) => request.post('/daily-task', data),
    update: (data) => request.put('/daily-task', data),
    complete: (id, completed) => request.put(`/daily-task/complete/${id}`, { completed }),
    remove: (id) => request.del(`/daily-task/${id}`)
  },

  /** 固定支出 */
  fixedExpense: {
    list: (params) => request.get('/fixed-expense/list', params),
    overview: (params) => request.get('/fixed-expense/overview', params),
    add: (data) => request.post('/fixed-expense', data),
    update: (data) => request.put('/fixed-expense', data),
    remove: (id) => request.del(`/fixed-expense/${id}`)
  },

  /** 生活计划 */
  lifePlan: {
    list: (params) => request.get('/life-plan/list', params),
    add: (data) => request.post('/life-plan', data),
    update: (data) => request.put('/life-plan', data),
    updateStatus: (id, status) => request.put(`/life-plan/status/${id}`, { status }),
    remove: (id) => request.del(`/life-plan/${id}`)
  },

  /** 成长计划 */
  growthPlan: {
    list: (params) => request.get('/growth-plan/list', params),
    add: (data) => request.post('/growth-plan', data),
    update: (data) => request.put('/growth-plan', data),
    updateProgress: (id, progress) => request.put(`/growth-plan/progress/${id}`, { progress }),
    updateStatus: (id, status) => request.put(`/growth-plan/status/${id}`, { status }),
    remove: (id) => request.del(`/growth-plan/${id}`)
  }
}
