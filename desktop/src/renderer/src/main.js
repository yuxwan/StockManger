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

// 全局错误处理 — 防止单个页面崩溃导致整个应用卡死
app.config.errorHandler = (err, vm, info) => {
  console.error('[Page Error]', err, info)
}

app.mount('#app')
