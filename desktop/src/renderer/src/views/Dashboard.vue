<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useReportsStore } from '../stores/reports'

use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const dpr = window.devicePixelRatio || 2
const store = useReportsStore()

function getTheme() {
  return document.documentElement.classList.contains('dark') ? 'dark' : 'light'
}

const theme = ref(getTheme())
let observer

onMounted(() => {
  observer = new MutationObserver(() => { theme.value = getTheme() })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
  store.fetchSummary()
})
onUnmounted(() => observer?.disconnect())

const isDark = computed(() => theme.value === 'dark')

const rangeOptions = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'year', label: '今年' }
]

// ── 较昨日变化（今日对比昨天） ──
const yesterdayRevenue = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10)
  const todayRev = store.revenueTrend.find(d => d.date === today)?.revenue ?? 0
  const yestRev = store.revenueTrend.find(d => d.date === yesterday)?.revenue ?? 0
  if (yestRev === 0) return null
  return ((todayRev - yestRev) / yestRev * 100).toFixed(1)
})

const yesterdayOrders = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10)
  const todayOrders = store.filteredOrders.filter(o => o.date === today && o.status === 'completed').length
  const yestOrders = store.filteredOrders.filter(o => o.date === yesterday && o.status === 'completed').length
  if (yestOrders === 0) return null
  return ((todayOrders - yestOrders) / yestOrders * 100).toFixed(1)
})

// ── 收入趋势图（替换原来的硬编码周数据） ──
const trendOption = computed(() => ({
  textStyle: {
    fontFamily: '"Plus Jakarta Sans"',
    fontSize: 12
  },
  tooltip: {
    trigger: 'axis',
    backgroundColor: isDark.value ? '#252525' : '#ffffff',
    borderColor: 'transparent',
    textStyle: {
      color: isDark.value ? '#f2f0f0' : '#1b1c1c',
      fontSize: 13
    },
    formatter: (params) => {
      const p = params[0]
      return `${p.axisValue}<br/><span style="font-weight:600">¥${p.value.toLocaleString()}</span>`
    }
  },
  grid: {
    left: 50,
    right: 20,
    top: 10,
    bottom: 30
  },
  xAxis: {
    type: 'category',
    data: store.revenueTrend.map(d => {
      const parts = d.date.split('-')
      return `${Number(parts[1])}/${Number(parts[2])}`
    }),
    axisLine: { lineStyle: { color: isDark.value ? '#444' : '#cfc4c5' } },
    axisTick: { show: false },
    axisLabel: {
      color: isDark.value ? '#999' : '#7e7576',
      fontFamily: '"Hanken Grotesk"',
      fontSize: 11,
      fontWeight: 600
    }
  },
  yAxis: {
    type: 'value',
    splitLine: {
      lineStyle: {
        color: isDark.value ? '#333' : '#e3e2e2',
        type: 'dashed'
      }
    },
    axisLabel: {
      color: isDark.value ? '#999' : '#7e7576',
      fontFamily: '"Hanken Grotesk"',
      fontSize: 11,
      fontWeight: 600,
      formatter: '¥{value}'
    },
    axisLine: { show: false },
    axisTick: { show: false }
  },
  series: [
    {
      type: 'line',
      smooth: true,
      data: store.revenueTrend.map(d => d.revenue),
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        color: isDark.value ? '#ffffff' : '#000000',
        width: 2
      },
      itemStyle: {
        color: isDark.value ? '#ffffff' : '#000000'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: isDark.value ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.08)' },
            { offset: 1, color: isDark.value ? 'rgba(255,255,255,0)' : 'rgba(0,0,0,0)' }
          ]
        }
      }
    }
  ]
}))

// ── 分类饼图 ──
// ── 支付方式柱状图 ──
const paymentOption = computed(() => ({
  textStyle: {
    fontFamily: '"Plus Jakarta Sans"',
    fontSize: 11
  },
  tooltip: {
    trigger: 'axis',
    backgroundColor: isDark.value ? '#252525' : '#ffffff',
    borderColor: 'transparent',
    textStyle: {
      color: isDark.value ? '#f2f0f0' : '#1b1c1c',
      fontSize: 13
    },
    formatter: (params) => {
      const p = params[0]
      return `${p.name}<br/>¥${p.value.toLocaleString()}`
    }
  },
  grid: {
    left: 50,
    right: 20,
    top: 10,
    bottom: 25
  },
  xAxis: {
    type: 'category',
    data: store.paymentBreakdown.map(p => p.label),
    axisLine: { lineStyle: { color: isDark.value ? '#444' : '#cfc4c5' } },
    axisTick: { show: false },
    axisLabel: {
      color: isDark.value ? '#999' : '#7e7576',
      fontFamily: '"Hanken Grotesk"',
      fontSize: 11,
      fontWeight: 600
    }
  },
  yAxis: {
    type: 'value',
    splitLine: {
      lineStyle: {
        color: isDark.value ? '#333' : '#e3e2e2',
        type: 'dashed'
      }
    },
    axisLabel: {
      color: isDark.value ? '#999' : '#7e7576',
      fontFamily: '"Hanken Grotesk"',
      fontSize: 11,
      fontWeight: 600,
      formatter: '¥{value}'
    },
    axisLine: { show: false },
    axisTick: { show: false }
  },
  series: [
    {
      type: 'bar',
      barWidth: '40%',
      borderRadius: [6, 6, 0, 0],
      itemStyle: {
        color: isDark.value ? '#ffffff' : '#000000'
      },
      data: store.paymentBreakdown.map(p => p.value)
    }
  ]
}))

// ── 热销商品 ──
const topProducts = computed(() =>
  [...store.productSales].sort((a, b) => b.qty - a.qty).slice(0, 10)
)

const maxQty = computed(() => Math.max(...topProducts.value.map(p => p.qty), 1))
</script>

<template>
  <div class="flex-1 flex flex-col gap-6">
    <!-- ═══ 顶栏：标题 + 时间筛选 ═══ -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">仪表盘</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">经营数据总览</p>
      </div>
      <div
        class="flex items-center gap-1 p-1 rounded-xl bg-surface dark:bg-[#1a1a1a] border border-outline-variant/50 dark:border-[#333]">
        <button v-for="opt in rangeOptions" :key="opt.key"
          class="px-4 py-1.5 rounded-lg text-xs font-body font-semibold transition-all duration-200" :class="store.dateRange === opt.key
            ? 'bg-black dark:bg-white text-white dark:text-black shadow-sm'
            : 'text-on-surface-variant dark:text-gray-400 hover:text-on-surface dark:hover:text-inverse-on-surface'"
          @click="store.setDateRange(opt.key)">
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- ═══ 统计卡片（数据来自 reports store，跟随时间筛选） ═══ -->
    <div class="grid grid-cols-4 gap-4 w-full">
      <n-card>
        <div class="flex items-center gap-3 h-full">
          <div
            class="w-11 h-11 rounded-xl bg-emerald-100 dark:bg-emerald-950/30 flex items-center justify-center shrink-0">
            <Icon icon="mdi:cash" width="20" class="text-emerald-600 dark:text-emerald-400" />
          </div>
          <div class="flex flex-col justify-center">
            <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body">总营收</div>
            <div class="text-xl font-body font-bold tracking-tight">¥{{ store.totalRevenue.toLocaleString() }}</div>
            <div v-if="yesterdayRevenue != null" class="flex items-center justify-center gap-0.5 mt-0.5">
              <Icon :icon="Number(yesterdayRevenue) >= 0 ? 'mdi:trending-up' : 'mdi:trending-down'" width="14"
                :class="Number(yesterdayRevenue) >= 0 ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'" />
              <span class="text-xs font-body font-semibold"
                :class="Number(yesterdayRevenue) >= 0 ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'">
                {{ Number(yesterdayRevenue) >= 0 ? '+' : '' }}{{ yesterdayRevenue }}%
              </span>
              <span class="text-[10px] text-on-surface-variant/50 dark:text-gray-500 ml-1">较昨日</span>
            </div>
          </div>

        </div>
      </n-card>

      <!-- 订单数 -->
      <n-card>
        <div class="flex items-center gap-3 h-full">
          <div class="w-11 h-11 rounded-xl bg-blue-100 dark:bg-blue-950/30 flex items-center justify-center shrink-0">
            <Icon icon="mdi:shopping-outline" width="20" class="text-blue-600 dark:text-blue-400" />
          </div>
          <div class="flex flex-col justify-center">
            <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body">订单数</div>
            <div class="text-xl font-body font-bold tracking-tight">{{ store.totalOrders }} 单</div>
            <div v-if="yesterdayOrders != null" class="flex items-center justify-center gap-0.5 mt-0.5">
              <Icon :icon="Number(yesterdayOrders) >= 0 ? 'mdi:trending-up' : 'mdi:trending-down'" width="14"
                :class="Number(yesterdayOrders) >= 0 ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'" />
              <span class="text-xs font-body font-semibold"
                :class="Number(yesterdayOrders) >= 0 ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'">
                {{ Number(yesterdayOrders) >= 0 ? '+' : '' }}{{ yesterdayOrders }}%
              </span>
              <span class="text-[10px] text-on-surface-variant/50 dark:text-gray-500 ml-1">较昨日</span>
            </div>
          </div>
        </div>
      </n-card>

      <!-- 平均客单价 -->
      <n-card>
        <div class="flex items-center gap-3 h-full">
          <div
            class="w-11 h-11 rounded-xl bg-purple-100 dark:bg-purple-950/30 flex items-center justify-center shrink-0">
            <Icon icon="mdi:receipt-text-outline" width="20" class="text-purple-600 dark:text-purple-400" />
          </div>
          <div class="flex flex-col justify-center">
            <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body">平均客单价</div>
            <div class="text-xl font-body font-bold tracking-tight">¥{{ store.avgOrderValue.toLocaleString() }}</div>
          </div>
        </div>
      </n-card>

      <!-- 退款 -->
      <n-card>
        <div class="flex items-center gap-3 h-full">
          <div class="w-11 h-11 rounded-xl bg-red-100 dark:bg-red-950/30 flex items-center justify-center shrink-0">
            <Icon icon="mdi:undo-variant" width="20" class="text-red-600 dark:text-red-400" />
          </div>
          <div class="flex flex-col justify-center">
            <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body">退款</div>
            <div class="text-xl font-body font-bold tracking-tight text-red-600 dark:text-red-400">
              {{ store.refundCount }} 单
              <span class="text-sm font-body font-normal text-on-surface-variant dark:text-gray-400">
                / ¥{{ store.refundAmount.toLocaleString() }}
              </span>
            </div>
          </div>
        </div>
      </n-card>
    </div>

    <!-- ═══ 图表行：收入趋势（替换原来的硬编码周数据） + 低库存预警 ═══ -->
    <div class="flex gap-4 min-h-[300px]">
      <n-card style="flex:3" content-style="display:flex;flex-direction:column;flex:1;min-height:0;padding:24px">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-base font-body font-semibold tracking-tight">收入趋势</h2>
          <span
            class="text-xs text-on-surface-variant dark:text-gray-400 font-body font-semibold tracking-wider uppercase">单位：元</span>
        </div>
        <div v-if="store.revenueTrend.length === 0"
          class="flex-1 flex items-center justify-center text-sm text-on-surface-variant dark:text-gray-500">
          暂无数据
        </div>
        <div v-else class="flex-1 min-h-0">
          <VChart :option="trendOption" autoresize class="h-full w-full" :init-options="{ devicePixelRatio: dpr }" />
        </div>
      </n-card>

      <!-- 低库存预警 -->
      <n-card style="flex:2;display:flex;flex-direction:column"
        content-style="display:flex;flex-direction:column;flex:1;min-height:0;padding:24px">
        <div class="flex items-center gap-2 mb-4">
          <Icon icon="mdi:alert-circle-outline" width="18" class="text-amber-500" />
          <h2 class="text-sm font-body font-semibold tracking-tight">低库存预警</h2>
        </div>
        <div v-if="store.lowStockProducts.length === 0"
          class="flex-1 flex items-center justify-center text-sm text-on-surface-variant dark:text-gray-500">
          暂无低库存商品
        </div>
        <div v-else class="flex-1 flex flex-col gap-3 overflow-auto">
          <div v-for="item in store.lowStockProducts" :key="item.id"
            class="flex items-center justify-between py-2.5 px-3 rounded-xl bg-surface dark:bg-[#1a1a1a]">
            <span class="text-sm font-body text-on-surface dark:text-inverse-on-surface truncate mr-2">{{ item.name
              }}</span>
            <span class="shrink-0 text-xs font-body font-semibold px-2 py-0.5 rounded-md" :class="item.stock <= 3
              ? 'bg-red-100 dark:bg-red-950/40 text-red-600 dark:text-red-400'
              : 'bg-amber-100 dark:bg-amber-950/40 text-amber-600 dark:text-amber-300'">
              仅剩 {{ item.stock }} {{ item.unit }}
            </span>
          </div>
        </div>
      </n-card>
    </div>


    <!-- ═══ 图表行：支付方式 + 热销商品排行 ═══ -->
    <div class="flex gap-4 min-h-[350px]">
      <n-card style="flex:1" content-style="display:flex;flex-direction:column;flex:1;min-height:0;padding:24px">
        <h2 class="text-base font-body font-semibold tracking-tight mb-4">支付方式分布</h2>
        <div v-if="store.paymentBreakdown.length === 0"
          class="flex-1 flex items-center justify-center text-sm text-on-surface-variant dark:text-gray-500">
          暂无数据
        </div>
        <div v-else class="flex-1 min-h-0">
          <VChart :option="paymentOption" autoresize class="h-full w-full" :init-options="{ devicePixelRatio: dpr }" />
        </div>
      </n-card>
      <n-card style="flex:1" content-style="display:flex;flex-direction:column;flex:1;min-height:0;padding:24px">
        <h2 class="text-base font-body font-semibold tracking-tight mb-4">热销商品排行
          <span
            class="text-xs text-on-surface-variant dark:text-gray-400 font-body font-semibold tracking-wider uppercase ml-2">按销量</span>
        </h2>
        <div v-if="topProducts.length === 0"
          class="flex-1 flex items-center justify-center text-sm text-on-surface-variant dark:text-gray-500">
          暂无数据
        </div>
        <div v-else class="flex flex-col gap-3">
          <div v-for="(p, i) in topProducts" :key="p.name" class="flex items-center gap-3">
            <span class="w-6 h-6 rounded-lg flex items-center justify-center text-xs font-body font-bold shrink-0"
              :class="{
                'bg-black dark:bg-white text-white dark:text-black': i < 3,
                'bg-surface dark:bg-[#1a1a1a] text-on-surface-variant dark:text-gray-400': i >= 3
              }">
              {{ i + 1 }}
            </span>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-1">
                <span class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface truncate">{{
                  p.name }}</span>
                <span class="text-xs font-body font-semibold ml-2 shrink-0">
                  {{ p.qty }} <span class="text-on-surface-variant dark:text-gray-400 font-normal">单</span>
                </span>
              </div>
              <div class="h-2 rounded-full bg-surface dark:bg-[#1a1a1a] overflow-hidden">
                <div class="h-full rounded-full transition-all duration-500"
                  :class="i === 0 ? 'bg-black dark:bg-white' : 'bg-black/20 dark:bg-white/20'"
                  :style="{ width: `${(p.qty / maxQty) * 100}%` }"></div>
              </div>
            </div>
            <span class="text-xs text-on-surface-variant dark:text-gray-400 font-body shrink-0 w-14 text-right">¥{{
              p.revenue.toLocaleString() }}</span>
          </div>
        </div>
      </n-card>
    </div>
  </div>
</template>
