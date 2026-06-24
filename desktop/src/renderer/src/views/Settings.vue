<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { authApi, userApi } from '../api'
import message from '../utils/message'

const router = useRouter()
const showPwdForm = ref(false)
const submitting = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const appVersion = ref('')

onMounted(async () => {
  if (window.electronAPI?.getAppVersion) {
    appVersion.value = await window.electronAPI.getAppVersion()
  } else {
    appVersion.value = '1.0.0'
  }
})

function toggleDark() {
  document.documentElement.classList.toggle('dark')
}

async function logout() {
  try {
    await authApi.logout()
  } catch {}
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  router.push('/login')
}

function checkForUpdates() {
  const api = window.electronAPI
  if (api?.checkUpdate) {
    api.checkUpdate()
  } else {
    message.info('当前环境不支持自动更新')
  }
}

async function changePassword() {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  if (!oldPassword) return message.warning('请输入原密码')
  if (!newPassword) return message.warning('请输入新密码')
  if (newPassword.length < 6) return message.warning('新密码不能少于6位')
  if (newPassword !== confirmPassword) return message.warning('两次密码不一致')
  submitting.value = true
  try {
    await userApi.changePassword({ oldPassword, newPassword })
    message.success('密码修改成功')
    showPwdForm.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (err) {
    const msg = err?.response?.data?.msg || '修改失败'
    message.error(msg)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="max-w-lg mx-auto w-full flex flex-col gap-6">
    <h2 class="text-2xl font-body font-bold tracking-tight">设置</h2>

    <div class="p-6 rounded-2xl bg-surface-container dark:bg-[#252525] drop-shadow-center dark:drop-shadow-center-dark flex flex-col gap-4">
      <div class="flex items-center justify-between py-3">
        <div>
          <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface">主题模式</div>
          <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body mt-0.5">切换亮色/暗色主题</div>
        </div>
        <button
          class="px-4 py-2 rounded-lg text-xs font-body font-semibold bg-black dark:bg-white text-white dark:text-black"
          @click="toggleDark"
        >切换</button>
      </div>

      <div class="border-t border-outline-variant/50 dark:border-[#333]"></div>

      <div class="flex items-center justify-between py-3">
        <div>
          <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface">账号密码</div>
          <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body mt-0.5">修改登录密码</div>
        </div>
        <button
          class="px-4 py-2 rounded-lg text-xs font-body font-semibold bg-black dark:bg-white text-white dark:text-black"
          @click="showPwdForm = !showPwdForm"
        >{{ showPwdForm ? '取消' : '修改' }}</button>
      </div>

      <template v-if="showPwdForm">
        <div class="border-t border-outline-variant/50 dark:border-[#333]"></div>

        <div class="flex flex-col gap-3 pt-2">
          <input
            v-model="pwdForm.oldPassword"
            type="password"
            placeholder="原密码"
            class="px-3 py-2 rounded-lg text-sm bg-surface dark:bg-[#1a1a1a] text-on-surface dark:text-inverse-on-surface border border-outline-variant/50 dark:border-[#444] outline-none"
          />
          <input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="新密码（至少6位）"
            class="px-3 py-2 rounded-lg text-sm bg-surface dark:bg-[#1a1a1a] text-on-surface dark:text-inverse-on-surface border border-outline-variant/50 dark:border-[#444] outline-none"
          />
          <input
            v-model="pwdForm.confirmPassword"
            type="password"
            placeholder="确认新密码"
            class="px-3 py-2 rounded-lg text-sm bg-surface dark:bg-[#1a1a1a] text-on-surface dark:text-inverse-on-surface border border-outline-variant/50 dark:border-[#444] outline-none"
          />
          <button
            class="w-full py-2.5 rounded-lg text-sm font-body font-semibold bg-black dark:bg-white text-white dark:text-black hover:opacity-80 disabled:opacity-40 flex items-center justify-center gap-2"
            :disabled="submitting"
            @click="changePassword"
          >
            <Icon v-if="submitting" icon="mdi:loading" width="16" class="animate-spin" />
            {{ submitting ? '修改中...' : '确认修改' }}
          </button>
        </div>
      </template>

      <div class="border-t border-outline-variant/50 dark:border-[#333]"></div>

      <div class="flex items-center justify-between py-3">
        <div>
          <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface">版本</div>
          <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body mt-0.5">当前版本号</div>
        </div>
        <span class="text-xs text-on-surface-variant dark:text-gray-500 font-mono">v{{ appVersion }}</span>
      </div>

      <div class="border-t border-outline-variant/50 dark:border-[#333]"></div>

      <div class="flex items-center justify-between py-3">
        <div>
          <div class="text-sm font-body font-semibold text-on-surface dark:text-inverse-on-surface">检查更新</div>
          <div class="text-xs text-on-surface-variant dark:text-gray-400 font-body mt-0.5">检查是否有新版本可用</div>
        </div>
        <button
          class="px-4 py-2 rounded-lg text-xs font-body font-semibold bg-black dark:bg-white text-white dark:text-black hover:opacity-80"
          @click="checkForUpdates"
        >检查</button>
      </div>
    </div>

    <button
      class="flex items-center justify-center gap-2 py-3.5 rounded-xl border border-red-200 dark:border-red-900 text-sm font-body font-semibold text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
      @click="logout"
    >
      <Icon icon="mdi:logout" width="18" />
      退出登录
    </button>
  </div>
</template>
