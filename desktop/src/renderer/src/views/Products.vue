<script setup>
import { ref, reactive, computed, h, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, NImage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import JsBarcode from 'jsbarcode'
import { productApi } from '../api'
import { hasPermission, loadMenus } from '../composables/permission'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const router = useRouter()
const message = useMessage()

const products = ref([])
const total = ref(0)
const searchLoading = ref(false)
const searchQuery = ref('')
let searchTimer = null

const lowStockThreshold = 10

// 分页状态（须在 watch/searchProducts 引用前声明，避免 TDZ）
const pagination = reactive({
  page: 1,
  pageSize: 10,
  pageSizes: [5, 10, 20, 50]
})

async function searchProducts() {
  searchLoading.value = true
  try {
    const res = await productApi.search(searchQuery.value.trim(), pagination.page, pagination.pageSize)
    products.value = res.records
    total.value = res.total
  } catch { }
  finally { searchLoading.value = false }
}

function onSearchInput() {
  clearTimeout(searchTimer)
  pagination.page = 1
  searchTimer = setTimeout(searchProducts, 300)
}

// ── 确认弹窗 ──
const confirmShow = ref(false)
const confirmLoading = ref(false)
const confirmTitle = ref('')
const confirmContent = ref('')
let confirmCallback = null

function useConfirm(title, content, callback) {
  confirmTitle.value = title
  confirmContent.value = content
  confirmCallback = callback
  confirmShow.value = true
}

async function onConfirmOk() {
  if (confirmLoading.value) return
  confirmLoading.value = true
  try {
    const ret = confirmCallback?.()
    if (ret && typeof ret.then === 'function') await ret
  } finally {
    confirmLoading.value = false
    confirmShow.value = false
  }
}

// ── 库存调整弹窗 ──
const stockDialogShow = ref(false)
const stockDialogProduct = ref(null)
const stockDialogMode = ref('in') // 'in' | 'out'
const stockQuantity = ref(1)

function openStockDialog(product, mode) {
  stockDialogProduct.value = product
  stockDialogMode.value = mode
  stockQuantity.value = 1
  stockDialogShow.value = true
}

function closeStockDialog() {
  stockDialogShow.value = false
  stockDialogProduct.value = null
}

async function confirmStockAdjust() {
  const product = stockDialogProduct.value
  const qty = stockQuantity.value
  if (!product || !qty || qty < 1) return
  try {
    if (stockDialogMode.value === 'in') {
      await productApi.stockIn(product.id, qty)
    } else {
      await productApi.stockOut(product.id, qty)
    }
    const label = stockDialogMode.value === 'in' ? '入库' : '出库'
    message.success(`${label}成功：${product.name} × ${qty}`)
    stockDialogShow.value = false
    await searchProducts()
  } catch {
    stockDialogShow.value = false
  }
}

function handleDelete(product) {
  useConfirm('确认删除', '确定要删除商品「' + product.name + '」吗？此操作不可撤销。', async () => {
    try {
      await productApi.delete(product.id)
      message.success('商品已删除')
      await searchProducts()
    } catch {
    }
  })
}

// 分页变化时重新搜索
watch(() => [pagination.page, pagination.pageSize], searchProducts)

onMounted(() => {
  // 加载当前用户按钮权限（MainLayout 已加载时会直接命中缓存）
  loadMenus()
  searchProducts()
})

function formatExpiry(expiry) {
  if (!expiry) return ''
  if (expiry.includes('-')) return `到期 ${expiry}`
  const match = expiry.match(/^(\d+)([DMY])$/)
  if (!match) return expiry
  const n = match[1], t = { D: '天', M: '个月', Y: '年' }[match[2]]
  return `${n}${t}`
}

const totalProducts = computed(() => total.value)

const lowStockCount = computed(() => 0) // 服务端分页后，低库存数由后端提供

const totalValue = computed(() => 0) // 暂不支持

// 仅用于「进货价 / 小计 / 库存总价」等字段级可见性；页面操作按钮一律走菜单按钮权限（hasPermission）
const isCashier = localStorage.getItem('userRole') === 'cashier'

const productColumns = computed(() => {
  const cols = [
    {
      title: '商品图', key: 'image', width: 70, align: 'center',
      render(row) {
        if (!row.image) return h('div', { class: 'w-8 h-8 mx-auto rounded bg-black/5 dark:bg-white/10' })
        return h(NImage, {
          src: row.image,
          alt: row.name,
          width: 32,
          height: 32,
          objectFit: 'cover',
          class: 'rounded mx-auto',
          fallbackSrc: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" fill="%23f4f4f5"/></svg>'
        })
      }
    },
    { title: '条码', key: 'barcode', minWidth: 180 },
    { title: '商品名称', key: 'name', minWidth: 150 },
    { title: '规格型号', key: 'spec', minWidth: 120 },
    { title: '存放位置', key: 'location', minWidth: 100 },
    { title: '有效期', key: 'expiry', minWidth: 120 },
    {
      title: '销售价', key: 'price', minWidth: 90,
      render(row) { return h('span', '¥' + row.price) }
    }
  ]
  if (!isCashier) {
    cols.push({
      title: '进货价', key: 'purchasePrice', minWidth: 90,
      render(row) { return h('span', row.purchasePrice ? '¥' + row.purchasePrice : '-') }
    })
  }
  cols.push({
    title: '库存', key: 'stock', minWidth: 90,
    render(row) {
      const isLow = row.stock < lowStockThreshold
      return h('span', {
        class: isLow ? 'text-red-600 dark:text-red-400' : ''
      }, String(row.stock) + ' ' + (row.unit || ''))
    }
  })
  if (!isCashier) {
    cols.push({
      title: '小计', key: 'subtotal', minWidth: 100,
      render(row) { return h('span', '¥' + ((row.purchasePrice || row.price) * row.stock).toLocaleString()) }
    })
  }
  // 操作列按钮均受菜单按钮权限控制（type=3），全部无权限时不显示该列
  const actionDefs = [
    { perm: 'products:stock-in', title: '入库', icon: 'mdi:plus-circle-outline', cls: 'text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10', text: '入库', action: (row) => openStockDialog(row, 'in') },
    { perm: 'products:stock-out', title: '出库', icon: 'mdi:minus-circle-outline', cls: 'text-amber-600 dark:text-amber-400 hover:bg-amber-500/10', text: '出库', action: (row) => openStockDialog(row, 'out') },
    { perm: 'products:edit', title: '编辑', icon: 'mdi:pencil-outline', cls: 'text-on-surface-variant dark:text-gray-400 hover:bg-black/10 dark:hover:bg-white/10', text: '编辑', action: (row) => router.push('/products/edit/' + row.id) },
    { perm: 'products:delete', title: '删除', icon: 'mdi:delete-outline', cls: 'text-red-500 hover:bg-red-500/10', text: '删除', action: (row) => handleDelete(row) }
  ]
  const visibleActions = actionDefs.filter(a => hasPermission(a.perm))
  if (visibleActions.length > 0) {
    cols.push({
      title: '操作', key: 'actions', width: visibleActions.length * 62 + 8, fixed: 'right',
      render(row) {
        return h('div', { class: 'inline-flex items-center gap-0.5' }, visibleActions.map(a =>
          h('button', {
            class: `inline-flex items-center gap-0.5 px-2 py-1 rounded-lg text-xs font-semibold ${a.cls}`,
            title: a.title,
            onClick: () => a.action(row)
          }, [h(Icon, { icon: a.icon, width: 14 }), a.text])
        ))
      }
    })
  }
  return cols
})

// ── 打印标签 ──
const showPrintModal = ref(false)
const printSelectedIds = ref([])

const allSelected = computed(() => printSelectedIds.value.length === products.value.length)

function togglePrint(id) {
  const idx = printSelectedIds.value.indexOf(id)
  if (idx >= 0) printSelectedIds.value.splice(idx, 1)
  else printSelectedIds.value.push(id)
}

function selectAllPrint() {
  if (printSelectedIds.value.length === products.value.length) {
    printSelectedIds.value = []
  } else {
    printSelectedIds.value = products.value.map(p => p.id)
  }
}

function doPrint() {
  const selected = products.value.filter(p => printSelectedIds.value.includes(p.id))
  if (selected.length === 0) return

  const labels = selected.map(p => {
    let barcodeSvg = ''
    if (p.barcode) {
      const tmp = document.createElement('svg')
      JsBarcode(tmp, p.barcode, { format: 'CODE128', width: 1.5, height: 30, displayValue: true, fontSize: 10, margin: 4 })
      barcodeSvg = tmp.outerHTML
    }
    return { name: p.name, price: p.price, barcodeSvg, barcode: p.barcode }
  })

  const labelHTML = labels.map(p => `
    <div class="label-card">
      <h2>${p.name}</h2>
      <p class="price">¥${p.price}</p>
      ${p.barcodeSvg}
    </div>
  `).join('')

  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;top:0;left:0;width:0;height:0;border:none;opacity:0'
  document.body.appendChild(iframe)

  const w = 60, h = 40 // 标签尺寸 mm
  iframe.contentWindow.document.write(`
    <html><head><style>
      @page { size: ${w}mm ${h}mm; margin:0; }
      body { margin:4mm; display:flex; flex-wrap:wrap; gap:4mm; justify-content:center; font-family:sans-serif; }
      .label-card {
        width:${w - 8}mm; height:${h - 8}mm;
        border:1.5px solid #000;
        border-radius:2mm;
        display:flex; flex-direction:column;
        justify-content:center; align-items:center;
        text-align:center;
        page-break-inside:avoid;
        box-sizing:border-box;
        padding:2mm;
      }
      h2 { margin:0 0 1mm; font-size:3.5mm; font-weight:700; }
      .price { margin:0 0 1mm; font-size:3mm; color:#555; }
      svg { max-width:90%; height:auto; }
    </style></head><body>${labelHTML}</body></html>
  `)
  iframe.contentWindow.document.close()
  iframe.contentWindow.focus()

  setTimeout(() => {
    iframe.contentWindow.print()
    setTimeout(() => document.body.removeChild(iframe), 500)
  }, 300)

  showPrintModal.value = false
}
</script>

<template>
  <div class="flex-1 min-h-0 flex flex-col">
    <div class="flex-1 min-h-0 flex flex-col gap-6">
      <!-- 统计卡片 -->
      <div class="flex gap-4 shrink-0">
        <n-card style="flex:1">
          <div class="flex items-center gap-4">
            <div class="w-11 h-11 rounded-xl bg-black/10 dark:bg-white/10 flex items-center justify-center shrink-0">
              <Icon icon="mdi:package-variant-closed" width="20" class="text-on-surface dark:text-inverse-on-surface" />
            </div>
            <div>
              <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body">总商品</div>
              <div class="text-xl font-body font-bold tracking-tight">{{ totalProducts }} 种</div>
            </div>
          </div>
        </n-card>
        <n-card  style="flex:1">
          <div class="flex items-center gap-4">
            <div class="w-11 h-11 rounded-xl bg-red-100 dark:bg-red-950/30 flex items-center justify-center shrink-0">
              <Icon icon="mdi:alert-circle-outline" width="20" class="text-red-600 dark:text-red-400" />
            </div>
            <div>
              <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body">低库存预警</div>
              <div class="text-xl font-body font-bold tracking-tight text-red-600 dark:text-red-400">{{ lowStockCount }} 项</div>
            </div>
          </div>
        </n-card>
        <n-card v-if="!isCashier"  style="flex:1">
          <div class="flex items-center gap-4">
            <div class="w-11 h-11 rounded-xl bg-emerald-100 dark:bg-emerald-950/30 flex items-center justify-center shrink-0">
              <Icon icon="mdi:currency-cny" width="20" class="text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body">库存总价</div>
              <div class="text-xl font-body font-bold tracking-tight">¥{{ totalValue.toLocaleString() }}</div>
            </div>
          </div>
        </n-card>
      </div>

      <!-- 商品列表 -->
      <n-card class="flex-1 min-h-0 flex flex-col" content-style="flex:1;display:flex;flex-direction:column;min-height:0">
        <template #header>
          <div class="flex items-center justify-between gap-4">
            <n-input v-model:value="searchQuery" placeholder="搜索商品名称、条码、规格..." clearable style="max-width:320px" @update:value="onSearchInput">
              <template #prefix>
                <Icon icon="mdi:magnify" class="text-on-surface-variant/40 dark:text-gray-500" />
              </template>
            </n-input>
            <div class="flex items-center gap-2 shrink-0">
              <button v-if="hasPermission('products:print')" class="h-8 px-3.5 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors flex items-center gap-1.5" @click="showPrintModal = true">
                <Icon icon="mdi:printer-outline" width="14" />打印标签
              </button>
              <button v-if="hasPermission('products:add')" class="h-8 px-3.5 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-1.5" @click="$router.push('/products/add')">
                <Icon icon="mdi:plus" width="14" />新增
              </button>
            </div>
          </div>
        </template>
        <!-- 空状态 -->
        <div v-if="products.length === 0"
          class="flex-1 flex flex-col items-center justify-center gap-3 text-on-surface-variant/50 dark:text-gray-500 py-8">
          <Icon icon="mdi:package-variant-closed" width="48" class="opacity-40" />
          <span class="text-sm font-body">暂无商品</span>
        </div>
        <div v-else class="flex-1 min-h-0">
          <n-data-table flex-height :bordered="false" :columns="productColumns" :data="products" size="small" scroll-x="1800"
            :loading="searchLoading" style="height:100%" />
        </div>
      </n-card>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex justify-end pt-2 shrink-0">
        <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize" :item-count="total" :page-sizes="pagination.pageSizes" show-size-picker>
          <template #prefix>
            <span class="text-xs text-on-surface-variant">共 {{ total }} 条</span>
          </template>
        </n-pagination>
      </div>
    </div>

    <!-- 打印标签弹窗 -->
    <n-drawer v-model:show="showPrintModal" :width="420" placement="right">
      <n-drawer-content title="选择打印商品">
        <template #header-extra>
          <span class="text-xs text-on-surface-variant">已选 {{ printSelectedIds.length }} / {{ products.length }}</span>
        </template>

        <div class="flex items-center gap-2 pb-3">
          <label class="flex items-center gap-2 cursor-pointer text-xs font-body font-semibold">
            <input type="checkbox" :checked="allSelected" @change="selectAllPrint" class="sr-only" />
            <div class="w-[18px] h-[18px] rounded-md border-2 flex items-center justify-center transition-all" :class="allSelected ? 'bg-black dark:bg-white border-black dark:border-white' : 'border-outline-variant dark:border-[#555]'">
              <Icon v-if="allSelected" icon="mdi:check" width="14" class="text-white dark:text-black" />
            </div>
            全选
          </label>
        </div>

        <div class="flex flex-col gap-1">
          <div v-for="p in products" :key="p.id" class="flex items-center gap-3 py-2.5 px-3 rounded-xl hover:bg-black/5 dark:hover:bg-white/5 cursor-pointer" @click="togglePrint(p.id)">
            <input type="checkbox" :checked="printSelectedIds.includes(p.id)" class="sr-only" @click.stop="togglePrint(p.id)" />
            <div class="w-[18px] h-[18px] rounded-md border-2 flex items-center justify-center transition-all shrink-0" :class="printSelectedIds.includes(p.id) ? 'bg-black dark:bg-white border-black dark:border-white' : 'border-outline-variant dark:border-[#555]'">
              <Icon v-if="printSelectedIds.includes(p.id)" icon="mdi:check" width="14" class="text-white dark:text-black" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-body font-semibold">{{ p.name }}</div>
              <div class="text-xs text-on-surface-variant font-body">¥{{ p.price }} / {{ p.unit }}</div>
              <div v-if="!isCashier && p.purchasePrice" class="text-[10px] text-on-surface-variant/60">进货价 ¥{{ p.purchasePrice }}</div>
            </div>
            <div v-if="p.barcode" class="text-[10px] font-mono text-on-surface-variant/50">{{ p.barcode }}</div>
          </div>
        </div>

        <template #footer>
          <div class="flex items-center justify-end gap-3">
            <n-button size="small" secondary @click="showPrintModal = false">取消</n-button>
            <n-button size="small" type="primary" :disabled="printSelectedIds.length === 0" @click="doPrint">打印（{{ printSelectedIds.length }}）</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <ConfirmDialog
      :show="confirmShow"
      :loading="confirmLoading"
      :title="confirmTitle"
      :content="confirmContent"
      confirm-text="确定删除"
      type="error"
      icon-type="warning"
      @update:show="confirmShow = $event"
      @confirm="onConfirmOk"
    />

    <!-- 库存调整弹窗 -->
    <n-modal :show="stockDialogShow" :mask-closable="true" transform-origin="center"
      :on-update:show="(v) => { if (!v) closeStockDialog() }">
      <div class="relative w-[340px] rounded-2xl bg-surface dark:bg-[#252525] p-6 shadow-xl border border-outline-variant/20 dark:border-[#333]">
        <h3 class="text-base font-body font-bold text-on-surface dark:text-inverse-on-surface">
          {{ stockDialogMode === 'in' ? '入库' : '出库' }}
        </h3>
        <p class="mt-1.5 text-sm font-body text-on-surface-variant dark:text-gray-400">
          {{ stockDialogProduct?.name }}（当前库存：{{ stockDialogProduct?.stock }}）
        </p>
        <div class="mt-4">
          <n-input-number v-model:value="stockQuantity" :min="1" style="width:100%" />
        </div>
        <div class="flex items-center justify-end gap-2 mt-6">
          <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" @click="closeStockDialog">取消</button>
          <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity" :disabled="!stockQuantity || stockQuantity < 1" @click="confirmStockAdjust">确认</button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<style scoped>
.dropdown-enter-active {
  transition: opacity 120ms ease, transform 120ms ease;
}

.dropdown-leave-active {
  transition: opacity 80ms ease, transform 80ms ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
