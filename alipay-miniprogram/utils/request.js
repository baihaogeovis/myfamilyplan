/**
 * 请求封装
 * 统一处理 Result{code, message, data} 返回结构、JWT 认证、成员请求头、401 跳转
 */
const config = require('../config')

function getStorage(key) {
  return my.getStorageSync({ key }).data
}

function request(options) {
  const token = getStorage('token')
  const memberId = getStorage('currentMemberId')

  return new Promise((resolve, reject) => {
    my.request({
      url: config.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      headers: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : '',
        'X-Member-Id': memberId ? String(memberId) : ''
      },
      success(res) {
        const body = res.data
        // 登录失效统一处理
        if (res.status === 401 || (body && body.code === 401)) {
          my.removeStorageSync({ key: 'token' })
          my.removeStorageSync({ key: 'familyInfo' })
          my.removeStorageSync({ key: 'currentMemberId' })
          my.removeStorageSync({ key: 'currentMemberName' })
          my.showToast({ type: 'none', content: '登录已过期，请重新登录' })
          setTimeout(() => {
            my.reLaunch({ url: '/pages/login/login' })
          }, 800)
          reject(body || new Error('未授权'))
          return
        }
        if (body && body.code === 200) {
          resolve(body.data)
        } else {
          const msg = (body && body.message) || '请求失败'
          my.showToast({ type: 'none', content: msg })
          reject(body)
        }
      },
      fail(err) {
        my.showToast({ type: 'none', content: '网络异常，请检查后端服务' })
        reject(err)
      }
    })
  })
}

module.exports = {
  get(url, params) {
    return request({ url, method: 'GET', data: params })
  },
  post(url, data) {
    return request({ url, method: 'POST', data })
  },
  put(url, data) {
    return request({ url, method: 'PUT', data })
  },
  del(url, data) {
    return request({ url, method: 'DELETE', data })
  }
}
