// pages/stock-log/stock-log.js — 出入库记录（来自操作日志，仅显示 STOCK_IN / STOCK_OUT）
const { get } = require('../../utils/request')

Page({
  data: {
    logs: [],
    loading: false,
    refreshing: false
  },

  onShow() {
    if (!wx.getStorageSync('token')) return
    this.load()
  },

  async onRefresh() {
    if (this.data.refreshing) return
    this.setData({ refreshing: true })
    try {
      await this.load()
    } finally {
      this.setData({ refreshing: false })
    }
  },

  async load() {
    this.setData({ loading: true })
    try {
      const list = await get('/operation-logs', { limit: 200 })
      const logs = (list || [])
        .filter(l => l.type === 'STOCK_IN' || l.type === 'STOCK_OUT')
        .map(l => ({
          id: l.id,
          type: l.type,
          targetName: l.targetName || '',
          detail: l.detail || '',
          operatorName: l.operatorName || '',
          timeText: l.createTime || ''
        }))
      this.setData({ logs })
    } catch (e) {
      // request 已 toast；401 会弹重新登录
    } finally {
      this.setData({ loading: false })
    }
  }
})
