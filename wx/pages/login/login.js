// pages/login/login.js — 登录页（账号密码，对接 /api/auth/login）
const { post } = require('../../utils/request')

Page({
  data: {
    statusBarHeight: 20,
    username: '',
    password: '',
    showPwd: false,
    logging: false
  },

  onLoad() {
    try {
      const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
      this.setData({ statusBarHeight: info.statusBarHeight || 20 })
    } catch (e) {}
  },

  onUsername(e) {
    this.setData({ username: e.detail.value })
  },

  onPassword(e) {
    this.setData({ password: e.detail.value })
  },

  togglePwd() {
    this.setData({ showPwd: !this.data.showPwd })
  },

  /** 登录：校验 → 调后端 → 存 token → 进收银台（Tab） */
  async onLogin() {
    const username = String(this.data.username || '').trim()
    const password = this.data.password || ''
    if (!username) {
      wx.showToast({ title: '请输入账号', icon: 'none' })
      return
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    if (this.data.logging) return
    this.setData({ logging: true })
    wx.showLoading({
      title: '登陆中~',
    })
    try {
      const res = await post('/auth/login', { username, password })
      if (!res || !res.token) {
        wx.showToast({ title: '登录失败，请稍后重试', icon: 'none' })
        return
      }
      wx.setStorageSync('token', res.token)
      if (res.userId) wx.setStorageSync('userId', res.userId)
      if (res.role) wx.setStorageSync('userRole', res.role)
      if (res.username) wx.setStorageSync('userName', res.username)
      if (res.nickname) wx.setStorageSync('userNickname', res.nickname)
      this.setData({ logging: false })
      // 样例：登录后进入收银台
      wx.showToast({ title: '登录成功', icon: 'none' })
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' })
      }, 1000);
    } catch (e) {
      // request 已 toast 后端 msg（账号或密码错误/账号禁用等）
      this.setData({ logging: false })
    }
  }
})
