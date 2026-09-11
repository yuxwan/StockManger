const { get, post } = require('../../utils/request')

const PAY_LABELS = { wechat: '微信支付', alipay: '支付宝', cash: '现金' }
const THUMB_COLORS = [
  { bg: '#fef3c7', fg: '#b45309' },
  { bg: '#fee2e2', fg: '#b91c1c' },
  { bg: '#dcfce7', fg: '#15803d' },
  { bg: '#dbeafe', fg: '#1d4ed8' },
  { bg: '#f3e8ff', fg: '#7e22ce' },
  { bg: '#ffe4e6', fg: '#be123c' }
]

function fmt(n) { return (Number(n) || 0).toFixed(2) }

Page({
  data: {
    loading: false,
    order: null,
    items: [],
    refundShow: false,
    refunding: false,
    userMap: {}
  },

  onLoad(options) {
    this.orderId = (options && options.id) ? String(options.id) : ''
    this.fetchUsers()
  },

  onShow() {
    if (!wx.getStorageSync('token')) { wx.reLaunch({ url: '/pages/index/index' }); return }
    this.load()
  },

  // 页面原生下拉刷新
  async onPullDownRefresh() {
    try {
      await this.load()
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  async fetchUsers() {
    try {
      const users = await get('/system/users')
      const map = {}
      ;(users || []).forEach(u => { map[u.id] = u.nickname || u.username })
      this.setData({ userMap: map })
    } catch (e) {}
  },

  async load() {
    this.setData({ loading: true })
    try {
      let id = this.orderId
      if (!id) {
        const page = await get('/orders/search', { page: 1, pageSize: 1 })
        const first = (page.records || [])[0]
        if (!first) { this.setData({ loading: false, order: null }); return }
        id = first.id
        this.orderId = id
      }
      const data = await get('/orders/' + id)
      const order = data.order
      const items = (data.items || []).map((i, idx) => {
        const c = THUMB_COLORS[idx % THUMB_COLORS.length]
        return {
          id: i.id,
          name: i.productName,
          qty: i.quantity,
          refunded: Number(i.refundedQty || 0),
          priceText: fmt(i.price),
          thumb: String(i.productName || '品').substring(0, 1),
          thumbBg: c.bg,
          thumbFg: c.fg
        }
      })
      this.setData({
        loading: false,
        order: {
          id: order.id,
          orderNo: order.orderNo,
          status: order.status, // completed / refunded
          timeText: order.createTime,
          payText: PAY_LABELS[order.payment] || order.payment,
          totalText: fmt(order.total),
          sellerName: this.data.userMap[order.userId] || ''
        },
        items
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  // 发起退款（底部确认）
  openRefund() { this.setData({ refundShow: true }) },
  closeRefund() { this.setData({ refundShow: false }) },

  async confirmRefund() {
    const order = this.data.order
    if (!order || this.data.refunding) return
    this.setData({ refunding: true })
    try {
      await post('/orders/' + order.id + '/refund')
      wx.showToast({ title: '已退款', icon: 'success' })
      this.setData({ refundShow: false })
      this.load()
    } catch (e) {
    } finally {
      this.setData({ refunding: false })
    }
  },

  /** 转发给好友/群：携带订单 id，分享标题含单号与金额 */
  onShareAppMessage() {
    const order = this.data.order
    const title = order && order.orderNo
      ? '订单 ' + order.orderNo + ' · ¥' + order.totalText
      : '订单详情'
    return {
      title,
      path: '/pages/order-detail/order-detail?id=' + this.orderId
    }
  }
})
