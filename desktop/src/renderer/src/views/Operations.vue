<template>
  <!-- 最外层不产生滚动条：根与卡片都限制在 main 可视高度内（min-h-0），
       数据多时只由 n-data-table(flex-height) 在自身区域内滚动 -->
  <div class="flex-1 min-h-0 flex flex-col gap-6">
    <div class="shrink-0">
      <h1 class="text-2xl font-body font-bold tracking-tight">操作日志</h1>
      <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">商品新增、编辑、入库、出库等操作记录</p>
    </div>

    <n-card class="flex-1 min-h-0 flex flex-col" content-style="flex:1;display:flex;flex-direction:column;min-height:0">
      <div v-if="loading" class="flex-1 flex items-center justify-center">
        <Icon icon="mdi:loading" width="24" class="animate-spin text-on-surface-variant dark:text-gray-400" />
      </div>
      <div v-else-if="logs.length === 0"
        class="flex-1 flex flex-col items-center justify-center gap-3 text-on-surface-variant/50 dark:text-gray-500">
        <Icon icon="mdi:clipboard-text-clock-outline" width="48" class="opacity-40" />
        <span class="text-sm font-body">暂无操作记录</span>
      </div>
      <div v-else class="flex-1 min-h-0">
        <n-data-table flex-height :bordered="false" :columns="columns" :data="paginatedLogs" size="small"
          style="height:100%" />
      </div>
    </n-card>

    <div class="flex justify-end pt-2 shrink-0">
      <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize"
        :item-count="logs.length" :page-sizes="pagination.pageSizes" show-size-picker>
        <template #prefix>
          <span class="text-xs text-on-surface-variant">共 {{ logs.length }} 条</span>
        </template>
      </n-pagination>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { operationLogApi } from '../api'

const logs = ref([])
const loading = ref(false)

const typeLabels = {
  CREATE_PRODUCT: '新增商品',
  UPDATE_PRODUCT: '编辑商品',
  DELETE_PRODUCT: '删除商品',
  STOCK_IN: '入库',
  STOCK_OUT: '出库',
  SALE: '收银出库',
  REFUND: '退款入库',
  CREATE_ORDER: '创建订单'
}

const typeIcons = {
  CREATE_PRODUCT: 'mdi:plus-circle-outline',
  UPDATE_PRODUCT: 'mdi:pencil-outline',
  DELETE_PRODUCT: 'mdi:delete-outline',
  STOCK_IN: 'mdi:plus-circle-outline',
  STOCK_OUT: 'mdi:minus-circle-outline',
  SALE: 'mdi:cart-outline',
  REFUND: 'mdi:undo-variant',
  CREATE_ORDER: 'mdi:receipt-text-outline'
}

const typeColors = {
  CREATE_PRODUCT: 'text-emerald-600 dark:text-emerald-400',
  UPDATE_PRODUCT: 'text-blue-600 dark:text-blue-400',
  DELETE_PRODUCT: 'text-red-500',
  STOCK_IN: 'text-emerald-600 dark:text-emerald-400',
  STOCK_OUT: 'text-amber-600 dark:text-amber-400',
  SALE: 'text-sky-600 dark:text-sky-400',
  REFUND: 'text-orange-600 dark:text-orange-400',
  CREATE_ORDER: 'text-violet-600 dark:text-violet-400'
}

const pagination = reactive({
  page: 1,
  pageSize: 20,
  pageSizes: [10, 20, 50, 100]
})

const paginatedLogs = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return logs.value.slice(start, start + pagination.pageSize)
})

async function fetchLogs() {
  loading.value = true
  try {
    logs.value = await operationLogApi.list(200)
  } catch { }
  loading.value = false
}

onMounted(fetchLogs)

const columns = [
  {
    title: '时间', key: 'createTime'
  },
  {
    title: '操作', key: 'type',
    render(row) {
      return h('span', {
        class: 'inline-flex items-center gap-1 ' + (typeColors[row.type] || '')
      }, [
        h(Icon, { icon: typeIcons[row.type] || 'mdi:circle-small', width: 14 }),
        typeLabels[row.type] || row.type
      ])
    }
  },
  {
    title: '商品', key: 'targetName',
    render(row) {
      return h('span', { class: 'text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface' }, row.targetName)
    }
  },
  {
    title: '详情', key: 'detail'
  },
  {
    title: '操作员', key: 'operatorName'
  }
]
</script>
