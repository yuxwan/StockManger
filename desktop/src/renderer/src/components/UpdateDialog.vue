<template>
  <Teleport to="body">
    <Transition name="confirm">
      <div v-if="visible" class="fixed inset-0 z-[9999] flex items-center justify-center">
        <div class="fixed inset-0 bg-black/30 dark:bg-black/50" @click="onClose" />
        <div
          class="relative rounded-2xl bg-surface dark:bg-[#252525] shadow-xl border border-outline-variant/20 dark:border-[#333] w-[400px] overflow-hidden"
        >
          <div class="px-6 pt-6 pb-4">
            <!-- 标题 -->
            <div class="flex items-center gap-3 mb-4">
              <div
                class="w-10 h-10 rounded flex items-center justify-center shrink-0"
                :class="iconBgClass"
              >
                <Icon
                  v-if="status === 'checking'"
                  icon="mdi:loading"
                  width="20"
                  class="animate-spin text-blue-500"
                />
                <Icon
                  v-else-if="status === 'available'"
                  icon="mdi:download"
                  width="20"
                  class="text-blue-600 dark:text-blue-400"
                />
                <Icon
                  v-else-if="status === 'downloading'"
                  icon="mdi:download"
                  width="20"
                  class="text-blue-600 dark:text-blue-400"
                />
                <Icon
                  v-else-if="status === 'downloaded'"
                  icon="mdi:check-circle-outline"
                  width="20"
                  class="text-green-600 dark:text-green-400"
                />
                <Icon
                  v-else-if="status === 'no-update'"
                  icon="mdi:check"
                  width="20"
                  class="text-green-600 dark:text-green-400"
                />
                <Icon
                  v-else-if="status === 'error'"
                  icon="mdi:alert-circle-outline"
                  width="20"
                  class="text-red-600 dark:text-red-400"
                />
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="text-base font-body font-bold text-on-surface dark:text-inverse-on-surface">
                  {{ titleText }}
                </h3>
                <p class="mt-0.5 text-sm text-on-surface-variant dark:text-gray-400">
                  {{ subText }}
                </p>
              </div>
            </div>

            <!-- 进度条（下载中） -->
            <div v-if="status === 'downloading'" class="mb-4">
              <div class="w-full h-2 bg-black/10 dark:bg-white/10 rounded-full overflow-hidden">
                <div
                  class="h-full bg-blue-500 rounded-full transition-all duration-300 ease-out"
                  :style="{ width: progressPercent + '%' }"
                />
              </div>
              <p class="mt-1.5 text-xs text-on-surface-variant dark:text-gray-400 text-right">
                {{ progressPercent }}% · {{ progressSpeed }}
              </p>
            </div>

            <!-- 版本信息 / 发布说明 -->
            <div v-if="updateInfo && (status === 'available' || status === 'downloaded')" class="mb-4">
              <div class="rounded bg-black/5 dark:bg-white/5 p-3">
                <p class="text-xs font-semibold text-on-surface-variant dark:text-gray-400 mb-1">版本 {{ updateInfo.version }}</p>
                <p v-if="updateInfo.releaseNotes" class="text-xs text-on-surface dark:text-inverse-on-surface leading-relaxed whitespace-pre-wrap max-h-[120px] overflow-y-auto">
                  {{ typeof updateInfo.releaseNotes === 'string' ? updateInfo.releaseNotes : updateInfo.releaseNotes?.[0]?.note || '暂无更新说明' }}
                </p>
                <p v-else class="text-xs text-on-surface-variant dark:text-gray-400">暂无更新说明</p>
              </div>
            </div>

            <!-- 下载完成提示 -->
            <div v-if="status === 'downloaded' && updateInfo?.savedPath" class="mb-4">
              <div class="rounded bg-emerald-50 dark:bg-emerald-950/20 p-3">
                <p class="text-xs text-emerald-600 dark:text-emerald-400 font-semibold mb-1">安装包已保存到下载目录</p>
                <p class="text-xs text-on-surface-variant dark:text-gray-400 font-mono truncate">{{ updateInfo.savedPath }}</p>
              </div>
            </div>

            <!-- 错误信息 -->
            <div v-if="status === 'error' && errorMsg" class="mb-4">
              <div class="rounded bg-red-50 dark:bg-red-950/20 p-3">
                <p class="text-xs text-red-600 dark:text-red-400 leading-relaxed">{{ errorMsg }}</p>
              </div>
            </div>
          </div>

          <!-- 底部按钮 -->
          <div class="flex items-center justify-end gap-2 px-6 pb-6">
            <button
              v-if="status === 'available' || status === 'error' || status === 'downloaded'"
              class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
              @click="onClose"
            >
              稍后再说
            </button>
            <button
              v-if="status === 'no-update'"
              class="h-9 px-5 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity"
              @click="onClose"
            >
              知道了
            </button>
            <button
              v-if="status === 'available'"
              class="h-9 px-5 rounded text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-2"
              @click="handleDownload"
            >
              <Icon icon="mdi:download" width="14" />
              立即下载
            </button>
            
            <button
              v-if="status === 'downloaded'"
              class="h-9 px-5 rounded text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-2"
              @click="handleInstall"
            >
              <Icon icon="mdi:check" width="14" />
              立即安装
            </button>
            <button
              v-if="status === 'downloading'"
              disabled
              class="h-9 px-5 rounded text-sm font-body font-semibold text-white bg-black/50 dark:bg-white/50 dark:text-black/50 cursor-not-allowed flex items-center gap-2"
            >
              <Icon icon="mdi:loading" width="14" class="animate-spin" />
              下载中...
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'

const electronAPI = window.electronAPI
const currentVersion = ref('')

// 简单 semver 比较，返回 true 如果 a > b
function isNewerVersion(a, b) {
  const pa = a.split('.').map(Number)
  const pb = b.split('.').map(Number)
  for (let i = 0; i < 3; i++) {
    if ((pa[i] || 0) !== (pb[i] || 0)) return (pa[i] || 0) > (pb[i] || 0)
  }
  return false
}

// 状态：checking | available | downloading | downloaded | error | no-update | idle
const status = ref('idle')
const updateInfo = ref(null)
const errorMsg = ref('')
const progressPercent = ref(0)
const progressSpeed = ref('')
const visible = ref(false)

const titleText = computed(() => {
  switch (status.value) {
    case 'checking': return '正在检查更新'
    case 'available': return '发现新版本'
    case 'downloading': return '正在下载更新'
    case 'downloaded': return '更新下载完成'
    case 'no-update': return '已是最新版本'
    case 'error': return '检查更新失败'
    default: return '软件更新'
  }
})

const subText = computed(() => {
  switch (status.value) {
    case 'checking': return '请稍候...'
    case 'available': return `v${updateInfo.value?.version || ''} 可供下载`
    case 'downloading': return '正在下载最新版本...'
    case 'downloaded': return '已准备就绪，点击安装即可重启应用'
    case 'no-update': return '当前已是最新版本，无需更新'
    case 'error': return '请稍后重试或手动下载更新'
    default: return ''
  }
})

const iconBgClass = computed(() => {
  switch (status.value) {
    case 'checking': return 'bg-blue-100 dark:bg-blue-950/30'
    case 'available': return 'bg-blue-100 dark:bg-blue-950/30'
    case 'downloading': return 'bg-blue-100 dark:bg-blue-950/30'
    case 'downloaded': return 'bg-green-100 dark:bg-green-950/30'
    case 'no-update': return 'bg-green-100 dark:bg-green-950/30'
    case 'error': return 'bg-red-100 dark:bg-red-950/30'
    default: return 'bg-gray-100 dark:bg-gray-800'
  }
})

function formatSpeed(bytesPerSecond) {
  if (!bytesPerSecond) return ''
  if (bytesPerSecond < 1024) return Math.round(bytesPerSecond) + ' B/s'
  if (bytesPerSecond < 1024 * 1024) return (bytesPerSecond / 1024).toFixed(1) + ' KB/s'
  return (bytesPerSecond / (1024 * 1024)).toFixed(1) + ' MB/s'
}

function handleDownload() {
  if (electronAPI?.downloadUpdate) {
    status.value = 'downloading'
    electronAPI.downloadUpdate()
  }
}

function handleInstall() {
  if (electronAPI?.installUpdate) {
    electronAPI.installUpdate()
  }
}

function onClose() {
  visible.value = false
  status.value = 'idle'
}

function setupListeners() {
  if (!electronAPI) return

  electronAPI.onUpdateChecking((data) => {
    // 自动检查时连"正在检查更新"也不弹
    if (!data?.manual) return
    status.value = 'checking'
    updateInfo.value = null
    errorMsg.value = ''
    progressPercent.value = 0
    visible.value = true
  })

  electronAPI.onUpdateAvailable((info) => {
    const serverVer = info?.version || ''
    const isManual = info?.manual === true

    if (serverVer && currentVersion.value && !isNewerVersion(serverVer, currentVersion.value)) {
      // 服务器版本不大于当前版本
      if (isManual) {
        // 手动检查 → 显示"已是最新版本"
        status.value = 'no-update'
        updateInfo.value = null
        errorMsg.value = ''
        visible.value = true
      }
      // 自动检查 → 静默
      return
    }
    status.value = 'available'
    updateInfo.value = info
    visible.value = true
  })

  electronAPI.onUpdateNotAvailable((data) => {
    // 无更新
    if (data?.manual) {
      // 手动检查 → 显示"已是最新版本"
      status.value = 'no-update'
      updateInfo.value = null
      errorMsg.value = ''
      visible.value = true
    }
    // 自动检查 → 静默
  })

  electronAPI.onUpdateProgress((progress) => {
    status.value = 'downloading'
    progressPercent.value = Math.round(progress.percent || 0)
    progressSpeed.value = formatSpeed(progress.bytesPerSecond)
  })

  electronAPI.onUpdateDownloaded((info) => {
    status.value = 'downloaded'
    updateInfo.value = info
  })

  electronAPI.onUpdateError((msg) => {
    status.value = 'error'
    errorMsg.value = msg
    visible.value = true
  })
}

onMounted(async () => {
  // 获取当前应用版本
  if (electronAPI?.getAppVersion) {
    currentVersion.value = await electronAPI.getAppVersion()
  }
  setupListeners()
})

onUnmounted(() => {
  if (electronAPI?.removeAllUpdateListeners) {
    electronAPI.removeAllUpdateListeners()
  }
})
</script>

<style scoped>
.confirm-enter-active {
  transition: opacity 200ms ease;
}
.confirm-leave-active {
  transition: opacity 150ms ease;
}
.confirm-enter-from,
.confirm-leave-to {
  opacity: 0;
}
.confirm-enter-active > div:last-child {
  transition: transform 200ms cubic-bezier(0.34, 1.56, 0.64, 1), opacity 200ms ease;
}
.confirm-leave-active > div:last-child {
  transition: transform 150ms ease, opacity 150ms ease;
}
.confirm-enter-from > div:last-child {
  transform: scale(0.92);
  opacity: 0;
}
.confirm-leave-to > div:last-child {
  transform: scale(0.92);
  opacity: 0;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.animate-spin {
  animation: spin 1s linear infinite;
}
</style>
