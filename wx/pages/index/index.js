// pages/index/index.js
const { get, post } = require('../../utils/request')

const CART_KEY = 'pos_cart'
Page({
  data: {
    items: [],
    totalText: '0.00',
    scanResult: null, // { name, barcode, price }
    scanLoading: false,
    tmp: { name: '', price: '', qty: '1' },
    tmpShow: false
  },

  onShow() {
    this.loadCart()
  },

  // 进入页面读回购物车（跨页/切 tab 保持一致）
  loadCart() {
    try {
      const c = wx.getStorageSync(CART_KEY)
      if (Array.isArray(c)) this.setData({ items: c })
    } catch (e) {}
    this.recalc()
  },

  /** 同 id 商品合并数量（扫码/库存加入不会出现重复行） */
  mergeDup(list) {
    const m = new Map()
    ;(list || []).forEach(i => {
      const key = 'p' + i.id
      if (m.has(key)) {
        const x = m.get(key)
        x.qty = (Number(x.qty) || 0) + (Number(i.qty) || 0)
      } else {
        m.set(key, Object.assign({}, i))
      }
    })
    return Array.from(m.values())
  },

  recalc() {
    const items = this.mergeDup(this.data.items)
    const total = items.reduce((sum, i) => sum + Number(i.price) * i.qty, 0)
    this.setData({ items, totalText: total.toFixed(2) })
    try {
      wx.setStorageSync(CART_KEY, items)
    } catch (e) {}
  },

  inc(e) {
    const idx = e.currentTarget.dataset.idx
    const item = this.data.items[idx]
    if (!item) return
    // 真实商品（id >= 0）不能超过库存；临时商品无限制
    if (item.id >= 0 && item.stock !== undefined && item.qty >= item.stock) {
      wx.showToast({ title: `库存仅剩 ${item.stock}`, icon: 'none' })
      return
    }
    const items = this.data.items.map((i, n) => (n === idx ? { ...i, qty: i.qty + 1 } : i))
    this.setData({ items })
    this.recalc()
  },

  dec(e) {
    const idx = e.currentTarget.dataset.idx
    const item = this.data.items[idx]
    if (!item || item.qty <= 1) return
    const items = this.data.items.map((i, n) => (n === idx ? { ...i, qty: i.qty - 1 } : i))
    this.setData({ items })
    this.recalc()
  },

  onQtyInput(e) {
    const idx = e.currentTarget.dataset.idx
    const v = e.detail.value
    if (v === '' || /^[0-9]*$/.test(v)) {
      const items = this.data.items.map((i, n) => (n === idx ? { ...i, qty: v } : i))
      this.setData({ items })
      this.recalc()
    }
  },

  onQtyBlur(e) {
    const idx = e.currentTarget.dataset.idx
    const items = this.data.items.slice()
    let qty = parseInt(items[idx].qty, 10)
    if (!qty || qty < 1) qty = 1
    const it = items[idx]
    // 真实商品不能超过库存
    if (it.id >= 0 && it.stock !== undefined && qty > it.stock) {
      qty = it.stock
      wx.showToast({ title: `库存仅剩 ${it.stock}`, icon: 'none' })
    }
    items[idx].qty = qty
    this.setData({ items })
    this.recalc()
  },

  removeItem(e) {
    const idx = e.currentTarget.dataset.idx
    const items = this.data.items.slice()
    items.splice(idx, 1)
    this.setData({ items })
    this.recalc()
  },

  // ── 扫码：拉起微信原生扫码 → 条码查商品 ──
  onScanTap() {
    if (this.data.scanLoading || this.data.scanResult) return
    wx.scanCode({
      scanType: ['barCode'],
      success: (res) => {
        console.log('[scan] wx.scanCode 原始结果:', res)
        this.matchCode(String(res.result || '').trim())
      },
      fail: () => {}
    })
  },

  async matchCode(code) {
    if (!code) return
    this.setData({ scanLoading: true })
    try {
      console.log('[scan] 识别条码:', code)
      const p = await get('/products/barcode/' + encodeURIComponent(code))
      console.log('[scan] 商品查询结果:', p)
      this.setData({ scanLoading: false })
      if (!p) {
        wx.showToast({ title: '未找到该条码的商品', icon: 'none' })
        return
      }
      // 扫码即加入本单
      this.addById(p)
      this.setData({ scanResult: { name: p.name, barcode: p.barcode || code, price: p.price } })
      wx.vibrateShort({ type: 'light' })
    } catch (e) {
      this.setData({ scanLoading: false })
      // 未登录/网络/接口问题：request 已 toast 原因，这里不再弹模拟结果
    }
  },

  closeScan() {
    this.setData({ scanResult: null })
  },

  // 扫码即加购：同商品（同 id）合并数量
  addById(p) {
    if (!p) return
    // 库存为 0 不允许加购
    if (p.stock !== undefined && p.stock <= 0) {
      wx.showToast({ title: '该商品已售罄', icon: 'none' })
      return
    }
    const existing = this.data.items.find(i => i.id === p.id)
    // 已在购物车且达到库存则提示
    if (existing && p.stock !== undefined && existing.qty >= p.stock) {
      wx.showToast({ title: `库存仅剩 ${p.stock}`, icon: 'none' })
      return
    }
    const items = this.data.items.map(i => (i.id === p.id ? { ...i, qty: i.qty + 1 } : i))
    if (!items.some(i => i.id === p.id)) {
      items.push({ id: p.id, name: p.name, spec: p.spec || '', price: Number(p.price) || 0, qty: 1, stock: p.stock })
    }
    this.setData({ items })
    this.recalc()
  },

  // “再扫一件”：关闭面板并再次拉起扫码
  rescan() {
    this.setData({ scanResult: null })
    this.onScanTap()
  },

  // ── 添加临时商品（无条码，手工录入） ──
  openTmp() {
    this.setData({ 'tmp.name': '', 'tmp.price': '', 'tmp.qty': '1', tmpShow: true })
  },

  onTmpName(e) { this.setData({ 'tmp.name': e.detail.value }) },
  onTmpPrice(e) { this.setData({ 'tmp.price': e.detail.value }) },
  onTmpQty(e) { this.setData({ 'tmp.qty': e.detail.value }) },

  closeTmp() {
    this.setData({ tmpShow: false })
  },

  onTmpContainerClose() {
    this.setData({ tmpShow: false })
  },

  confirmTmp() {
    const t = this.data.tmp
    if (!t) return
    const name = String(t.name || '').trim()
    const price = Number(t.price)
    let qty = parseInt(t.qty, 10)
    if (!name) { wx.showToast({ title: '请输入商品名称', icon: 'none' }); return }
    if (!price || price <= 0) { wx.showToast({ title: '请输入正确的单价', icon: 'none' }); return }
    if (!qty || qty < 1) qty = 1
    const items = this.data.items.slice()
    // 临时商品用负 id 占位：避免与数据库真实商品 id 冲突
    items.push({ id: -Date.now(), name, price, qty })
    this.setData({ items, tmpShow: false })
    this.recalc()
  },

  // 清空本单
  clearAll() {
    wx.showModal({
      title: '清空本单',
      content: '确定清空收银台的所有商品吗？',
      confirmText: '清空',
      confirmColor: '#111111',
      success: (res) => {
        if (!res.confirm) return
        this.setData({ items: [] })
        this.recalc()
      }
    })
  },

  noop() {},

  onCheckout() {
    const { items, totalText } = this.data
    if (!items.length) {
      wx.showToast({ title: '本单还没有商品', icon: 'none' })
      return
    }
    wx.showActionSheet({
      itemList: ['微信支付', '支付宝', '现金'],
      success: (res) => {
        const methods = ['wechat', 'alipay', 'cash']
        this.doCheckout(methods[res.tapIndex])
      }
    })
  },

  async doCheckout(payment) {
    const { items, totalText } = this.data
    const labels = { wechat: '微信', alipay: '支付宝', cash: '现金' }
    wx.showModal({
      title: '确认收款',
      content: '应收 ¥' + totalText + '\n以' + labels[payment] + '收款并生成订单？',
      confirmText: '确认收款',
      confirmColor: '#111111',
      success: async (res) => {
        if (!res.confirm) return
        try {
          wx.showLoading({
            title: '结算中',
          })
          const body = {
            payment,
            items: items.map(i => ({
              productId: i.id,
              productName: i.name,
              price: Number(i.price) || 0,
              quantity: Number(i.qty) || 1
            }))
          }
          const order = await post('/orders', body)
          this.setData({ items: [] })
          this.recalc()
          wx.showToast({ title: '收款成功', icon: 'success' })
          if (order && order.orderNo) {
            setTimeout(() => {
              wx.showModal({
                title: '收款成功',
                content: '订单号：' + order.orderNo + '\n已生成订单，可在「订单」中查看。',
                confirmText: '完成',
                showCancel: false
              })
            }, 600)
          }
        } catch (e) {
          // request 已提示原因
        }
      }
    })
  },

  /** 转发给好友/群 */
  onShareAppMessage() {
    const items = this.data.items
    const title = items && items.length
      ? '收银台 · 本单合计 ¥' + this.data.totalText
      : '收银台'
    return { title, path: '/pages/index/index' }
  }
})
