<script setup>
import { ref, reactive, computed, h, onMounted, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { orderApi, systemUserApi } from '../api'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const message = useMessage()

const orders = ref([])
const total = ref(0)
const loading = ref(false)
const searchQuery = ref('')
let searchTimer = null
const paymentLabels = { wechat: '微信', alipay: '支付宝', cash: '现金' }
const paymentIcons = { wechat: 'simple-icons:wechat', alipay: 'simple-icons:alipay', cash: 'mdi:cash' }

// ── 筛选条件：时间（本日/本月/本季度/本年/自定义）+ 卖出人 ──
const timePresets = [
  { key: 'all', label: '全部' },
  { key: 'today', label: '本日' },
  { key: 'month', label: '本月' },
  { key: 'quarter', label: '本季度' },
  { key: 'year', label: '本年' }
]
const timeRange = ref('all') // all | today | month | quarter | year | custom
const customRange = ref(null) // [startMs, endMs] | null
const sellerId = ref(null)
const staffOptions = ref([])

const userMap = computed(() => {
  const m = {}
  staffOptions.value.forEach(u => { m[u.value] = u.label })
  return m
})

/** 当前高亮的时间键（custom 且已选区间才算自定义） */
const activeTimeKey = computed(() =>
  timeRange.value === 'custom' && customRange.value ? 'custom' : timeRange.value
)

/** 是否有生效中的筛选条件（控制"重置"显隐） */
const hasFilter = computed(() => timeRange.value !== 'all' || !!sellerId.value)

function pad(n) { return n < 10 ? '0' + n : '' + n }
function fmtDate(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function fmtDateTime(d) {
  return `${fmtDate(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 按快捷项计算 [start, end] 本地时间；all 返回 null 表示不限 */
function presetRange(key) {
  if (key === 'all') return null
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  let start, end
  if (key === 'today') {
    start = new Date(y, m, now.getDate(), 0, 0, 0)
    end = new Date(y, m, now.getDate(), 23, 59, 59)
  } else if (key === 'month') {
    start = new Date(y, m, 1, 0, 0, 0)
    end = new Date(y, m + 1, 0, 23, 59, 59)
  } else if (key === 'quarter') {
    const qm = Math.floor(m / 3) * 3
    start = new Date(y, qm, 1, 0, 0, 0)
    end = new Date(y, qm + 3, 0, 23, 59, 59)
  } else if (key === 'year') {
    start = new Date(y, 0, 1, 0, 0, 0)
    end = new Date(y, 11, 31, 23, 59, 59)
  } else {
    return null
  }
  return { start, end }
}

function currentRange() {
  if (timeRange.value === 'custom' && Array.isArray(customRange.value) && customRange.value.length === 2) {
    return { start: new Date(customRange.value[0]), end: new Date(customRange.value[1]) }
  }
  return presetRange(timeRange.value)
}

function onTimePreset(key) {
  timeRange.value = key
  if (key !== 'custom') customRange.value = null
  if (key !== 'custom') applyFilterChange()
}

function onCustomRangeChange(val) {
  customRange.value = val || null
  timeRange.value = val ? 'custom' : 'all'
  applyFilterChange()
}

function onSellerChange() {
  applyFilterChange()
}

function resetFilters() {
  timeRange.value = 'all'
  customRange.value = null
  sellerId.value = null
  applyFilterChange()
}

function applyFilterChange() {
  pagination.page = 1
  searchOrders()
}

// ── 订单详情 ──
const showDetail = ref(false)
const detailOrder = ref(null)
const detailItems = ref([])

// ── 确认弹窗 ──
const confirmShow = ref(false)
const confirmTitle = ref('')
const confirmContent = ref('')
const confirmText = ref('确定')
let confirmCallback = null

function useConfirm(title, content, callback, btnText) {
  confirmTitle.value = title
  confirmContent.value = content
  confirmText.value = btnText || '确定'
  confirmCallback = callback
  confirmShow.value = true
}

function onConfirmOk() {
  confirmShow.value = false
  confirmCallback?.()
}

async function openDetail(order) {
  detailOrder.value = order
  detailItems.value = []
  showDetail.value = true
  try {
    const data = await orderApi.get(order.id)
    detailItems.value = (data.items || []).map(i => ({ ...i, _refundQty: i.quantity - (i.refundedQty || 0) > 0 ? 1 : 0 }))
  } catch { }
}

// ── 单品退款弹窗 ──
const refundItemShow = ref(false)
const refundItemData = ref(null)
const refundItemQty = ref(1)

async function handleItemRefund(item) {
  refundItemData.value = item
  refundItemQty.value = 1
  refundItemShow.value = true
}

async function onRefundItemConfirm() {
  const item = refundItemData.value
  const qty = refundItemQty.value
  if (!item || !qty || qty <= 0 || qty > item.quantity - (item.refundedQty || 0)) return
  try {
    await orderApi.refundItem(detailOrder.value.id, item.id, qty)
    message.success(`已退款 ${qty} 件「${item.productName}」`)
    // 刷新详情
    const data = await orderApi.get(detailOrder.value.id)
    detailItems.value = (data.items || []).map(i => ({ ...i, _refundQty: 0 }))
    detailOrder.value = data.order
    await searchOrders()
  } catch {
  } finally {
    refundItemShow.value = false
  }
}

function handleRefund(order) {
  useConfirm('确认退款', '确定要退款订单 ' + order.orderNo + ' 吗？退款后将恢复商品库存。', async () => {
    try {
      await orderApi.refund(order.id)
      message.success('订单已退款')
      await searchOrders()
    } catch {
    }
  }, '确定退款')
}

function handleDelete(order) {
  useConfirm('确认删除', '确定要删除订单 ' + order.orderNo + ' 吗？此操作不可撤销。', async () => {
    try {
      await orderApi.delete(order.id)
      message.success('订单已删除')
      await searchOrders()
    } catch {
    }
  }, '确定删除')
}

// ── 分页与搜索 ──
const pagination = reactive({
  page: 1,
  pageSize: 10,
  pageSizes: [5, 10, 20, 50]
})

async function searchOrders() {
  loading.value = true
  const range = currentRange()
  try {
    const params = {
      keyword: searchQuery.value.trim(),
      sellerId: sellerId.value || undefined,
      startTime: range ? fmtDateTime(range.start) : undefined,
      endTime: range ? fmtDateTime(range.end) : undefined,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    const res = await orderApi.search(params)
    orders.value = res.records
    total.value = res.total
  } catch {
  }
  loading.value = false
}

async function fetchStaff() {
  try {
    const users = await systemUserApi.list()
    staffOptions.value = users
      .filter(u => u.status !== 0)
      .map(u => ({ label: u.nickname || u.username, value: u.id }))
  } catch { }
}

function onSearchInput() {
  clearTimeout(searchTimer)
  pagination.page = 1
  searchTimer = setTimeout(searchOrders, 300)
}

watch(() => [pagination.page, pagination.pageSize], searchOrders)

onMounted(() => {
  fetchStaff()
  searchOrders()
})


const orderColumns = [
  { title: '订单号', key: 'orderNo', minWidth: 200 },
  { title: '时间', key: 'createTime', minWidth: 160 },
  {
    title: '销售员', key: 'userId', minWidth: 90,
    render(row) {
      const name = userMap.value[row.userId]
      return h('span', { class: 'text-sm text-on-surface dark:text-inverse-on-surface' }, name || '—')
    }
  },
  {
    title: '支付方式', key: 'payment', minWidth: 100,
    render(row) {
      return h('span', { class: 'inline-flex items-center gap-1' }, [
        h('span', paymentLabels[row.payment] || row.payment)
      ])
    }
  },
  { title: '商品数', key: 'itemCount', minWidth: 80 },
  {
    title: '金额', key: 'total', minWidth: 100, className: 'text-right',
    render(row) { return h('span', { class: 'font-semibold' }, '¥' + (row.total?.toFixed(2) || '0.00')) }
  },
  {
    title: '状态', key: 'status', minWidth: 80, className: 'text-right',
    render(row) {
      const isCompleted = row.status === 'completed'
      return h('span', {
        class: 'text-xs font-semibold py-0.5 rounded-md ' +
          (isCompleted
            ? 'text-emerald-600 dark:text-emerald-400'
            : 'text-red-600 dark:text-red-400')
      }, isCompleted ? '已完成' : '已退款')
    }
  },
  {
    title: '操作', key: 'actions', width: 150, fixed: 'right',
    render(row) {
      return h('div', { class: 'inline-flex items-center gap-0.5' }, [
        h('button', {
          class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5',
          onClick: () => openDetail(row)
        }, '详情'),
        row.status === 'completed' ? h('button', {
          class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-500/10',
          onClick: () => handleRefund(row)
        }, [h(Icon, { icon: 'mdi:undo-variant', width: 12 }), '退款']) : null,
        h('button', {
          class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-red-500 hover:bg-red-500/10',
          onClick: () => handleDelete(row)
        }, '删除')
      ])
    }
  }
]

const totalRevenue = computed(() =>
  orders.value.filter(o => o.status === 'completed')
    .reduce((s, o) => s + o.total, 0)
)
</script>

<template>
  <div class="flex-1 min-h-0 flex flex-col gap-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between shrink-0">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">订单管理</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">查看和管理所有交易订单</p>
      </div>
      <div class="flex items-center gap-3">
        <span class="text-xs text-on-surface-variant dark:text-gray-400 font-body">
          总营收 <strong class="text-on-surface dark:text-inverse-on-surface font-body">¥{{ totalRevenue.toFixed(2)
            }}</strong>
        </span>
        <n-input v-model:value="searchQuery" placeholder="搜索订单号..." clearable style="width:220px"
          @update:value="onSearchInput">
          <template #prefix>
            <Icon icon="mdi:magnify" class="text-on-surface-variant/40 dark:text-gray-500" />
          </template>
        </n-input>
      </div>
    </div>

    <!-- 筛选条件：时间范围 + 卖出人（仪表盘同款分段按钮） -->
    <div class="flex flex-wrap items-center gap-2 shrink-0">
      <div class="flex items-center gap-1 p-1 rounded-xl bg-white dark:bg-white/5 border border-outline-variant/50 dark:border-[#333]">
        <button v-for="p in timePresets" :key="p.key"
          class="px-3.5 py-1.5 rounded-lg text-xs font-body font-semibold transition-all duration-200"
          :class="activeTimeKey === p.key
            ? 'bg-black dark:bg-white text-white dark:text-black shadow-sm'
            : 'text-on-surface-variant dark:text-gray-400 hover:text-on-surface dark:hover:text-inverse-on-surface'"
          @click="onTimePreset(p.key)">
          {{ p.label }}
        </button>
        <button
          class="px-3.5 py-1.5 rounded-lg text-xs font-body font-semibold transition-all duration-200 flex items-center gap-1"
          :class="activeTimeKey === 'custom'
            ? 'bg-black dark:bg-white text-white dark:text-black shadow-sm'
            : 'text-on-surface-variant dark:text-gray-400 hover:text-on-surface dark:hover:text-inverse-on-surface'"
          @click="onTimePreset('custom')">
          <Icon v-if="activeTimeKey !== 'custom'" icon="mdi:calendar-range" width="13" />
          自定义
        </button>
        <transition name="date-fade">
          <n-date-picker v-if="timeRange === 'custom'" type="daterange" size="small"
            style="width: 250px; margin-left: 2px" clearable :value="customRange"
            @update:value="onCustomRangeChange" />
        </transition>
      </div>
      <div class="flex items-center gap-1 p-1 rounded-xl bg-white dark:bg-white/5 border border-outline-variant/50 dark:border-[#333]">
        <span class="pl-1.5 pr-0.5 text-xs font-semibold text-on-surface-variant dark:text-gray-400 flex items-center gap-1">
          <Icon icon="mdi:account-tie-outline" width="13" />销售员
        </span>
        <n-select :value="sellerId" size="small" clearable placeholder="全部" style="width: 130px"
          :options="staffOptions" @update:value="onSellerChange" />
      </div>
      <button v-if="hasFilter"
        class="px-3 py-1.5 rounded-lg text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/10 transition-colors flex items-center gap-1"
        @click="resetFilters">
        <Icon icon="mdi:refresh" width="13" />重置
      </button>
    </div>

    <!-- 订单列表 -->
    <n-card class="flex-1 min-h-0 flex flex-col"
      content-style="flex:1;display:flex;flex-direction:column;min-height:0">
      <div v-if="loading" class="flex-1 flex items-center justify-center">
        <Icon icon="mdi:loading" width="24" class="animate-spin text-on-surface-variant dark:text-gray-400" />
      </div>
      <div v-else-if="orders.length === 0"
        class="flex-1 flex flex-col items-center justify-center gap-3 text-on-surface-variant/50 dark:text-gray-500">
        <Icon icon="mdi:receipt-text-outline" width="48" class="opacity-40" />
        <span class="text-sm font-body">暂无订单</span>
      </div>
      <div v-else class="flex-1 min-h-0">
        <n-data-table flex-height :bordered="false" :columns="orderColumns" :data="orders" size="small" scroll-x="900"
          style="height:100%" />
      </div>
    </n-card>
    <div v-if="total > 0" class="flex justify-end pt-2 shrink-0">
      <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize"
        :item-count="total" :page-sizes="pagination.pageSizes" show-size-picker>
        <template #prefix>
          <span class="text-xs text-on-surface-variant">共 {{ total }} 条</span>
        </template>
      </n-pagination>
    </div>

    <!-- 订单详情抽屉 -->
    <n-drawer v-model:show="showDetail" :width="480" placement="right">
      <n-drawer-content v-if="detailOrder" :title="'订单 ' + detailOrder.orderNo">
        <div class="flex flex-col gap-4">
          <!-- 订单基本信息 -->
          <n-card>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <div
                  class="text-[10px] font-body font-semibold uppercase tracking-wider text-on-surface-variant/50 dark:text-gray-500 mb-1">
                  订单号</div>
                <div class="text-sm font-mono text-on-surface dark:text-inverse-on-surface">{{ detailOrder.orderNo }}
                </div>
              </div>
              <div>
                <div
                  class="text-[10px] font-body font-semibold uppercase tracking-wider text-on-surface-variant/50 dark:text-gray-500 mb-1">
                  时间</div>
                <div class="text-sm font-body text-on-surface dark:text-inverse-on-surface">{{ detailOrder.createTime }}
                </div>
              </div>
              <div>
                <div
                  class="text-[10px] font-body font-semibold uppercase tracking-wider text-on-surface-variant/50 dark:text-gray-500 mb-1">
                  支付方式</div>
                <div class="text-sm font-body text-on-surface dark:text-inverse-on-surface">{{
                  paymentLabels[detailOrder.payment] || detailOrder.payment }}</div>
              </div>
              <div>
                <div
                  class="text-[10px] font-body font-semibold uppercase tracking-wider text-on-surface-variant/50 dark:text-gray-500 mb-1">
                  销售员</div>
                <div class="text-sm font-body text-on-surface dark:text-inverse-on-surface">{{
                  userMap[detailOrder.userId] || '—' }}</div>
              </div>
              <div>
                <div
                  class="text-[10px] font-body font-semibold uppercase tracking-wider text-on-surface-variant/50 dark:text-gray-500 mb-1">
                  状态</div>
                <span class="inline-block py-0.5 rounded-md text-xs font-semibold" :class="detailOrder.status === 'completed'
                  ? 'text-emerald-600 dark:text-emerald-400'
                  : 'text-red-600 dark:text-red-400'">
                  {{ detailOrder.status === 'completed' ? '已完成' : '已退款' }}
                </span>
              </div>
            </div>
          </n-card>

          <!-- 商品明细 -->
          <n-card>
            <div
              class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-3">
              商品明细</div>
            <div class="flex flex-col gap-2">
              <template v-for="item in detailItems" :key="item.id">
                <div class="p-3 rounded-lg bg-surface dark:bg-[#1a1a1a]">
                  <div class="flex items-center justify-between">
                    <div
                      class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface truncate min-w-0 mr-3">
                      {{
                        item.productName }}</div>
                    <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface shrink-0">
                      ¥{{
                        (item.subtotal || item.price * item.quantity).toFixed(2) }}</div>
                  </div>
                  <div class="text-xs text-on-surface-variant dark:text-gray-400 mt-1">
                    <template v-if="item.originalPrice != null && Number(item.originalPrice) !== Number(item.price)">
                      <span class="line-through mr-1">原价 ¥{{ Number(item.originalPrice).toFixed(2) }}</span>
                      <span class="px-1 py-px rounded text-[10px] font-semibold bg-red-100 dark:bg-red-950/40 text-red-600 dark:text-red-400">改价</span>
                      <span class="text-red-600 dark:text-red-400 font-bold ml-1">¥{{ Number(item.price).toFixed(2) }}</span>
                    </template>
                    <template v-else>
                      单价：<span class="font-bold">¥{{ Number(item.price).toFixed(2) }}</span>
                    </template>
                  </div>
                  <div class="flex items-center gap-2 mt-0.5">
                    <div class="text-xs" :class="item.refundedQty > 0
                      ? 'text-red-500'
                      : 'text-on-surface-variant dark:text-gray-400'">
                      数量：<span class="font-bold">{{ item.quantity }}</span>
                      <span v-if="item.refundedQty > 0" class="text-red-500">（已退 {{ item.refundedQty }}）</span>
                    </div>
                    <div v-if="detailOrder.status === 'completed' && item.refundedQty < item.quantity">
                      <button
                        class="inline-flex items-center gap-1 py-1 rounded-lg text-xs font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 px-1"
                        @click="handleItemRefund(item)">
                        单品退款
                        <Icon icon="mdi:undo-variant" width="12" />
                      </button>
                    </div>
                  </div>
                </div>
              </template>

              <!-- 折扣信息 -->
              <div v-if="detailOrder.discount > 0" class="flex flex-col gap-1 items-end">
                <div class="text-sm font-body text-on-surface-variant dark:text-gray-400">
                  原价 <span class="line-through">¥{{ (detailOrder.total * 100 / detailOrder.discount).toFixed(2)
                    }}</span>
                </div>
                <div class="text-sm font-body text-amber-600 dark:text-amber-400">
                  折扣 {{ 100 - detailOrder.discount }}%（打{{ detailOrder.discount / 10 }}折）
                </div>
              </div>

              <div class="flex justify-end items-center gap-3">
                <div class="text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400">实付：</div>
                <div class="text-lg font-body font-bold text-on-surface dark:text-inverse-on-surface">¥{{
                  (detailOrder.total || 0).toFixed(2) }}</div>
              </div>

              <div v-if="detailItems.length === 0"
                class="flex items-center justify-center py-8 text-sm text-on-surface-variant/50 dark:text-gray-500">
                暂无明细数据
              </div>
            </div>
          </n-card>
        </div>
      </n-drawer-content>
    </n-drawer>

    <ConfirmDialog :show="confirmShow" :title="confirmTitle" :content="confirmContent" :confirm-text="confirmText"
      type="error" icon-type="warning" @update:show="confirmShow = $event" @confirm="onConfirmOk" />

    <!-- 单品退款弹窗 -->
    <ConfirmDialog :show="refundItemShow" title="单品退款" :hide-default-footer="true" width="340px"
      @update:show="refundItemShow = $event">
      <div class="flex flex-col gap-3">
        <p class="text-sm font-body text-on-surface-variant dark:text-gray-400">
          退款「{{ refundItemData?.productName }}」</p>
        <div class="flex items-center gap-2">
          <span class="text-xs text-on-surface-variant dark:text-gray-400 shrink-0 font-body">退款数量：</span>
          <n-input-number v-model:value="refundItemQty" :min="1"
            :max="(refundItemData?.quantity || 1) - (refundItemData?.refundedQty || 0)" size="small" style="width:80px"
            :show-button="false" placeholder="1" class="flex-1" />
        </div>
        <div class="text-xs text-on-surface-variant/60 dark:text-gray-500">
          可退 {{ (refundItemData?.quantity || 1) - (refundItemData?.refundedQty || 0) }} 件
        </div>
      </div>
      <template #footer>
        <button
          class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
          @click="refundItemShow = false">取消</button>
        <button
          class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-red-500 hover:bg-red-600 transition-colors"
          @click="onRefundItemConfirm">确定退款</button>
      </template>
    </ConfirmDialog>
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

/* 自定义日期选择器淡入 */
.date-fade-enter-active,
.date-fade-leave-active {
  transition: opacity 150ms ease;
}
.date-fade-enter-from,
.date-fade-leave-to {
  opacity: 0;
}
</style>
