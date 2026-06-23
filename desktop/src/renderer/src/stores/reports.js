import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { reportApi } from '../api'

export const useReportsStore = defineStore('reports', () => {
  const dateRange = ref('week')
  const rawData = ref(null)

  // ── 从后端拉取 ──
  async function fetchSummary() {
    rawData.value = await reportApi.summary(dateRange.value)
  }

  // ── 派生数据 ──
  const totalRevenue = computed(() => rawData.value?.totalRevenue ?? 0)
  const totalOrders = computed(() => rawData.value?.totalOrders ?? 0)
  const avgOrderValue = computed(() => rawData.value?.avgOrderValue ?? 0)
  const refundCount = computed(() => rawData.value?.refundCount ?? 0)
  const refundAmount = computed(() => rawData.value?.refundAmount ?? 0)
  const revenueTrend = computed(() => rawData.value?.revenueTrend ?? [])
  const paymentBreakdown = computed(() => rawData.value?.paymentBreakdown ?? [])
  const topProducts = computed(() => rawData.value?.topProducts ?? [])
  const categorySummary = computed(() => rawData.value?.categorySummary ?? [])
  const lowStockProducts = computed(() => rawData.value?.lowStockProducts ?? [])

  // ── 模拟商品销售明细（热销商品用后端数据，分类汇总需要 detail） ──
  const productSales = computed(() =>
    (rawData.value?.topProducts ?? []).map(p => ({
      name: p.name,
      category: '商品',
      qty: p.qty,
      revenue: p.revenue
    }))
  )

  // 保留 filteredOrders 用于较昨日计算
  const filteredOrders = computed(() => [])

  function setDateRange(range) {
    dateRange.value = range
    fetchSummary()
  }

  return {
    dateRange, rawData,
    totalRevenue, totalOrders, avgOrderValue,
    refundCount, refundAmount,
    revenueTrend, paymentBreakdown, topProducts, categorySummary,
    productSales, filteredOrders,
    lowStockProducts,
    setDateRange, fetchSummary
  }
})
