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
