<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { orderApi } from '../api'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const message = useMessage()

const orders = ref([])
const loading = ref(false)
const searchQuery = ref('')
const paymentLabels = { wechat: '微信', alipay: '支付宝', cash: '现金' }
const paymentIcons = { wechat: 'simple-icons:wechat', alipay: 'simple-icons:alipay', cash: 'mdi:cash' }

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
    await fetchOrders()
  } catch {
    message.error('退款失败')
  } finally {
    refundItemShow.value = false
  }
}

function handleRefund(order) {
  useConfirm('确认退款', '确定要退款订单 ' + order.orderNo + ' 吗？退款后将恢复商品库存。', async () => {
    try {
      await orderApi.refund(order.id)
      message.success('订单已退款')
      await fetchOrders()
    } catch {
      message.error('退款失败')
    }
  }, '确定退款')
}

function handleDelete(order) {
  useConfirm('确认删除', '确定要删除订单 ' + order.orderNo + ' 吗？此操作不可撤销。', async () => {
    try {
      await orderApi.delete(order.id)
      message.success('订单已删除')
      await fetchOrders()
    } catch {
      message.error('删除失败')
    }
  }, '确定删除')
}

// ── 分页 ──
const pagination = reactive({
  page: 1,
  pageSize: 10,
  pageSizes: [5, 10, 20, 50]
})

const paginatedOrders = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredOrders.value.slice(start, start + pagination.pageSize)
})

async function fetchOrders() {
  loading.value = true
  try {
    orders.value = await orderApi.list()
  } catch { }
  loading.value = false
}

onMounted(fetchOrders)

const filteredOrders = computed(() => {
  if (!searchQuery.value) return orders.value
  const q = searchQuery.value.toLowerCase()
  return orders.value.filter(o =>
    o.orderNo?.toLowerCase().includes(q) ||
    paymentLabels[o.payment]?.includes(q)
  )
})


const orderColumns = [
  { title: '订单号', key: 'orderNo', width: 160 },
  { title: '时间', key: 'createTime', width: 160 },
  {
    title: '支付方式', key: 'payment', width: 100,
    render(row) {
      return h('span', { class: 'inline-flex items-center gap-1' }, [
        h('span', paymentLabels[row.payment] || row.payment)
      ])
    }
  },
  { title: '商品数', key: 'itemCount', width: 80 },
  {
    title: '金额', key: 'total', width: 100, className: 'text-right',
    render(row) { return h('span', { class: 'font-semibold' }, '¥' + (row.total?.toFixed(2) || '0.00')) }
  },
  {
    title: '状态', key: 'status', width: 80, className: 'text-right',
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
    title: '操作', key: 'actions', width: 60, className: 'text-center',
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
  <div class="flex-1 flex flex-col gap-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">订单管理</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">查看和管理所有交易订单</p>
      </div>
      <div class="flex items-center gap-3">
        <span class="text-xs text-on-surface-variant dark:text-gray-400 font-body">
          总营收 <strong class="text-on-surface dark:text-inverse-on-surface font-body">¥{{ totalRevenue.toFixed(2)
            }}</strong>
        </span>
        <div class="relative">
          <Icon icon="mdi:magnify" width="16"
            class="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50 dark:text-gray-500" />
          <input v-model="searchQuery" type="text" placeholder="搜索订单号..."
            class="w-52 h-9 pl-9 pr-3 rounded-xl bg-surface dark:bg-[#1a1a1a] text-sm text-on-surface dark:text-inverse-on-surface outline-none placeholder:text-on-surface-variant/40 dark:placeholder:text-gray-600 font-body" />
        </div>
      </div>
    </div>

    <!-- 订单列表 -->
    <n-card size="small" :bordered="false" style="flex:1;display:flex;flex-direction:column"
      content-style="flex:1;display:flex;flex-direction:column">
      <div v-if="loading" class="flex-1 flex items-center justify-center">
        <Icon icon="mdi:loading" width="24" class="animate-spin text-on-surface-variant dark:text-gray-400" />
      </div>
      <div v-else-if="paginatedOrders.length === 0"
        class="flex-1 flex flex-col items-center justify-center gap-3 text-on-surface-variant/50 dark:text-gray-500">
        <Icon icon="mdi:receipt-text-outline" width="48" class="opacity-40" />
        <span class="text-sm font-body">暂无订单</span>
      </div>
      <div v-else>
        <n-data-table :bordered="false" :columns="orderColumns" :data="paginatedOrders" size="small" />
      </div>
    </n-card>
    <div class="flex justify-end pt-2">
      <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize"
        :item-count="filteredOrders.length" :page-sizes="pagination.pageSizes" show-size-picker>
        <template #prefix>
          <span class="text-xs text-on-surface-variant">共 {{ filteredOrders.length }} 条</span>
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
                  <div class="text-xs text-on-surface-variant dark:text-gray-400 mt-1">单价：¥
                    <span class="font-bold">{{ item.price }}</span>
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
                        class="inline-flex items-center gap-1 py-1 rounded-lg text-xs font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-500/10"
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
</style>
