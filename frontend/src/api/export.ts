import axios from 'axios'
import { ElMessage } from 'element-plus'

function clean(params: Record<string, unknown>) {
  const out: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      out[key] = value
    }
  })
  return out
}

/**
 * 下载 xlsx。走独立请求，避免 JSON 拦截器把文件当成 Result 解包。
 */
export async function downloadExcel(path: string, params: Record<string, unknown>, fallbackName: string) {
  const token = localStorage.getItem('token')
  const memberId = localStorage.getItem('currentMemberId')
  try {
    const response = await axios.get(path, {
      baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
      params: clean(params),
      responseType: 'blob',
      timeout: 60000,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(memberId ? { 'X-Member-Id': memberId } : {})
      }
    })
    const contentType = String(response.headers['content-type'] || '')
    if (contentType.includes('application/json')) {
      const text = await (response.data as Blob).text()
      const body = JSON.parse(text)
      throw new Error(body.message || '导出失败')
    }
    const filename = filenameFrom(response.headers['content-disposition'], fallbackName)
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('已导出')
  } catch (error: any) {
    if (error?.response?.status === 401 || error?.response?.status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('currentMemberId')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return
    }
    const data = error?.response?.data
    if (data instanceof Blob) {
      try {
        const text = await data.text()
        const body = JSON.parse(text)
        ElMessage.error(body.message || '导出失败')
        return
      } catch {
        // 不是 JSON 错误体
      }
    }
    ElMessage.error(error?.message || '导出失败')
  }
}

function filenameFrom(disposition: unknown, fallbackName: string) {
  const header = String(disposition || '')
  const encoded = header.match(/filename\*=(?:UTF-8'')?([^;]+)/i)
  if (encoded) {
    return decodeURIComponent(encoded[1].trim().replace(/^"|"$/g, ''))
  }
  const plain = header.match(/filename="?([^";]+)"?/i)
  if (plain) {
    try {
      return decodeURIComponent(plain[1])
    } catch {
      return plain[1]
    }
  }
  return fallbackName
}

export function exportIncome(params: Record<string, unknown>) {
  return downloadExcel('/export/income', params, '收入记录.xlsx')
}

export function exportExpense(params: Record<string, unknown>) {
  return downloadExcel('/export/expense', params, '支出记录.xlsx')
}

export function exportBudget(params: Record<string, unknown>) {
  return downloadExcel('/export/budget', params, '预算计划.xlsx')
}

export function exportStatistics(params: Record<string, unknown>) {
  return downloadExcel('/export/statistics', params, '收支统计.xlsx')
}
