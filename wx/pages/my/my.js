// pages/my/my.js — 我的（Tab4）
const { get, post, clearLoginState } = require('../../utils/request')

const ROLE_TEXT = { admin: '管理员', cashier: '收银员' }

Page({
  data: {
    loggedIn: false,
    user: { name: '', sub: '' },
    orderCountText: ''
  },

  onShow() {
    if (!wx.getStorageSync('token')) {
      this.setData({ loggedIn: false, user: { name: '未登录', sub: '点击登录账号' } })
      return
    }
    this.setData({ loggedIn: true })
    this.loadProfile()
    this.loadOrderCount()
  },

  /** 顶部用户卡点击：未登录 → 登录页；已登录暂无动作 */
  onUserCardTap() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' })
    }
  },

  /** 当前登录用户信息（昵称/角色/账号） */
  async loadProfile() {
    try {
      const u = await get('/auth/info')
      const name = u.nickname || u.username || '未登录'
      const roleText = ROLE_TEXT[u.role] || u.role || ''
      const sub = [roleText, u.username].filter(Boolean).join(' · ')
      this.setData({ user: { name, sub } })
    } catch (e) {
      // request 已 toast；保留占位
    }
  },

  /** 订单记录行尾笔数：取订单分页 total */
  async loadOrderCount() {
    try {
      const res = await get('/orders/search', { page: 1, pageSize: 1 })
      if (res && typeof res.total === 'number') {
        this.setData({ orderCountText: res.total + ' 笔' })
      }
    } catch (e) {
      // request 已 toast；静默
    }
  },

  /** 订单记录 → 订单列表 Tab */
  goOrders() {
    wx.navigateTo({ url: '/pages/order/order' })
  },

  /** 库存记录 → 记录页 */
  goStockLog() {
    wx.navigateTo({ url: '/pages/stock-log/stock-log' })
  },

  /** 门店与员工 / 打印机设置 / 消息通知 / 帮助与反馈：功能即将上线 */
  comingSoon() {
    wx.showToast({ title: '功能即将上线', icon: 'none' })
  },

  /** 退出登录：后端登出 + 清本地 token + 回首页 */
  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      confirmText: '退出',
      confirmColor: '#18181b',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await post('/auth/logout')
        } catch (e) {
          // 后端登出失败也放行本地退出
        }
        clearLoginState()
        wx.showToast({ title: '已退出登录', icon: 'none' })
        setTimeout(() => wx.reLaunch({ url: '/pages/login/login' }), 500)
      }
    })
  },

  /** 转发给好友/群 */
  onShareAppMessage() {
    const u = this.data.user
    const name = (u && u.name) || 'Stock 移动端'
    return {
      title: name + ' · 我的',
      path: '/pages/my/my'
    }
  }
})
