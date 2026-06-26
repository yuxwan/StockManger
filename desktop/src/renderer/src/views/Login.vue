<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import message from '../utils/message'
import { Icon } from '@iconify/vue'
import { authApi } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const isMaximized = ref(false)
const electronAPI = window.electronAPI || { minimize: () => { }, maximize: () => { }, close: () => { }, isMaximized: () => { } }

function toggleDark() {
  const isDark = document.documentElement.classList.toggle('dark')
  try {
    localStorage.setItem('theme', isDark ? 'dark' : 'light')
  } catch {}
}

async function login() {
  if (!username.value) {
    message.warning('请输入账号')
    return
  }
  if (!password.value) {
    message.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    const res = await authApi.login({ username: username.value, password: password.value })
    localStorage.setItem('token', res.token)
    if (res.role) localStorage.setItem('userRole', res.role)
    if (res.username) localStorage.setItem('userName', res.username)
    if (res.nickname) localStorage.setItem('userNickname', res.nickname)
    router.push('/checkout')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  isMaximized.value = await electronAPI.isMaximized()
  window.addEventListener('resize', async () => {
    isMaximized.value = await electronAPI.isMaximized()
  })
})
</script>

<template>
  <div class="h-screen flex flex-col bg-surface dark:bg-[#1a1a1a] overflow-hidden">
    <!-- 拖拽区域 -->
    <div class="fixed top-0 left-0 right-0 h-8 z-40" style="-webkit-app-region: drag"></div>
    <!-- 窗口控制按钮 -->
    <div class="flex items-center gap-1 fixed top-1 right-2 z-50" style="-webkit-app-region: no-drag">
      <button class="w-9 h-[28px] flex items-center justify-center rounded-md hover:bg-black/10 dark:hover:bg-white/10"
        title="切换深色模式" @click="toggleDark">
        <Icon icon="mdi:weather-night" width="16" class="text-on-surface dark:text-inverse-on-surface" />
      </button>
      <div class="w-px h-4 bg-outline-variant/40 dark:bg-[#444] mx-0.5"></div>
      <button class="w-11 h-[28px] flex items-center justify-center rounded-md hover:bg-black/10 dark:hover:bg-white/10"
        title="最小化" @click="electronAPI.minimize">
        <Icon icon="mdi:window-minimize" width="14" class="text-on-surface dark:text-inverse-on-surface" />
      </button>
      <button class="w-11 h-[28px] flex items-center justify-center rounded-md hover:bg-black/10 dark:hover:bg-white/10"
        :title="isMaximized ? '还原' : '最大化'" @click="electronAPI.maximize">
        <Icon v-if="!isMaximized" icon="mdi:window-maximize" width="14"
          class="text-on-surface dark:text-inverse-on-surface" />
        <Icon v-else icon="mdi:window-restore" width="14" class="text-on-surface dark:text-inverse-on-surface" />
      </button>
      <button class="w-11 h-[28px] flex items-center justify-center rounded-md hover:bg-red-500" title="关闭"
        @click="electronAPI.close">
        <Icon icon="mdi:close" width="16" class="text-on-surface dark:text-inverse-on-surface" />
      </button>
    </div>

    <div class="flex-1 flex bg-surface dark:bg-[#1a1a1a]">
      <!-- 左侧品牌区 -->
      <div class="hidden lg:flex flex-1 flex-col items-center justify-center">
        <img src="/logo.png" alt="logo" class="w-24 h-24 object-contain drop-shadow-center dark:drop-shadow-center-dark" />
      </div>

      <!-- 右侧登录区 -->
      <div class="flex-1 flex items-center justify-center p-8">
        <div class="w-full max-w-sm">
          <div class="lg:hidden flex items-center gap-3 mb-10">
            <img src="/logo.png" alt="logo" class="w-8 h-8 object-contain" />
            <span class="text-lg font-body font-bold tracking-tight">STOCK</span>
          </div>

          <h2 class="text-2xl font-body font-bold tracking-tight text-on-surface dark:text-inverse-on-surface">登录
          </h2>
          <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">请输入账号密码登录系统</p>

          <div class="flex flex-col gap-4 mt-8">
            <div>
              <label
                class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">账号</label>
              <input v-model="username" type="text" placeholder="请输入账号"
                class="w-full h-11 px-4 rounded-lg bg-surface-container dark:bg-[#252525] text-sm text-on-surface dark:text-inverse-on-surface outline-none placeholder:text-on-surface-variant/40 dark:placeholder:text-gray-600 font-body" />
            </div>
            <div>
              <label
                class="text-xs font-body font-semibold uppercase tracking-wider text-on-surface-variant dark:text-gray-500 mb-2 block">密码</label>
              <input v-model="password" type="password" placeholder="请输入密码"
                class="w-full h-11 px-4 rounded-lg bg-surface-container dark:bg-[#252525] text-sm text-on-surface dark:text-inverse-on-surface outline-none placeholder:text-on-surface-variant/40 dark:placeholder:text-gray-600 font-body"
                @keydown.enter="login" />
            </div>
          </div>

          <button
            class="w-full h-11 rounded-lg bg-black dark:bg-white text-white dark:text-black font-body font-semibold text-sm uppercase tracking-widest mt-6 hover:opacity-80 transition-opacity flex items-center justify-center gap-2 disabled:opacity-50"
            :disabled="loading"
            @click="login">
            <Icon v-if="loading" icon="mdi:loading" width="16" class="animate-spin" />
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
