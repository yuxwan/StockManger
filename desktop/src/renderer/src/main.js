import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import naive from 'naive-ui'
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(naive)

app.config.errorHandler = (err, vm, info) => {
  console.error('[Page Error]', err, info)
}

app.mount('#app')

// ── 扫码枪全局监听 ──
const SCAN_MIN_LENGTH = 4
const SCAN_MAX_INTERVAL = 80 // 两次按键最大间隔(ms)
let scanBuffer = ''
let scanLastTime = 0

document.addEventListener('keydown', (e) => {
  // 如果在输入框中则不处理
  const tag = e.target?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  if (e.target?.isContentEditable) return

  const now = Date.now()

  if (e.key === 'Enter') {
    if (scanBuffer.length >= SCAN_MIN_LENGTH && now - scanLastTime < SCAN_MAX_INTERVAL * 3) {
      e.preventDefault()
      const code = scanBuffer
      scanBuffer = ''
      scanLastTime = 0
      // 已在收银页则直接派发事件，否则跳转
      if (window.location.hash.includes('/checkout')) {
        window.dispatchEvent(new CustomEvent('barcode-scanned', { detail: code }))
      } else {
        sessionStorage.setItem('pendingBarcode', code)
        router.push('/checkout')
      }
      return
    }
    scanBuffer = ''
    return
  }

  if (e.key.length === 1 && !e.ctrlKey && !e.altKey && !e.metaKey) {
    if (now - scanLastTime > SCAN_MAX_INTERVAL && scanBuffer.length > 0) {
      scanBuffer = '' // 超时重置
    }
    scanBuffer += e.key
    scanLastTime = now
  }
}, true)

// ── 加载遮罩 ──
setTimeout(() => {
  const loadingOverlay = document.getElementById('loading-overlay')
  if (loadingOverlay) {
    loadingOverlay.style.opacity = '0'
    loadingOverlay.style.transition = 'opacity 0.3s ease'
    setTimeout(() => {
      loadingOverlay.remove()
    }, 300)
  }
}, 100)
