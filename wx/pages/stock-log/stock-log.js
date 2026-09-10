// pages/stock-log/stock-log.js — 库存记录（新增/删除商品 + 手动出入库 + 收银出库 + 退款入库）
const { get } = require('../../utils/request')

const TYPE_TAGS = {
  CREATE_PRODUCT: { label: '新增', bg: '#e8f6ef', fg: '#0a8f4c' },
  DELETE_PRODUCT: { label: '删除', bg: '#fdecee', fg: '#e5484d' },
  STOCK_IN: { label: '入库', bg: '#dcfce7', fg: '#15803d' },
  STOCK_OUT: { label: '出库', bg: '#fee2e2', fg: '#b91c1c' },
  SALE: { label: '收银', bg: '#e0edff', fg: '#1d4ed8' },
  REFUND: { label: '退款', bg: '#fef3c7', fg: '#b45309' }
}

// 库存记录相关类型，传给后端过滤
const STOCK_TYPES = Object.keys(TYPE_TAGS).join(',')
const PAGE_SIZE = 20

function mapLog(l) {
  const tag = TYPE_TAGS[l.type] || { label: l.type, bg: '#f4f4f5', fg: '#52525b' }
  return {
    id: l.id,
    type: l.type,
    tagLabel: tag.label,
    tagBg: tag.bg,
    tagFg: tag.fg,
    targetName: l.targetName || '',
    detail: l.detail || '',
    operatorName: l.operatorName || '',
    timeText: l.createTime || ''
  }
}

Page({
  data: {
    logs: [],
    loading: false,
    loadingMore: false,
    hasMore: true,
    page: 1
  },

  onShow() {
    if (!wx.getStorageSync('token')) return
    this.load()
  },

  // 页面上拉触底
  onReachBottom() {
    if (this.data.loadingMore || !this.data.hasMore) return
    this.loadMore()
  },

  // 页面下拉刷新
  async onPullDownRefresh() {
    try {
      await this.load()
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  // 首次加载 / 下拉刷新：重置到第一页
  async load() {
    this.setData({ loading: true, hasMore: true, page: 1 })
    try {
      const res = await get('/operation-logs', { page: 1, pageSize: PAGE_SIZE, types: STOCK_TYPES })
      const records = (res.records || []).map(mapLog)
      this.setData({
        logs: records,
        hasMore: records.length >= PAGE_SIZE,
        page: 1
      })
    } catch (e) {
      // request 已 toast
    } finally {
      this.setData({ loading: false })
    }
  },

  // 加载更多
  async loadMore() {
    const nextPage = this.data.page + 1
    this.setData({ loadingMore: true })
    try {
      const res = await get('/operation-logs', { page: nextPage, pageSize: PAGE_SIZE, types: STOCK_TYPES })
      const records = (res.records || []).map(mapLog)
      this.setData({
        logs: this.data.logs.concat(records),
        hasMore: records.length >= PAGE_SIZE,
        page: nextPage
      })
    } catch (e) {
      // request 已 toast
    } finally {
      this.setData({ loadingMore: false })
    }
  }
})
