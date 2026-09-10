const { get } = require('../../utils/request')

const PAY_LABELS = { wechat: '微信支付', alipay: '支付宝', cash: '现金' }
function fmt(n) { return (Number(n) || 0).toFixed(2) }

Page({
  data: {
    filter: 'all', // all | completed | refunded
    orders: [],
    shown: [],
    loading: false,
    refreshing: false
  },

  onShow() {
    if (!wx.getStorageSync('token')) { wx.reLaunch({ url: '/pages/index/index' }); return }
    this.load()
  },

  onRefresh() {
    if (this.data.refreshing) return
    this.setData({ refreshing: true })
    this.load().finally(() => this.setData({ refreshing: false }))
  },

  async load() {
    this.setData({ loading: true })
    try {
      const res = await get('/orders/search', { page: 1, pageSize: 30 })
      const orders = (res.records || []).map(o => ({
        id: o.id,
        orderNo: o.orderNo,
        timeText: o.createTime,
        payText: PAY_LABELS[o.payment] || o.payment,
        status: o.status,
        itemCount: o.itemCount || 0,
        totalText: fmt(o.total)
      }))
      this.setData({ orders })
      this.applyFilter()
    } catch (e) {
    } finally {
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

  /** 转发给好友/群 */
  onShareAppMessage() {
    const n = this.data.orders.length
    return {
      title: '订单记录 · 共 ' + n + ' 笔',
      path: '/pages/order/order'
    }
  }
})
