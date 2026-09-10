const { BASE_URL } = require('../config')

// 401 登录过期弹窗防重（同一时刻只弹一次，避免并发请求连环弹窗）
let loginModalVisible = false

const LOGIN_KEYS = ['token', 'userId', 'userRole', 'userName', 'userNickname', 'pos_cart']

/** 清除本地登录态与收银台缓存（退出登录 / token 过期共用） */
function clearLoginState() {
  try {
    LOGIN_KEYS.forEach(k => wx.removeStorageSync(k))
  } catch (e) {}
}

/** 登录过期：清缓存，弹窗引导重新登录（不直接跳转） */
function handleUnauthorized() {
  if (loginModalVisible) return
  loginModalVisible = true
  clearLoginState()
  wx.showModal({
    title: '登录已过期',
    content: '登录状态已失效，请重新登录',
    confirmText: '去登录',
    confirmColor: '#111111',
    cancelText: '取消',
    success(res) {
      if (!res.confirm) return
      // 已在登录页则不重复跳转
      const pages = getCurrentPages()
      const cur = pages[pages.length - 1]
      if (cur && cur.route === 'pages/login/login') return
      wx.navigateTo({ url: '/pages/login/login' })
    },
    complete() {
      setTimeout(() => { loginModalVisible = false }, 600)
    }
  })
}

function request(options = {}) {
  return new Promise((resolve, reject) => {
    const path = (options.url || '').replace(/^\/+/, '')
    wx.request({
      url: BASE_URL.replace(/\/+$/, '') + '/' + path,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 15000,
      header: { 'content-type': 'application/json', Authorization: wx.getStorageSync('token') || '' },
      success(res) {
        const { statusCode, data } = res
        if (statusCode >= 200 && statusCode < 300) { resolve(data); return }
        // 401：token 过期/未登录 → 清缓存 + 弹窗，不再 toast
        if (statusCode === 401) {
          handleUnauthorized()
          reject(new Error('unauthorized'))
          return
        }
        const msg = (data && data.msg) || '请求失败(' + statusCode + ')'
        wx.showToast({ title: msg, icon: 'none' })
        reject(new Error(msg))
      },
      fail() {
        wx.showToast({ title: '网络异常，请重试', icon: 'none' })
        reject(new Error('network'))
      }
    })
  })
}

module.exports = {
  get: (url, data) => request({ url, data }),
  post: (url, data) => request({ url, method: 'POST', data }),
  put: (url, data) => request({ url, method: 'PUT', data }),
  del: (url, data) => request({ url, method: 'DELETE', data }),
  clearLoginState
}
