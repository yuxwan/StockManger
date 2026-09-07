<script setup>
import { ref, h, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { reportApi } from '../api'

const dateRange = ref('month')
const rangeOptions = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'year', label: '今年' }
]

const data = ref([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    data.value = await reportApi.staffSummary(dateRange.value)
  } catch {}
  loading.value = false
}

onMounted(fetchData)

function switchRange(key) {
  dateRange.value = key
  fetchData()
}

const columns = [
  { title: '员工', key: 'staffName',
    render(row) {
      return h('span', { class: 'font-semibold' }, row.staffName)
    }
  },
  { title: '销售额', key: 'totalRevenue', className: 'text-right',
    render(row) { return h('span', { class: 'font-semibold' }, '¥' + Number(row.totalRevenue).toLocaleString()) }
  },
  { title: '订单数', key: 'orderCount', className: 'text-right' },
  { title: '退款额', key: 'refundAmount', className: 'text-right',
    render(row) {
      const v = Number(row.refundAmount)
      return h('span', v > 0 ? { class: 'text-red-500' } : {}, v > 0 ? '-¥' + v.toLocaleString() : '—')
    }
  },
  { title: '销售额占比', key: 'share', className: 'text-right',
    render(row) {
      return h('span', { class: 'text-on-surface-variant' }, Number(row.share).toFixed(1) + '%')
    }
  }
]
</script>

<template>
  <div class="flex-1 flex flex-col gap-6">
    <!-- 标题 + 时间段切换 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">员工业绩</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">按员工统计销售额，用于工资 / 分红计算</p>
      </div>
      <div class="flex items-center gap-1 p-1 rounded-xl bg-surface dark:bg-[#1a1a1a] border border-outline-variant/50 dark:border-[#333]">
        <button
          v-for="opt in rangeOptions"
          :key="opt.key"
          class="px-4 py-1.5 rounded-lg text-xs font-body font-semibold transition-all"
          :class="dateRange === opt.key
            ? 'bg-black dark:bg-white text-white dark:text-black'
            : 'text-on-surface-variant dark:text-gray-400 hover:text-on-surface'"
          @click="switchRange(opt.key)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- 业绩表格 -->
    <n-card size="small" :bordered="false" style="flex:1;display:flex;flex-direction:column" content-style="flex:1;display:flex;flex-direction:column">
      <template #header>
        <div class="flex items-center gap-2">
          <span class="text-sm font-body font-semibold">销售排行</span>
        </div>
      </template>

      <div v-if="loading" class="flex-1 flex items-center justify-center">
        <Icon icon="mdi:loading" width="24" class="animate-spin text-on-surface-variant dark:text-gray-400" />
      </div>
      <div v-else-if="data.length === 0"
        class="flex-1 flex flex-col items-center justify-center gap-3 text-on-surface-variant/50 dark:text-gray-500">
        <Icon icon="mdi:chart-donut" width="48" class="opacity-40" />
        <span class="text-sm font-body">暂无数据</span>
      </div>
      <div v-else>
        <n-data-table
          :bordered="false"
          :columns="columns"
          :data="data"
          size="small"
        />
      </div>
    </n-card>
  </div>
</template>
