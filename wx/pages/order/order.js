const { get } = require('../../utils/request')

const PAGE_SIZE = 20
const PAY_LABELS = { wechat: '微信支付', alipay: '支付宝', cash: '现金' }
function fmt(n) { return (Number(n) || 0).toFixed(2) }

Page({
  data: {
    filter: 'all',
    orders: [],
    shown: [],
    page: 1,
    hasMore: true,
    loading: false
  },

  onShow() {
    if (!wx.getStorageSync('token')) { wx.reLaunch({ url: '/pages/index/index' }); return }
    // 首次进入显示全屏加载态；从详情返回时静默更新，避免列表闪烁
    this.load(this.data.shown.length > 0)
  },

  // 页面原生下拉刷新（静默刷新：不切换 loading，避免底部提示闪动导致列表闪烁）
  async onPullDownRefresh() {
    try {
      await this.load(true)
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) this.loadMore()
  },

  // silent=true 时为下拉刷新：已有列表数据，不显示任何加载提示
  async load(silent) {
    if (!silent) this.setData({ loading: true })
    try {
      const res = await get('/orders/search', { page: 1, pageSize: PAGE_SIZE })
      const orders = (res.records || []).map(o => ({
        id: o.id,
        orderNo: o.orderNo,
        timeText: o.createTime,
        payText: PAY_LABELS[o.payment] || o.payment,
        status: o.status,
        itemCount: o.itemCount || 0,
        totalText: fmt(o.total)
      }))
      // orders 与筛选后的 shown 一次性更新，只渲染一次
      const filter = this.data.filter
      const shown = filter === 'all' ? orders : orders.filter(o => o.status === filter)
      this.setData({
        orders,
        shown,
        page: 1,
        hasMore: orders.length >= PAGE_SIZE,
        loading: false
      })
    } catch (e) {
      if (!silent) this.setData({ loading: false })
    }
  },

  async loadMore() {
    if (!this.data.hasMore) return
    const nextPage = this.data.page + 1
    this.setData({ loading: true })
    try {
      const res = await get('/orders/search', { page: nextPage, pageSize: PAGE_SIZE })
      const newOrders = (res.records || []).map(o => ({
        id: o.id,
        orderNo: o.orderNo,
        timeText: o.createTime,
        payText: PAY_LABELS[o.payment] || o.payment,
        status: o.status,
        itemCount: o.itemCount || 0,
        totalText: fmt(o.total)
      }))
      const orders = this.data.orders.concat(newOrders)
      const filter = this.data.filter
      const shown = filter === 'all' ? orders : orders.filter(o => o.status === filter)
      this.setData({
        orders,
        shown,
        page: nextPage,
        hasMore: newOrders.length >= PAGE_SIZE,
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  applyFilter() {
    const { orders, filter } = this.data
    this.setData({ shown: filter === 'all' ? orders.slice() : orders.filter(o => o.status === filter) })
  },

  onFilter(e) {
    this.setData({ filter: e.currentTarget.dataset.key }, () => this.applyFilter())
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + id })
  },

  onShareAppMessage() {
    const n = this.data.orders.length
    return {
      title: '订单记录 · 共 ' + n + ' 笔',
      path: '/pages/order/order'
    }
  }
})
