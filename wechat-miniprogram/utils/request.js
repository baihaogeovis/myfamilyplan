/**
 * 请求封装
 * 统一处理 Result{code, message, data} 返回结构、JWT 认证、成员请求头、401 跳转
 */
const config = require('../config')

function request(options) {
  const token = wx.getStorageSync('token')
  const memberId = wx.getStorageSync('currentMemberId')

  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : '',
        'X-Member-Id': memberId ? String(memberId) : ''
      },
      success(res) {
        const body = res.data
        // 登录失效统一处理
        if (res.statusCode === 401 || (body && body.code === 401)) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('familyInfo')
          wx.removeStorageSync('currentMemberId')
          wx.removeStorageSync('currentMemberName')
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => {
            wx.reLaunch({ url: '/pages/login/login' })
          }, 800)
          reject(body || new Error('未授权'))
          return
        }
        if (body && body.code === 200) {
          resolve(body.data)
        } else {
          const msg = (body && body.message) || '请求失败'
          wx.showToast({ title: msg, icon: 'none' })
          reject(body)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端服务', icon: 'none' })
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
