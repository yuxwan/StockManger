const { get, post } = require('../../utils/request')

/** 低库存阈值：0 < stock <= LOW_STOCK 视为低库存 */
const LOW_STOCK = 10

/** 无图商品首字占位颜色 */
const THUMB_COLORS = [
  { bg: '#fef3c7', fg: '#b45309' },
  { bg: '#fee2e2', fg: '#b91c1c' },
  { bg: '#dcfce7', fg: '#15803d' },
  { bg: '#dbeafe', fg: '#1d4ed8' },
  { bg: '#f3e8ff', fg: '#7e22ce' },
  { bg: '#ffe4e6', fg: '#be123c' }
]

const PAGE_SIZE = 20

/** 把后端商品映射成列表项，globalIdx 用于首字占位颜色取色，保证分页后颜色一致 */
function mapProduct(p, globalIdx) {
  const item = {
    id: p.id,
    name: p.name,
    barcode: p.barcode || '',
    price: p.price,
    stock: p.stock || 0,
    unit: p.unit || '件',
    spec: p.spec || '',
    location: p.location || '',
    image: p.image || ''
  }
  if (!item.image) {
    const c = THUMB_COLORS[globalIdx % THUMB_COLORS.length]
    item.thumb = p.name ? String(p.name).substring(0, 1) : '品'
    item.thumbBg = c.bg
    item.thumbFg = c.fg
  }
  return item
}

Page({
  data: {
    keyword: '',
    list: [],
    loading: false,
    loadingMore: false,
    hasMore: true,
    page: 1,
    changing: false,
    sheet: null, // { idx, id, name, barcode, price, stock, unit, spec, location }
    sheetShow: false,
    remark: '',
    soldOutCount: 0,
    lowCount: 0,
    // 自定义数量输入弹窗
    qtyModal: { show: false, title: '', placeholder: '', value: '', max: 0, action: '' }
  },

  onShow() {
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

  // 页面上拉触底：加载下一页
  onReachBottom() {
    this.loadMore()
  },

  // 首次加载 / 下拉刷新 / 搜索：重置到第一页
  async load() {
    const keyword = String(this.data.keyword || '').trim()
    this.setData({ loading: true, hasMore: true, page: 1 })
    try {
      const res = await get('/products/search', { keyword, page: 1, pageSize: PAGE_SIZE })
      const records = res.records || []
      const list = records.map((p, i) => mapProduct(p, i))
      const soldOutCount = list.filter(i => i.stock <= 0).length
      const lowCount = list.filter(i => i.stock > 0 && i.stock <= LOW_STOCK).length
      this.setData({
        list,
        soldOutCount,
        lowCount,
        hasMore: records.length >= PAGE_SIZE,
        page: 1
      })
    } catch (e) {
      // request 已提示
    } finally {
      this.setData({ loading: false })
    }
  },

  // 上拉加载更多
  async loadMore() {
    if (this.data.loadingMore || !this.data.hasMore) return
    const nextPage = this.data.page + 1
    const keyword = String(this.data.keyword || '').trim()
    const baseLen = this.data.list.length
    this.setData({ loadingMore: true })
    try {
      const res = await get('/products/search', { keyword, page: nextPage, pageSize: PAGE_SIZE })
      const records = res.records || []
      const more = records.map((p, i) => mapProduct(p, baseLen + i))
      const list = this.data.list.concat(more)
      const soldOutCount = list.filter(i => i.stock <= 0).length
      const lowCount = list.filter(i => i.stock > 0 && i.stock <= LOW_STOCK).length
      this.setData({
        list,
        soldOutCount,
        lowCount,
        hasMore: records.length >= PAGE_SIZE,
        page: nextPage
      })
    } catch (e) {
      // request 已提示
    } finally {
      this.setData({ loadingMore: false })
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

  // 库存记录
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
    if (!isIn && (sheet.stock || 0) <= 0) {
      wx.showToast({ title: '库存为 0，无法出库', icon: 'none' })
      return
    }
    const title = isIn ? '入库' : '出库'
    this.setData({
      qtyModal: {
        show: true,
        title: `${title}「${sheet.name}」`,
        placeholder: `当前库存 ${sheet.stock} ${sheet.unit}`,
        value: '',
        max: isIn ? 99999 : sheet.stock,
        action: isIn ? 'in' : 'out'
      }
    })
  },

  onQtyModalInput(e) {
    this.setData({ 'qtyModal.value': e.detail.value })
  },

  closeQtyModal() {
    this.setData({ 'qtyModal.show': false })
  },

  confirmQtyModal() {
    const { qtyModal } = this.data
    const qty = parseInt(qtyModal.value, 10)
    if (!qty || qty <= 0) {
      wx.showToast({ title: '请输入正确的数量', icon: 'none' })
      return
    }
    if (qtyModal.action === 'in' || qtyModal.action === 'out') {
      this.doAdjust(qtyModal.action === 'in', qty)
    } else if (qtyModal.action === 'pos') {
      this.doAddToPos(qty)
    }
    this.setData({ 'qtyModal.show': false })
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

  // 加入收银台：弹窗输入数量
  addToPos() {
    const sheet = this.data.sheet
    if (!sheet) return
    const stock = sheet.stock || 0
    if (stock <= 0) {
      wx.showToast({ title: '该商品已售罄', icon: 'none' })
      return
    }
    this.setData({
      qtyModal: {
        show: true,
        title: `加入收银台「${sheet.name}」`,
        placeholder: `当前库存 ${stock} ${sheet.unit}`,
        value: '',
        max: stock,
        action: 'pos'
      }
    })
  },

  // 加入收银台：执行写入
  doAddToPos(qty) {
    const sheet = this.data.sheet
    const stock = sheet.stock || 0
    if (qty > stock) {
      wx.showToast({ title: `不能超过库存 ${stock}`, icon: 'none' })
      return
    }
    try {
      let cart = wx.getStorageSync('pos_cart')
      if (!Array.isArray(cart)) cart = []
      const hit = cart.find(i => i.id === sheet.id)
      if (hit) {
        const newQty = hit.qty + qty
        if (newQty > stock) {
          wx.showToast({ title: `加入后超过库存（购物车已有 ${hit.qty}）`, icon: 'none' })
          return
        }
        hit.qty = newQty
      } else {
        cart.push({ id: sheet.id, name: sheet.name, spec: sheet.spec || '', price: Number(sheet.price) || 0, qty, stock })
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
