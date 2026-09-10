const { get, post } = require('../../utils/request')

/** 低库存阈值：0 < stock <= LOW_STOCK 视为低库存 */
const LOW_STOCK = 10

Page({
  data: {
    keyword: '',
    list: [],
    loading: false,
    refreshing: false,
    changing: false,
    sheet: null, // { idx, id, name, barcode, price, stock, unit, spec, location }
    sheetShow: false,
    remark: '',
    soldOutCount: 0,
    lowCount: 0
  },

  onShow() {
    this.load()
  },

  // scroll-view 下拉刷新（仅列表区域触发，搜索头不参与）
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
    const keyword = String(this.data.keyword || '').trim()
    this.setData({ loading: true })
    try {
      const res = await get('/products/search', { keyword, page: 1, pageSize: 500 })
      const list = (res.records || []).map(p => ({
        id: p.id,
        name: p.name,
        barcode: p.barcode || '',
        price: p.price,
        stock: p.stock || 0,
        unit: p.unit || '件',
        spec: p.spec || '',
        location: p.location || '',
        image: p.image || ''
      }))
      const soldOutCount = list.filter(i => i.stock <= 0).length
      const lowCount = list.filter(i => i.stock > 0 && i.stock <= LOW_STOCK).length
      this.setData({ list, soldOutCount, lowCount })
    } catch (e) {
      // request 已提示
    } finally {
      this.setData({ loading: false })
    }
  },

  onKeywordInput(e) {
    const keyword = e.detail.value
    this.setData({ keyword })
    clearTimeout(this._timer)
    this._timer = setTimeout(() => this.load(), 350)
  },

  onSearchConfirm() {
    this.load()
  },

  // 添加商品（跳转表单页，表单内可扫码）
  goAdd() {
    wx.navigateTo({ url: '/pages/product-add/product-add' })
  },

  // 出入库记录
  goStockLog() {
    wx.navigateTo({ url: '/pages/stock-log/stock-log' })
  },

  async openByCode(code) {
    if (!code) return
    try {
      const p = await get('/products/barcode/' + encodeURIComponent(code))
      if (!p) {
        wx.showToast({ title: '未找到该条码的商品', icon: 'none' })
        return
      }
      const idx = this.data.list.findIndex(i => i.id === p.id)
      this.setData({
        sheet: {
          idx,
          id: p.id,
          name: p.name,
          barcode: p.barcode || code,
          price: p.price,
          stock: p.stock || 0,
          unit: p.unit || '件',
          spec: p.spec || '',
          location: p.location || ''
        },
        remark: '',
        sheetShow: true
      })
      wx.vibrateShort({ type: 'light' })
    } catch (e) {
      // request 已提示
    }
  },

  openRow(e) {
    const item = this.data.list[e.currentTarget.dataset.idx]
    if (!item) return
    this.setData({
      sheet: { idx: e.currentTarget.dataset.idx, ...item },
      remark: '',
      sheetShow: true
    })
  },

  closeSheet() {
    this.setData({ sheetShow: false })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  // 编辑：跳转商品新增页（带 id 即编辑模式）
  goEditProduct() {
    const sheet = this.data.sheet
    if (!sheet) return
    this.setData({ sheetShow: false })
    wx.navigateTo({ url: '/pages/product-add/product-add?id=' + sheet.id })
  },

  askQty(isIn) {
    const { sheet, changing } = this.data
    if (!sheet || changing) return
    const title = isIn ? '入库' : '出库'
    wx.showModal({
      title: `${title}「${sheet.name}」`,
      editable: true,
      placeholderText: `当前库存 ${sheet.stock} ${sheet.unit}`,
      confirmText: '确认',
      success: (res) => {
        if (!res.confirm) return
        const qty = parseInt(res.content, 10)
        if (!qty || qty <= 0) {
          wx.showToast({ title: '请输入正确的数量', icon: 'none' })
          return
        }
        this.doAdjust(isIn, qty)
      }
    })
  },

  stockIn() { this.askQty(true) },
  stockOut() { this.askQty(false) },

  async doAdjust(isIn, qty) {
    const { sheet, remark } = this.data
    if (!sheet) return
    this.setData({ changing: true })
    try {
      await post(`/products/${sheet.id}/${isIn ? 'stock-in' : 'stock-out'}`, {
        quantity: qty,
        remark: String(remark || '').trim()
      })
      wx.showToast({ title: isIn ? '入库成功' : '出库成功', icon: 'success' })
      const newStock = Math.max(0, sheet.stock + (isIn ? qty : -qty))
      const list = this.data.list.map((i, n) =>
        n === sheet.idx ? { ...i, stock: newStock } : i
      )
      const soldOutCount = list.filter(i => i.stock <= 0).length
      const lowCount = list.filter(i => i.stock > 0 && i.stock <= LOW_STOCK).length
      this.setData({ list, 'sheet.stock': newStock, remark: '', soldOutCount, lowCount, changing: false })
    } catch (e) {
      this.setData({ changing: false })
    }
  },

  // 加入收银台：写入 pos_cart（收银页 onShow 自动读取）
  addToPos() {
    const sheet = this.data.sheet
    if (!sheet) return
    try {
      let cart = wx.getStorageSync('pos_cart')
      if (!Array.isArray(cart)) cart = []
      const hit = cart.find(i => i.id === sheet.id)
      if (hit) {
        hit.qty += 1
      } else {
        cart.push({ id: sheet.id, name: sheet.name, price: Number(sheet.price) || 0, qty: 1 })
      }
      wx.setStorageSync('pos_cart', cart)
    } catch (e) {}
    this.setData({ sheetShow: false })
    wx.showToast({ title: '已加入收银台', icon: 'success' })
  },

  /** 转发给好友/群 */
  onShareAppMessage() {
    const { keyword, list } = this.data
    const title = keyword
      ? '库存 · 搜索「' + keyword + '」共 ' + list.length + ' 件'
      : '库存 · 共 ' + list.length + ' 件商品'
    return { title, path: '/pages/inventory/inventory' }
  },

  noop() {}
})
