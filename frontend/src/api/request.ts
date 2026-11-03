import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// 请求拦截器
instance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 携带当前操作成员ID(后端校验其属于当前家庭)
    const memberId = localStorage.getItem('currentMemberId')
    if (memberId) {
      config.headers['X-Member-Id'] = memberId
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    const status = error.response?.status
    // 成员新增/修改若被服务端拒绝，把 403 交回页面展示原因，不要当成登录过期清掉会话
    if (status === 403 && isMemberWrite(error.config)) {
      return Promise.reject(error)
    }
    if (status === 401 || status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('currentMemberId')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

/**
 * 统一封装 Axios 实例，默认响应拦截器已经把 Result 解包为 data，
 * 因此这里让方法直接返回业务数据类型，而不是 AxiosResponse。
 */
const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as Promise<T>
  },
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as Promise<T>
  },
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config) as Promise<T>
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config) as Promise<T>
  }
}

function isMemberWrite(config?: { url?: string; method?: string }): boolean {
  const method = String(config?.method || '').toLowerCase()
  if (method !== 'post' && method !== 'put') return false
  const path = String(config?.url || '').split('?')[0]
  return /\/member\/?$/.test(path)
}

export default request
