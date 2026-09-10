const { get, post, put, del } = require('../../utils/request')
const { BASE_UPLOAD_URL } = require('../../config')

const EXPIRY_TYPES = [
  { key: '', label: '不限' },
  { key: 'D', label: '按天' },
  { key: 'M', label: '按月' },
  { key: 'Y', label: '按年' }
]

Page({
  data: {
    // 编辑模式：editId 有值 → 展示并更新既有商品
    editId: 0,
    uploading: false,
    form: {
      barcode: '',
      name: '',
      spec: '',
      price: '',
      stock: '1',
      unit: '个',
      location: '',
      expiryType: '',
      expiryValue: '',
      image: ''
    },
    expiryTypes: EXPIRY_TYPES,
    submitting: false,
    scanning: false,
    deleting: false
  },

  onLoad(options) {
    const id = options && options.id ? Number(options.id) : 0
    if (id) {
      this.setData({ editId: id })
      wx.setNavigationBarTitle({ title: '编辑商品' })
      this.loadProduct(id)
    }
  },

  /** 编辑模式：拉取商品信息回填 */
  async loadProduct(id) {
    try {
      const p = await get('/products/' + id)
      let expiryType = ''
      let expiryValue = ''
      if (p && p.expiry) {
        const m = String(p.expiry).match(/^(\d+)([DMY])$/)
        if (m) { expiryType = m[2]; expiryValue = m[1] }
      }
      this.setData({
        'form.barcode': (p && p.barcode) || '',
        'form.name': (p && p.name) || '',
        'form.spec': (p && p.spec) || '',
        'form.price': p && p.price != null ? String(p.price) : '',
        'form.stock': p && p.stock != null ? String(p.stock) : '1',
        'form.unit': (p && p.unit) || '个',
        'form.location': (p && p.location) || '',
        'form.expiryType': expiryType,
        'form.expiryValue': expiryValue,
        'form.image': (p && p.image) || ''
      })
    } catch (e) {
      // request 已提示
    }
  },

  onBarcodeInput(e) { this.setData({ 'form.barcode': e.detail.value }) },
  onNameInput(e) { this.setData({ 'form.name': e.detail.value }) },
  onSpecInput(e) { this.setData({ 'form.spec': e.detail.value }) },
  onPriceInput(e) { this.setData({ 'form.price': e.detail.value }) },
  onStockInput(e) { this.setData({ 'form.stock': e.detail.value }) },
  onUnitInput(e) { this.setData({ 'form.unit': e.detail.value }) },
  onLocationInput(e) { this.setData({ 'form.location': e.detail.value }) },
  onExpiryValueInput(e) { this.setData({ 'form.expiryValue': e.detail.value }) },
  onExpiryType(e) { this.setData({ 'form.expiryType': e.currentTarget.dataset.key }) },

  // 扫码：识别条码 → 填入 → 查是否已存在（仅新增模式带出已有信息）
  scanBarcode() {
    if (this.data.scanning) return
    this.setData({ scanning: true })
    wx.scanCode({
      scanType: ['barCode'],
      success: (res) => {
        const code = String(res.result || '').trim()
        if (code) {
          this.setData({ 'form.barcode': code })
          if (!this.data.editId) this.checkExists(code)
        }
      },
      complete: () => this.setData({ scanning: false })
    })
  },

  async checkExists(code) {
    try {
      const p = await get('/products/barcode/' + encodeURIComponent(code))
      if (p && p.id) {
        // 扫码命中已有商品：带出其信息便于修改（保存时后端按条码并入库存）
        this.setData({
          'form.name': p.name || '',
          'form.spec': p.spec || '',
          'form.price': p.price != null ? String(p.price) : '',
          'form.unit': p.unit || '个',
          'form.location': p.location || ''
        })
        wx.showModal({
          title: '条码已存在',
          content: '已带出「' + p.name + '」的信息（现库存 ' + p.stock + ' ' + (p.unit || '件') +
            '）。修改后可保存，同条码保存将并入库存。',
          confirmText: '知道了',
          showCancel: false
        })
      }
    } catch (e) {
      // 404 / 未找到视为新条码，无需提示
    }
  },

  // 名称行扫码：扫条码/二维码上的文字填入商品名称
  scanName() {
    if (this.data.scanning) return
    this.setData({ scanning: true })
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (res) => {
        const text = String(res.result || '').trim()
        if (text) this.setData({ 'form.name': text })
      },
      complete: () => this.setData({ scanning: false })
    })
  },

  // ── 商品图片：选图 → 上传 BASE_UPLOAD_URL → 回填 URL ──
  chooseImage() {
    if (this.data.uploading) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = (res.tempFiles || [])[0]
        if (file && file.tempFilePath) this.uploadImage(file.tempFilePath)
      }
    })
  },

  uploadImage(filePath) {
    this.setData({ uploading: true })
    wx.uploadFile({
      url: BASE_UPLOAD_URL,
      filePath,
      name: 'file',
      header: { Authorization: wx.getStorageSync('token') || '' },
      success: (res) => {
        let data = null
        try { data = JSON.parse(res.data || '{}') } catch (e) {}
        // 兼容多种返回：data.url / 顶层 url
        const url = (data && (data.data && data.data.url)) || (data && data.url) || ''
        if (data && (data.code === 200 || data.code === 0) && url) {
          this.setData({ 'form.image': url })
          wx.showToast({ title: '图片已上传', icon: 'success' })
        } else {
          wx.showToast({ title: (data && data.msg) || '上传失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '上传失败，请检查网络', icon: 'none' })
      },
      complete: () => this.setData({ uploading: false })
    })
  },

  removeImage() {
    this.setData({ 'form.image': '' })
  },

  // 提交（新增 POST / 编辑 PUT）
  async submit() {
    const f = this.data.form
    if (this.data.submitting) return
    const name = String(f.name || '').trim()
    const price = Number(f.price)
    if (!name) { wx.showToast({ title: '请输入商品名称', icon: 'none' }); return }
    if (!price || price < 0) { wx.showToast({ title: '请输入正确的销售价', icon: 'none' }); return }

    let expiry = null
    if (f.expiryType) {
      const n = parseInt(f.expiryValue, 10)
      if (!n || n <= 0) { wx.showToast({ title: '请填写有效期数值', icon: 'none' }); return }
      expiry = n + f.expiryType
    }

    const isEdit = !!this.data.editId
    let stock
    if (!isEdit) {
      stock = f.stock === '' ? 1 : Number(f.stock)
      if (!Number.isInteger(stock) || stock < 1) { wx.showToast({ title: '数量最低为 1', icon: 'none' }); return }
    }

    const base = {
      barcode: String(f.barcode || '').trim(),
      name,
      spec: String(f.spec || '').trim(),
      price,
      unit: String(f.unit || '个').trim(),
      location: String(f.location || '').trim(),
      expiry,
      image: String(f.image || '').trim()
    }

    this.setData({ submitting: true })
    try {
      if (isEdit) {
        await put('/products/' + this.data.editId, base)
        wx.showToast({ title: '已保存', icon: 'success' })
      } else {
        const body = Object.assign({ purchasePrice: null, stock }, base)
        const res = await post('/products', body)
        const isMerge = res && (res.type === 'stock_in' || res.added !== undefined)
        wx.showToast({
          title: isMerge ? `已并入库存 +${res.added || ''}` : '添加成功',
          icon: 'success'
        })
      }
      setTimeout(() => wx.navigateBack(), 600)
    } catch (e) {
      // request 已提示原因
    } finally {
      this.setData({ submitting: false })
    }
  },

  // 编辑模式：删除商品（二次确认）
  deleteProduct() {
    const { editId, deleting } = this.data
    if (!editId || deleting) return
    wx.showModal({
      title: '删除商品',
      content: '确定删除该商品吗？删除后不可恢复。',
      confirmText: '删除',
      confirmColor: '#e5484d',
      success: async (res) => {
        if (!res.confirm) return
        this.setData({ deleting: true })
        try {
          await del('/products/' + editId)
          wx.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 600)
        } catch (e) {
          // request 已提示
        } finally {
          this.setData({ deleting: false })
        }
      }
    })
  }
})
