<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import JsBarcode from 'jsbarcode'
import message from '../utils/message'
import { productApi } from '../api'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const submitting = ref(false)
const form = ref({ barcode: '', name: '', spec: '', price: '', purchasePrice: '', stock: '', unit: '', location: '', expiryType: 'days', expiryValue: '', expiryDate: null })
const barcodeInput = ref(null)
const expiryOptions = [
  { value: 'days', label: '按天' },
  { value: 'months', label: '按月' },
  { value: 'years', label: '按年' },
  { value: 'date', label: '到期时间' }
]
const labelSize = ref({ width: 60, height: 40, label: '60×40mm' })
const labelSizes = [
  { width: 40, height: 30, label: '40×30mm' },
  { width: 50, height: 30, label: '50×30mm' },
  { width: 60, height: 40, label: '60×40mm' },
  { width: 100, height: 50, label: '100×50mm' }
]

const totalPrice = computed(() => {
  const price = Number(form.value.purchasePrice)
  const stock = Number(form.value.stock)
  return price && stock ? price * stock : 0
})

onMounted(async () => {
  // 编辑模式：加载商品数据
  if (isEdit.value) {
    try {
      const product = await productApi.get(route.params.id)
      if (product) {
        form.value = {
          barcode: product.barcode || '',
          name: product.name || '',
          spec: product.spec || '',
          price: product.price || '',
          purchasePrice: product.purchasePrice || '',
          stock: product.stock || '',
          unit: product.unit || '',
          location: product.location || '',
          expiryType: 'days',
          expiryValue: '',
          expiryDate: null
        }
        // 解析有效期
        if (product.expiry) {
          if (product.expiry.includes('-')) {
            form.value.expiryType = 'date'
            form.value.expiryDate = product.expiry
          } else {
            const match = product.expiry.match(/^(\d+)([DMY])$/)
            if (match) {
              form.value.expiryValue = Number(match[1])
              form.value.expiryType = { D: 'days', M: 'months', Y: 'years' }[match[2]] || 'days'
            }
          }
        }
      }
    } catch (err) {
      message.error('加载商品失败')
    }
  } else {
    barcodeInput.value?.focus()
  }
})

function generateBarcode() {
  const ts = Date.now().toString().slice(-8)
  const rand = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  form.value.barcode = ts + rand
}

// 条码输入后自动查询商品信息
let barcodeTimer = null
watch(() => form.value.barcode, (val) => {
  clearTimeout(barcodeTimer)
  if (!val || val.length < 4) return
  barcodeTimer = setTimeout(async () => {
    try {
      const product = await productApi.getByBarcode(val)
      if (product) {
        form.value.name = product.name || ''
        form.value.spec = product.spec || ''
        form.value.price = product.price || ''
        form.value.purchasePrice = product.purchasePrice || ''
        form.value.unit = product.unit || ''
        form.value.location = product.location || ''
      }
    } catch {}
  }, 400)
})

function printLabel() {
  const { barcode } = form.value
  if (!barcode) {
    message.warning('请填写条码或点击生成条码')
    return
  }
  const tmp = document.createElement('svg')
  JsBarcode(tmp, barcode, { format: 'CODE128', width: 2, height: 50, displayValue: true, fontSize: 13, margin: 8 })
  const svgHtml = tmp.outerHTML

  const { width, height } = labelSize.value
  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;top:0;left:0;width:0;height:0;border:none;opacity:0'
  document.body.appendChild(iframe)
  iframe.contentWindow.document.write(`
    <html><head><style>
      @page { size: ${width}mm ${height}mm; margin:0; }
      html, body { width:${width}mm; height:${height}mm; margin:0; padding:0; }
      body { display:grid; place-items:center; padding:4mm; font-family:sans-serif; box-sizing:border-box; }
      svg { max-width:90%; }
      h2 { margin:2mm 0 1mm; font-size:3.5mm; text-align:center; }
      p { margin:0; color:#666; font-size:2.5mm; }
    </style></head><body>
      ${svgHtml}
    </body></html>
  `)
  iframe.contentWindow.document.close()
  iframe.contentWindow.focus()
  setTimeout(() => {
    iframe.contentWindow.print()
    setTimeout(() => document.body.removeChild(iframe), 500)
  }, 300)
}

function onBarcodeKeydown(e) {
  if (e.key === 'Enter' && form.value.barcode) {
    form.value.name && form.value.stock !== null && form.value.stock !== '' && form.value.price !== null && form.value.price !== '' && submit()
  }
}

async function submit() {
  const { barcode, name, spec, price, purchasePrice, stock, unit, location, expiryType, expiryValue, expiryDate } = form.value
  if (!barcode) {
    message.warning('请填写条码或点击生成条码')
    return
  }
  if (!name) {
    message.warning('请输入商品名称')
    return
  }
  if (price === null || price === '') {
    message.warning('请输入销售价')
    return
  }
  if (stock === null || stock === '') {
    message.warning('请输入数量')
    return
  }
  submitting.value = true
  let expiry = ''
  if (expiryType === 'date') {
    if (expiryDate) expiry = expiryDate
  } else if (expiryValue) {
    expiry = `${expiryValue}${{ days: 'D', months: 'M', years: 'Y' }[expiryType]}`
  }
  const data = { barcode, name, spec, price: Number(price), purchasePrice: purchasePrice === '' ? null : Number(purchasePrice), stock: Number(stock), unit, location, expiry }
  try {
    if (isEdit.value) {
      await productApi.update(route.params.id, data)
      message.success('修改成功')
    } else {
      const result = await productApi.create(data)
      if (result.type === 'stock_in') {
        message.success(`已补充库存 +${result.added} ${result.product.unit || '件'}`)
      } else {
        message.success('添加成功')
      }
    }
    router.push('/products')
  } catch {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex-1 max-w-xl mx-auto">
    <div class="p-6 rounded-2xl bg-surface-container dark:bg-[#252525] drop-shadow-center dark:drop-shadow-center-dark">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <button class="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-black/10 dark:hover:bg-white/10" @click="router.back()">
            <Icon icon="mdi:arrow-left" width="18" class="text-on-surface dark:text-inverse-on-surface" />
          </button>
          <h2 class="text-lg font-body font-bold tracking-tight text-on-surface dark:text-inverse-on-surface">{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
        </div>
      </div>

      <div class="flex flex-col gap-5">
        <!-- 条码 -->
        <div>
          <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">
            条码
            <span class="font-body font-normal normal-case text-on-surface-variant/50 dark:text-gray-600 ml-1">扫码枪或手动输入</span>
          </label>
          <div class="flex gap-2">
            <div class="relative flex-1">
              <n-input
                ref="barcodeInput"
                v-model:value="form.barcode"
                placeholder="扫描或输入条码"
                clearable
                @keydown.enter="onBarcodeKeydown"
              >
                <template #prefix>
                  <Icon icon="mdi:barcode-scan" class="text-on-surface-variant/40 dark:text-gray-600 z-10" />
                </template>
              </n-input>
            </div>
            <button class="h-8 px-3.5 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors flex items-center gap-1.5" :disabled="submitting" @click="generateBarcode">
              <Icon icon="mdi:barcode-scan" width="14" />生成条码
            </button>
          </div>
          <div v-if="form.barcode" class="mt-2 text-xs text-on-surface-variant dark:text-gray-500 font-mono">
            条码: {{ form.barcode }}
          </div>
        </div>

        <!-- 商品名称 -->
        <div>
          <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">商品名称</label>
          <n-input v-model:value="form.name" placeholder="输入商品名称" clearable />
        </div>

        <!-- 品牌规格型号 -->
        <div>
          <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">品牌规格型号</label>
          <n-input v-model:value="form.spec" placeholder="如 农夫山泉·550ml" clearable />
        </div>

        <!-- 存放位置 -->
        <div>
          <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">存放位置</label>
          <n-input v-model:value="form.location" placeholder="如 A1-04 货架" clearable />
        </div>

        <!-- 有效期 -->
        <div>
          <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">有效期</label>
          <div class="flex gap-3">
            <n-select v-model:value="form.expiryType" :options="expiryOptions" style="width:110px" />
            <template v-if="form.expiryType !== 'date'">
              <n-input-number v-model:value="form.expiryValue" placeholder="0" :min="0" style="width:100px" />
              <div class="flex items-center text-sm text-on-surface-variant dark:text-gray-500 font-body">
                {{ { days: '天', months: '个月', years: '年' }[form.expiryType] }}
              </div>
            </template>
            <n-date-picker
              v-else
              v-model:formatted-value="form.expiryDate"
              value-format="yyyy-MM-dd"
              placeholder="选择到期日期"
              class="flex-1"
            />
          </div>
        </div>

        <div class="grid grid-cols-4 gap-3">
          <div>
            <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">销售价（元）</label>
            <n-input-number v-model:value="form.price" placeholder="0" :min="0" clearable style="width:100%" />
          </div>
          <div>
            <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">进货价（元）</label>
            <n-input-number v-model:value="form.purchasePrice" placeholder="0" :min="0" clearable style="width:100%" />
          </div>
          <div>
            <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">数量</label>
            <n-input-number v-model:value="form.stock" placeholder="0" :min="0" clearable style="width:100%" />
          </div>
          <div>
            <label class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">单位</label>
            <n-input v-model:value="form.unit" placeholder="个" clearable />
          </div>
        </div>
      </div>

      <!-- 本次入库价格 -->
      <div class="flex items-center justify-between px-4 py-3 rounded-lg bg-surface dark:bg-[#1a1a1a] mt-6">
        <span class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500">本次入库价格</span>
        <span class="text-lg font-body font-bold text-on-surface dark:text-inverse-on-surface">¥{{ totalPrice.toLocaleString() }}</span>
      </div>

      <!-- 标签尺寸 -->
      <div class="flex items-center justify-between mt-5">
        <span class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500">标签尺寸</span>
        <div class="flex gap-1">
          <button
            v-for="s in labelSizes"
            :key="s.label"
            class="px-2.5 py-1 rounded text-[11px] font-body font-semibold"
            :class="labelSize.label === s.label
              ? 'bg-black dark:bg-white text-white dark:text-black'
              : 'text-on-surface-variant dark:text-gray-500 hover:bg-black/10 dark:hover:bg-white/10'"
            @click="labelSize = s"
          >{{ s.label }}</button>
        </div>
      </div>

      <div class="flex gap-3 mt-4">
        <button class="h-9 flex-1 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" :disabled="submitting" @click="router.back()">取消</button>
        <button class="h-9 flex-1 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors flex items-center justify-center gap-1.5" :disabled="submitting" @click="printLabel">
          <Icon icon="mdi:printer-outline" width="14" />打印标签
        </button>
        <button class="h-9 flex-1 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center justify-center gap-1.5" :disabled="submitting" @click="submit">
          <Icon v-if="submitting" icon="mdi:loading" width="14" class="animate-spin" />
          {{ isEdit ? '保存修改' : '确认添加' }}
        </button>
      </div>
    </div>
  </div>
</template>
