<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import JsBarcode from 'jsbarcode'
import { productApi } from '../api'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const router = useRouter()
const message = useMessage()

const products = ref([])

const lowStockThreshold = 10

async function fetchProducts() {
  try {
    products.value = await productApi.list()
  } catch { }
}

// ── 确认弹窗 ──
const confirmShow = ref(false)
const confirmTitle = ref('')
const confirmContent = ref('')
let confirmCallback = null

function useConfirm(title, content, callback) {
  confirmTitle.value = title
  confirmContent.value = content
  confirmCallback = callback
  confirmShow.value = true
}

function onConfirmOk() {
  confirmShow.value = false
  confirmCallback?.()
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
  if (!product || !stockQuantity.value || stockQuantity.value < 1) return
  const delta = stockDialogMode.value === 'in' ? stockQuantity.value : -stockQuantity.value
  try {
    await productApi.adjustStock(product.id, delta)
    const label = stockDialogMode.value === 'in' ? '入库' : '出库'
    message.success(`${label}成功：${product.name} × ${stockQuantity.value}`)
    stockDialogShow.value = false
    await fetchProducts()
  } catch {
    stockDialogShow.value = false
  }
}

function handleDelete(product) {
  useConfirm('确认删除', '确定要删除商品「' + product.name + '」吗？此操作不可撤销。', async () => {
    try {
      await productApi.delete(product.id)
      message.success('商品已删除')
      await fetchProducts()
    } catch {
    }
  })
}

onMounted(() => {
  fetchProducts()
})

function formatExpiry(expiry) {
  if (!expiry) return ''
  if (expiry.includes('-')) return `到期 ${expiry}`
  const match = expiry.match(/^(\d+)([DMY])$/)
  if (!match) return expiry
  const n = match[1], t = { D: '天', M: '个月', Y: '年' }[match[2]]
  return `${n}${t}`
}

const totalProducts = computed(() => products.value.length)

const lowStockCount = computed(() =>
  products.value.filter(p => p.stock < lowStockThreshold).length
)

const totalValue = computed(() =>
  isCashier ? 0 : products.value.reduce((s, p) => s + (p.purchasePrice || p.price) * p.stock, 0)
)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  pageSizes: [5, 10, 20, 50]
})

const paginatedProducts = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return products.value.slice(start, start + pagination.pageSize)
})

const isCashier = localStorage.getItem('userRole') === 'cashier'

const productColumns = computed(() => {
  const cols = [
    { title: '条码', key: 'barcode', minWidth: 120 },
    { title: '商品名称', key: 'name', minWidth: 150 },
    { title: '规格型号', key: 'spec', minWidth: 120 },
    { title: '存放位置', key: 'location', minWidth: 100 },
    { title: '有效期', key: 'expiry', minWidth: 120 },
    {
      title: '销售价', key: 'price', className: 'text-right', minWidth: 90,
      render(row) { return h('span', '¥' + row.price) }
    }
  ]
  if (!isCashier) {
    cols.push({
      title: '进货价', key: 'purchasePrice', className: 'text-right', minWidth: 90,
      render(row) { return h('span', row.purchasePrice ? '¥' + row.purchasePrice : '-') }
    })
  }
  cols.push({
    title: '库存', key: 'stock', className: 'text-right', minWidth: 90,
    render(row) {
      const isLow = row.stock < lowStockThreshold
      return h('span', {
        class: isLow ? 'text-red-600 dark:text-red-400' : ''
      }, String(row.stock) + ' ' + (row.unit || ''))
    }
  })
  if (!isCashier) {
    cols.push({
      title: '小计', key: 'subtotal', className: 'text-right', minWidth: 100,
      render(row) { return h('span', '¥' + ((row.purchasePrice || row.price) * row.stock).toLocaleString()) }
    })
  }
  cols.push({
    title: '操作', key: 'actions', width: 260, fixed: 'right',
    render(row) {
      return h('div', { class: 'inline-flex items-center gap-0.5' }, [
        h('button', {
          class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10',
          title: '入库',
          onClick: () => openStockDialog(row, 'in')
        }, [
          h(Icon, { icon: 'mdi:plus-circle-outline', width: 14 }),
          '入库'
        ]),
        h('button', {
          class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-500/10',
          title: '出库',
          onClick: () => openStockDialog(row, 'out')
        }, [
          h(Icon, { icon: 'mdi:minus-circle-outline', width: 14 }),
          '出库'
        ]),
        h('button', {
          class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/10 dark:hover:bg-white/10',
          onClick: () => router.push('/products/edit/' + row.id)
        }, [
          h(Icon, { icon: 'mdi:pencil-outline', width: 14 }),
          '编辑'
        ]),
        h('button', {
          class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-red-500 hover:bg-red-500/10',
          onClick: () => handleDelete(row)
        }, [
          h(Icon, { icon: 'mdi:delete-outline', width: 14 }),
          '删除'
        ])
      ])
    }
  })
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
  <div class="flex-1 flex flex-col">
    <div class="flex-1 flex flex-col gap-6">
      <!-- 统计卡片 -->
      <div class="flex gap-4">
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
      <n-card  style="flex:1;display:flex;flex-direction:column" content-style="flex:1;display:flex;flex-direction:column">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-sm font-body font-semibold uppercase tracking-wider text-on-surface-variant">商品清单</span>
            <div class="flex items-center gap-2">
              <button class="h-8 px-3.5 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors flex items-center gap-1.5" @click="showPrintModal = true">
                <Icon icon="mdi:printer-outline" width="14" />打印标签
              </button>
              <button v-if="!isCashier" class="h-8 px-3.5 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-1.5" @click="$router.push('/products/add')">
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
        <div v-else>
          <n-data-table :bordered="false" :columns="productColumns" :data="paginatedProducts" size="small" scroll-x="1200" />
        </div>
      </n-card>

      <!-- 分页 -->
      <div v-if="products.length > 0" class="flex justify-end pt-2">
        <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize" :item-count="products.length" :page-sizes="pagination.pageSizes" show-size-picker>
          <template #prefix>
            <span class="text-xs text-on-surface-variant">共 {{ products.length }} 条</span>
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
      :title="confirmTitle"
      :content="confirmContent"
      confirm-text="确定删除"
      @update:show="confirmShow = $event"
      @confirm="onConfirmOk"
    />

    <!-- 库存调整弹窗 -->
    <Transition name="stock">
      <div v-if="stockDialogShow" class="fixed inset-0 z-[200] flex items-center justify-center" @click.self="closeStockDialog">
        <div class="fixed inset-0 bg-black/30 dark:bg-black/50" />
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
      </div>
    </Transition>
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

/* 库存弹窗动画 */
.stock-enter-active {
  transition: opacity 200ms ease;
}
.stock-leave-active {
  transition: opacity 150ms ease;
}
.stock-enter-from,
.stock-leave-to {
  opacity: 0;
}
.stock-enter-active > div:last-child {
  transition: transform 200ms cubic-bezier(0.34, 1.56, 0.64, 1), opacity 200ms ease;
}
.stock-leave-active > div:last-child {
  transition: transform 150ms ease, opacity 150ms ease;
}
.stock-enter-from > div:last-child {
  transform: scale(0.92);
  opacity: 0;
}
.stock-leave-to > div:last-child {
  transform: scale(0.92);
  opacity: 0;
}
</style>
