<script setup>
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import message from '../utils/message'
import { productApi, orderApi } from '../api'

const products = ref([])
const loading = ref(false)
const searchQuery = ref('')
const cart = ref({})
const payment = ref('wechat')

async function fetchProducts() {
  try {
    products.value = await productApi.list()
  } catch {
    // 错误已在拦截器处理
  }
}

onMounted(fetchProducts)

function addToCart(product) {
  if (!cart.value[product.id]) {
    cart.value[product.id] = { ...product, qty: 1 }
  } else {
    cart.value[product.id].qty++
  }
}

function updateQty(id, delta) {
  const item = cart.value[id]
  if (!item) return
  item.qty += delta
  if (item.qty <= 0) {
    delete cart.value[id]
  }
}

function handleQtyInput(id, val) {
  if (val === null || val === undefined) return
  if (val <= 0) {
    delete cart.value[id]
  } else {
    cart.value[id].qty = val
  }
}

async function submitOrder() {
  const items = cartItems.value.map(i => ({
    productId: i.id,
    productName: i.name,
    price: i.price,
    quantity: i.qty
  }))
  loading.value = true
  try {
    await orderApi.create({ payment: payment.value, items, discount: discount.value || undefined })
    cart.value = {}
    discount.value = 0
    message.success('交易完成')
  } catch {
    // 已处理
  } finally {
    loading.value = false
  }
}

const filteredProducts = computed(() =>
  products.value.filter(p => p.name.includes(searchQuery.value))
)

const cartItems = computed(() => Object.values(cart.value))

const totalCount = computed(() => cartItems.value.reduce((s, i) => s + i.qty, 0))
const subtotal = computed(() => Math.round(cartItems.value.reduce((s, i) => s + i.price * i.qty, 0) * 100) / 100)
const discount = ref(0)
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
        <n-input v-model:value="searchQuery" placeholder="搜索商品..." clearable>
          <template #prefix>
            <Icon icon="mdi:magnify" class="text-on-surface-variant/40 dark:text-gray-500" />
          </template>
        </n-input>
        <div class="flex flex-wrap gap-2 flex-1 overflow-auto min-h-0 pr-1 content-start">
          <button v-for="p in filteredProducts" :key="p.id"
            class="flex items-center justify-between p-3 rounded-lg bg-surface dark:bg-[#1a1a1a] hover:bg-black/5 dark:hover:bg-white/5"
            @click="addToCart(p)">
            <span
              class="font-body font-semibold text-sm text-on-surface dark:text-inverse-on-surface whitespace-nowrap">{{
                p.name }}</span>
            <span class="text-xs text-on-surface-variant dark:text-gray-400 shrink-0 ml-2">¥{{ p.price }}</span>
          </button>
        </div>

      </div>
    </n-card>

    <!-- 右边：购物车 + 结算 -->
    <n-card class="flex-[1]">
      <div class="min-w-[280px] max-w-[400px] flex flex-col gap-4 h-full">
        <div class="flex items-center justify-between">
          <h3
            class="text-sm font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-400">
            购物车</h3>
          <button v-if="cartItems.length > 0"
            class="text-xs text-on-surface-variant/50 dark:text-gray-500 hover:text-red-500 dark:hover:text-red-400 font-body"
            @click="cart = {}">
            清空
          </button>
        </div>

        <!-- 已选商品列表 -->
        <div class="flex-1 flex flex-col gap-2 overflow-auto min-h-0 pr-1">
          <div v-for="item in cartItems" :key="item.id"
            class="flex items-center justify-between py-3 px-4 rounded-lg bg-surface dark:bg-[#1a1a1a]">
            <div class="min-w-0 mr-2">
              <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface truncate">{{
                item.name }}</div>
              <div class="text-xs text-on-surface-variant dark:text-gray-400">¥{{ item.price }}</div>
              <div v-if="discount > 0 && discount < 100" class="text-xs text-red-500">
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
  </div>
</template>
