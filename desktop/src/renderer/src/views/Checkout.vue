<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import message from '../utils/message'
import { productApi, orderApi, systemUserApi } from '../api'

const products = ref([])
const loading = ref(false)
const searchQuery = ref('')
const searchLoading = ref(false)
let searchTimer = null
const cart = ref({})
const payment = ref('wechat')
const discount = ref(0)

// ── 无图商品首字占位色（与小程序端一致） ──
const THUMB_COLORS = [
  { bg: '#fef3c7', fg: '#b45309' },
  { bg: '#fee2e2', fg: '#b91c1c' },
  { bg: '#dcfce7', fg: '#15803d' },
  { bg: '#dbeafe', fg: '#1d4ed8' },
  { bg: '#f3e8ff', fg: '#7e22ce' },
  { bg: '#ffe4e6', fg: '#be123c' }
]
function thumbStyle(idx) {
  const c = THUMB_COLORS[idx % THUMB_COLORS.length]
  return { backgroundColor: c.bg, color: c.fg }
}

// ── 销售员（默认当前登录人，可临时切换） ──
const staffList = ref([])
const currentStaffId = ref(Number(localStorage.getItem('userId')) || null)

async function fetchStaffList() {
  try {
    // 只展示收银员角色（管理员不参与收银）
    const users = await systemUserApi.cashiers()
    staffList.value = (users || [])
      .filter(u => u.status !== 0)
      .map(u => ({ label: u.nickname || u.username, value: u.id }))
    // 若当前选择不在列表中，落到第一个
    if (!staffList.value.some(s => s.value === currentStaffId.value)) {
      currentStaffId.value = staffList.value[0]?.value ?? null
    }
  } catch {}
}

async function fetchProducts() {
  try {
    products.value = await productApi.list()
  } catch {
    // 错误已在拦截器处理
  }
}

async function searchProducts(keyword) {
  searchLoading.value = true
  try {
    const res = await productApi.search(keyword, 1, 50)
    products.value = res.records
  } catch {
    // 错误已在拦截器处理
  } finally {
    searchLoading.value = false
  }
}

function onSearchInput(val) {
  clearTimeout(searchTimer)
  if (!val.trim()) {
    fetchProducts()
    return
  }
  searchTimer = setTimeout(() => searchProducts(val.trim()), 300)
}

onMounted(async () => {
  window.addEventListener('barcode-scanned', handleBarcodeEvent)

  loadHeldOrders()

  await Promise.all([fetchProducts(), fetchStaffList()])

  // 处理从其他页面扫码跳转
  const pendingBarcode = sessionStorage.getItem('pendingBarcode')
  if (pendingBarcode) {
    sessionStorage.removeItem('pendingBarcode')
    addProductByBarcode(pendingBarcode)
  }
})

onUnmounted(() => {
  window.removeEventListener('barcode-scanned', handleBarcodeEvent)
})

function handleBarcodeEvent(e) {
  addProductByBarcode(e.detail)
}

async function addProductByBarcode(code) {
  if (!code) return
  try {
    const existing = products.value.find(p => p.barcode === code)
    if (existing) {
      addToCart(existing)
      message.success(`已扫描：${existing.name}`)
      return
    }
    const product = await productApi.getByBarcode(code)
    if (product) {
      addToCart(product)
      products.value.unshift(product)
      message.success(`已扫描：${product.name}`)
    }
  } catch {
    // 错误已在拦截器处理
  }
}


// 判断是否为临时商品（负 id 占位，无库存限制）
function isTempItem(item) {
  return item && item.id < 0
}

function addToCart(product) {
  // 真实商品库存为 0 时不允许添加
  if (!isTempItem(product) && (product.stock || 0) <= 0) {
    message.warning(`「${product.name}」已售罄`)
    return
  }
  if (!cart.value[product.id]) {
    // 记录原价，用于标识“已改价”的临时特价商品
    cart.value[product.id] = { ...product, qty: 1, origPrice: product.price, modified: false }
  } else {
    const item = cart.value[product.id]
    // 真实商品不能超过库存
    if (!isTempItem(item) && item.qty >= (item.stock || 0)) {
      message.warning(`「${item.name}」库存不足（仅剩 ${item.stock} ${item.unit || '件'}）`)
      return
    }
    item.qty++
  }
}

function updateQty(id, delta) {
  const item = cart.value[id]
  if (!item) return
  // 真实商品不能超过库存
  if (delta > 0 && !isTempItem(item) && item.qty >= (item.stock || 0)) {
    message.warning(`「${item.name}」库存不足（仅剩 ${item.stock} ${item.unit || '件'}）`)
    return
  }
  item.qty += delta
  if (item.qty <= 0) {
    delete cart.value[id]
  }
}

function handleQtyInput(id, val) {
  const item = cart.value[id]
  if (!item) return
  if (val === null || val === undefined) return
  if (val <= 0) {
    delete cart.value[id]
  } else {
    // 真实商品不能超过库存
    if (!isTempItem(item) && val > (item.stock || 0)) {
      message.warning(`「${item.name}」库存不足（仅剩 ${item.stock} ${item.unit || '件'}）`)
      item.qty = item.stock || 0
      return
    }
    item.qty = val
  }
}

// ── 挂单/取单 ──
const heldOrders = ref([])
const heldOrdersShow = ref(false)

const HELD_KEY = 'pos_held_orders'

function loadHeldOrders() {
  try {
    heldOrders.value = JSON.parse(localStorage.getItem(HELD_KEY) || '[]')
  } catch { heldOrders.value = [] }
}

function saveHeldOrders() {
  localStorage.setItem(HELD_KEY, JSON.stringify(heldOrders.value))
}

function holdOrder() {
  if (cartItems.value.length === 0) {
    message.warning('购物车为空，无法挂单')
    return
  }
  const order = {
    id: Date.now(),
    time: new Date().toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }),
    items: JSON.parse(JSON.stringify(cartItems.value)),
    discount: discount.value,
    payment: payment.value
  }
  heldOrders.value.unshift(order)
  saveHeldOrders()
  cart.value = {}
  discount.value = 0
  message.success('已挂单，可继续服务下一位顾客')
}

function retrieveOrder(order) {
  cart.value = {}
  order.items.forEach(item => {
    cart.value[item.id] = { ...item }
  })
  discount.value = order.discount || 0
  payment.value = order.payment || 'wechat'
  heldOrders.value = heldOrders.value.filter(o => o.id !== order.id)
  saveHeldOrders()
  heldOrdersShow.value = false
  message.success('已取单，继续结算')
}

function deleteHeldOrder(order) {
  heldOrders.value = heldOrders.value.filter(o => o.id !== order.id)
  saveHeldOrders()
}

function heldTotal(order) {
  const sub = order.items.reduce((s, i) => s + i.price * i.qty, 0)
  if (order.discount > 0 && order.discount < 100) {
    return Math.round(sub * (100 - order.discount) / 100 * 100) / 100
  }
  return Math.round(sub * 100) / 100
}

// ── 临时商品（未入库，直接录入名称/单价/数量结算） ──
const tmpShow = ref(false)
const tmpForm = ref({ name: '', price: '', qty: 1 })

function openTmp() {
  tmpForm.value = { name: '', price: '', qty: 1 }
  tmpShow.value = true
}

function addTmpProduct() {
  const name = String(tmpForm.value.name || '').trim()
  const price = Number(tmpForm.value.price)
  let qty = parseInt(tmpForm.value.qty, 10)
  if (!name) { message.warning('请输入商品名称'); return }
  if (!price || price <= 0) { message.warning('请输入正确的单价'); return }
  if (!qty || qty < 1) qty = 1
  // 临时商品用负 id 占位，避免与真实商品 id 冲突
  const tmpId = -Date.now()
  cart.value[tmpId] = { id: tmpId, name, price, qty, origPrice: price, modified: false, barcode: '', spec: '', unit: '件' }
  tmpShow.value = false
  message.success(`已添加：${name}`)
}

// ── 临时改价（仅本单生效，不改商品库价格） ──
const editingId = ref(null)
const editPrice = ref(0)

function startEditPrice(item) {
  editingId.value = item.id
  editPrice.value = item.price
}

function saveEditPrice(item) {
  const v = Number(editPrice.value)
  if (!isNaN(v) && v >= 0) {
    item.price = v
    item.modified = v !== item.origPrice
  }
  editingId.value = null
}

function cancelEditPrice() {
  editingId.value = null
}

async function submitOrder() {
  const items = cartItems.value.map(i => ({
    productId: i.id,
    productName: i.name,
    price: i.price,
    quantity: i.qty,
    // 改过价才带上原价，用于订单详情标注“改价”
    originalPrice: i.modified ? i.origPrice : undefined
  }))
  loading.value = true
  try {
    await orderApi.create({ payment: payment.value, items, discount: discount.value || undefined, saleByUserId: currentStaffId.value || undefined })
    cart.value = {}
    discount.value = 0
    message.success('交易完成')
  } catch {
    // 已处理
  } finally {
    loading.value = false
  }
}

const cartItems = computed(() => Object.values(cart.value))

const totalCount = computed(() => cartItems.value.reduce((s, i) => s + i.qty, 0))
const subtotal = computed(() => Math.round(cartItems.value.reduce((s, i) => s + i.price * i.qty, 0) * 100) / 100)
const discountAmount = computed(() => {
  const d = discount.value
  if (d <= 0 || d >= 100) return 0
  return Math.round(subtotal.value * (100 - d) / 100 * 100) / 100
})
const total = computed(() => {
  if (discount.value <= 0 || discount.value >= 100) return subtotal.value
  return Math.round((subtotal.value - discountAmount.value) * 100) / 100
})
</script>

<template>
  <div class="flex gap-6 h-full w-full">
    <!-- 左边：商品列表 -->
    <n-card>
      <div class="flex-[2] flex flex-col gap-4 h-full">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-body font-bold tracking-tight">商品</h2>
          <div class="flex items-center gap-2 text-sm text-on-surface-variant dark:text-gray-400">
            <Icon icon="mdi:shopping-outline" width="16" />
            <span>已选 {{ totalCount }} 件</span>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <n-input v-model:value="searchQuery" placeholder="搜索商品..." clearable @update:value="onSearchInput" class="flex-1">
            <template #prefix>
              <Icon icon="mdi:magnify" class="text-on-surface-variant/40 dark:text-gray-500" />
            </template>
          </n-input>
          <button class="h-9 px-3 rounded-lg text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-1.5 shrink-0" @click="openTmp">
            <Icon icon="mdi:plus" width="16" />
            临时商品
          </button>
        </div>
        <div class="grid gap-2 flex-1 overflow-auto min-h-0 pr-1 content-start" style="grid-template-columns: repeat(auto-fill, minmax(180px, 1fr))">
          <button v-for="(p, idx) in products" :key="p.id"
            class="flex items-center gap-2.5 p-2.5 rounded-lg bg-surface dark:bg-[#1a1a1a] hover:bg-black/5 dark:hover:bg-white/5 min-w-0 text-left transition-colors"
            @click="addToCart(p)">
            <img v-if="p.image" :src="p.image" :alt="p.name"
              class="w-11 h-11 rounded-lg object-cover shrink-0 bg-black/5 dark:bg-white/10" />
            <div v-else class="w-11 h-11 rounded-lg shrink-0 flex items-center justify-center text-base font-bold select-none"
              :style="thumbStyle(idx)">{{ (p.name || '品').charAt(0) }}</div>
            <div class="flex-1 min-w-0">
              <div class="font-body font-semibold text-sm text-on-surface dark:text-inverse-on-surface truncate leading-tight">{{ p.name }}</div>
              <div class="text-xs text-on-surface-variant/60 dark:text-gray-500 truncate leading-tight mt-0.5">{{ p.spec || '—' }}</div>
              <div class="flex items-center justify-between mt-1">
                <span class="text-xs font-mono font-semibold text-on-surface dark:text-inverse-on-surface">¥{{ p.price }}</span>
                <span class="text-[11px] font-medium"
                  :class="p.stock <= 0 ? 'text-red-500' : p.stock <= 10 ? 'text-amber-600 dark:text-amber-400' : 'text-on-surface-variant/50 dark:text-gray-500'">
                  {{ p.stock <= 0 ? '售罄' : '库存 ' + p.stock + (p.unit || '') }}
                </span>
              </div>
            </div>
          </button>
        </div>

      </div>
    </n-card>

    <!-- 右边：购物车 + 结算 -->
    <n-card class="flex-[1]">
      <div class="min-w-[300px] max-w-[400px] flex flex-col gap-4 h-full">
        <div class="flex items-center justify-between">
          <h3
            class="text-sm font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-400">
            购物车</h3>
          <div class="flex items-center gap-2">
            <n-select
              v-model:value="currentStaffId"
              :options="staffList"
              size="small"
              placeholder="销售员"
              style="width:110px"
            />
            <button v-if="cartItems.length > 0"
              class="text-xs px-2 py-1 rounded-md text-amber-700 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30 font-body font-medium"
              @click="holdOrder">
              挂单
            </button>
            <button
              class="text-xs px-2 py-1 rounded-md text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 font-body font-medium relative"
              @click="heldOrdersShow = true">
              取单
              <span v-if="heldOrders.length > 0" class="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-amber-500 text-white text-[10px] flex items-center justify-center leading-none">{{ heldOrders.length }}</span>
            </button>
            <button v-if="cartItems.length > 0"
              class="text-xs px-2 py-1 text-on-surface-variant/50 dark:text-gray-500 hover:text-red-500 dark:hover:text-red-400 font-body"
              @click="cart = {}">
              清空
            </button>
          </div>
        </div>

        <!-- 已选商品列表 -->
        <div class="flex-1 flex flex-col gap-2 overflow-auto min-h-0 pr-1">
          <div v-for="item in cartItems" :key="item.id"
            class="flex items-center justify-between py-3 px-4 rounded-lg bg-surface dark:bg-[#1a1a1a]">
            <div class="min-w-0 mr-2">
              <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface truncate">{{
                item.name }}</div>
              <div class="flex items-center gap-1 text-xs text-on-surface-variant dark:text-gray-400 mt-1">
                <template v-if="editingId === item.id">
                  <n-input-number
                    v-model:value="editPrice"
                    size="tiny"
                    :show-button="false"
                    :min="0"
                    :step="0.5"
                    style="width:76px"
                    placeholder="0"
                    @keydown.enter="saveEditPrice(item)"
                  />
                  <button class="p-0.5 rounded text-emerald-600 hover:bg-emerald-500/10" title="确认" @click="saveEditPrice(item)">
                    <Icon icon="mdi:check" width="14" />
                  </button>
                  <button class="p-0.5 rounded text-on-surface-variant/50 hover:bg-black/10" title="取消" @click="cancelEditPrice">
                    <Icon icon="mdi:close" width="14" />
                  </button>
                </template>
                <template v-else>
                  <span :class="item.modified ? 'text-red-600 dark:text-red-400 font-semibold' : ''">¥{{ item.price }}</span>
                  <span v-if="item.modified"
                    class="px-1 py-px rounded text-[10px] font-semibold bg-red-100 dark:bg-red-950/40 text-red-600 dark:text-red-400">改</span>
                  <button
                    class="w-6 h-6 inline-flex items-center justify-center rounded-md bg-black/5 dark:bg-white/10 text-on-surface-variant dark:text-gray-300 hover:bg-black/15 dark:hover:bg-white/20"
                    title="改价（仅本单）" @click="startEditPrice(item)">
                    <Icon icon="mdi:pencil-outline" width="14" />
                  </button>
                </template>
              </div>
              <div v-if="discount > 0 && discount < 100" class="text-xs text-red-500 mt-1">
                折后价 ¥{{ Math.round(item.price * discount / 100 * 100) / 100 }}
              </div>
            </div>
            <div class="flex items-center gap-1 shrink-0">
              <button
                class="w-7 h-7 rounded-lg bg-black/10 dark:bg-white/10 flex items-center justify-center hover:bg-black/20 dark:hover:bg-white/20"
                @click="updateQty(item.id, -1)">
                <Icon icon="mdi:minus" width="14" />
              </button>
              <n-input-number
                :value="item.qty"
                size="small"
                :min="0"
                :max="item.id < 0 ? undefined : item.stock"
                style="width:56px"
                :show-button="false"
                @update:value="(val) => handleQtyInput(item.id, val)"
              />
              <button
                class="w-7 h-7 rounded-lg bg-black/10 dark:bg-white/10 flex items-center justify-center hover:bg-black/20 dark:hover:bg-white/20"
                @click="updateQty(item.id, 1)">
                <Icon icon="mdi:plus" width="14" />
              </button>
            </div>
          </div>
          <div v-if="cartItems.length === 0"
            class="flex-1 flex items-center justify-center text-sm text-on-surface-variant/50 dark:text-gray-500">
            点击左侧商品添加
          </div>
        </div>

        <!-- 支付方式 -->
        <div class="flex gap-2">
          <button v-for="pay in [
            { key: 'wechat', icon: 'simple-icons:wechat', label: '微信', color: '#07c160' },
            { key: 'alipay', icon: 'simple-icons:alipay', label: '支付宝', color: '#1677ff' },
            { key: 'cash', icon: 'mdi:cash', label: '现金', color: '#666' }
          ]" :key="pay.key"
            class="flex-1 py-2 rounded-lg text-xs font-body font-semibold flex items-center justify-center gap-1 transition-all"
            :class="payment === pay.key
              ? 'bg-black dark:bg-white text-white dark:text-black'
              : 'bg-surface dark:bg-[#1a1a1a] text-on-surface-variant dark:text-gray-400'" @click="payment = pay.key">
            <Icon :icon="pay.icon" width="14" :style="pay.key === 'cash' && payment === pay.key ? {} : { color: pay.color }" />
            <span>{{ pay.label }}</span>
          </button>
        </div>

        <!-- 折扣 -->
        <div class="flex items-center gap-2">
          <label class="text-xs text-on-surface-variant dark:text-gray-400 shrink-0 font-body">打折</label>
          <div class="flex-1 flex items-center gap-1">
            <n-input-number v-model:value="discount" :min="0" :max="100" :step="5" size="small" class="flex-1" placeholder="0" />
            <span class="text-xs text-on-surface-variant dark:text-gray-400">%</span>
          </div>
          <div v-if="discount > 0" class="text-xs text-red-500 font-medium">
            -¥{{ discountAmount }}
          </div>
        </div>

        <!-- 合计 -->
        <div class="border-t border-outline-variant/50 dark:border-[#444] pt-4 flex flex-col gap-3">
          <div class="flex justify-between text-sm font-body">
            <span class="text-on-surface-variant dark:text-gray-400">小计</span>
            <span class="text-on-surface dark:text-inverse-on-surface">¥{{ subtotal }}</span>
          </div>
          <div v-if="discount > 0" class="flex justify-between text-sm font-body">
            <span class="text-on-surface-variant dark:text-gray-400">折扣 ({{ discount }}%)</span>
            <span class="text-red-500">-¥{{ discountAmount }}</span>
          </div>
          <div
            class="flex justify-between text-lg font-body font-bold border-t border-outline-variant/50 dark:border-[#444] pt-3">
            <span class="text-on-surface dark:text-inverse-on-surface">总计</span>
            <span class="text-on-surface dark:text-inverse-on-surface">¥{{ total }}</span>
          </div>
        </div>

        <button
          class="w-full py-3.5 rounded-xl bg-black dark:bg-white text-white dark:text-black font-body font-semibold text-sm uppercase tracking-widest hover:opacity-80 disabled:opacity-40 disabled:hover:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          :disabled="cartItems.length === 0 || loading" @click="submitOrder">
          <Icon v-if="loading" icon="mdi:loading" width="16" class="animate-spin" />
          {{ loading ? '提交中...' : '收款 ¥' + total }}
        </button>
      </div>
    </n-card>

    <!-- 添加临时商品弹窗 -->
    <n-modal :show="tmpShow" :mask-closable="true" transform-origin="center"
      :on-update:show="(v) => { if (!v) tmpShow = false }">
      <div class="relative w-[380px] rounded-2xl bg-surface dark:bg-[#252525] p-6 shadow-xl border border-outline-variant/20 dark:border-[#333]">
        <h3 class="text-base font-body font-bold text-on-surface dark:text-inverse-on-surface">添加临时商品</h3>
        <p class="mt-1 text-sm font-body text-on-surface-variant dark:text-gray-400">未录入商品库的商品，仅本单结算生效</p>
        <div class="mt-4 flex flex-col gap-4">
          <div>
            <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">商品名称</label>
            <n-input v-model:value="tmpForm.name" placeholder="请输入商品名称" clearable @keyup.enter="addTmpProduct" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">单价（元）</label>
              <n-input-number v-model:value="tmpForm.price" placeholder="0" :min="0" clearable style="width:100%" />
            </div>
            <div>
              <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">数量</label>
              <n-input-number v-model:value="tmpForm.qty" :min="1" clearable style="width:100%" />
            </div>
          </div>
        </div>
        <div class="flex items-center justify-end gap-2 mt-6">
          <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" @click="tmpShow = false">取消</button>
          <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity" @click="addTmpProduct">添加</button>
        </div>
      </div>
    </n-modal>

    <!-- 挂单列表弹窗 -->
    <n-modal :show="heldOrdersShow" :mask-closable="true" transform-origin="center"
      :on-update:show="(v) => { if (!v) heldOrdersShow = false }">
      <div class="relative w-[460px] rounded-2xl bg-surface dark:bg-[#252525] p-6 shadow-xl border border-outline-variant/20 dark:border-[#333]">
        <h3 class="text-base font-body font-bold text-on-surface dark:text-inverse-on-surface mb-4">挂单列表</h3>
        <div v-if="heldOrders.length === 0" class="py-12 text-center text-sm text-on-surface-variant/50 dark:text-gray-500">
          暂无挂单
        </div>
        <div v-else class="flex flex-col gap-3 max-h-[400px] overflow-auto">
          <div v-for="order in heldOrders" :key="order.id"
            class="flex items-center justify-between p-3 rounded-lg bg-black/5 dark:bg-white/5">
            <div class="min-w-0 flex-1">
              <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface truncate">
                {{ order.items.map(i => i.name).join('、') }}
              </div>
              <div class="flex items-center gap-3 text-xs text-on-surface-variant dark:text-gray-400 mt-1">
                <span>{{ order.time }}</span>
                <span>{{ order.items.reduce((s, i) => s + i.qty, 0) }} 件</span>
                <span class="font-mono font-semibold">¥{{ heldTotal(order) }}</span>
              </div>
            </div>
            <div class="flex items-center gap-2 shrink-0 ml-3">
              <button class="px-3 py-1.5 rounded-md text-xs font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80"
                @click="retrieveOrder(order)">取单</button>
              <button class="w-7 h-7 flex items-center justify-center rounded-md text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30"
                @click="deleteHeldOrder(order)">
                <Icon icon="mdi:delete-outline" width="16" />
              </button>
            </div>
          </div>
        </div>
        <div class="flex justify-end mt-4">
          <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
            @click="heldOrdersShow = false">关闭</button>
        </div>
      </div>
    </n-modal>
  </div>
</template>
