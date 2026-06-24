<script setup>
import { ref, h, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { menuApi } from '../api'

const route = useRoute()
const router = useRouter()
const isMaximized = ref(false)
const isDark = ref(false)
const sidebarCollapsed = ref(true)
const refreshTick = ref(0)

function refreshPage() {
  refreshTick.value++
}
const electronAPI = window.electronAPI || { minimize: () => { }, maximize: () => { }, close: () => { }, isMaximized: async () => false }

function renderIcon(iconName) {
  return () => h(Icon, { icon: iconName, width: 20 })
}

const selectedKey = ref(route.path)
const menuOptions = ref([])

// 路由变化时匹配当前菜单高亮
function syncSelectedKey() {
  const path = route.path
  if (menuOptions.value.length === 0) {
    selectedKey.value = path
    return
  }
  const allKeys = flattenKeys(menuOptions.value)
  const match = allKeys.filter(k => path.startsWith(k)).sort((a, b) => b.length - a.length)[0]
  selectedKey.value = match || path
}
watch(() => route.path, syncSelectedKey, { immediate: true })
// 菜单加载完成后重新同步高亮
watch(menuOptions, syncSelectedKey, { immediate: false })

function flattenKeys(items) {
  const keys = []
  for (const item of items) {
    if (item.key && !item.key.startsWith('group-')) keys.push(item.key)
    if (item.children) keys.push(...flattenKeys(item.children))
  }
  return keys
}

function handleMenuUpdate(key) {
  if (key && !key.startsWith('group-')) {
    selectedKey.value = key
    router.push(key)
  }
}

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
}

async function checkMaximized() {
  isMaximized.value = await electronAPI.isMaximized()
}

// 默认菜单选项（API 加载失败时的回退）
const defaultMenuOptions = [
  { key: '/checkout', icon: renderIcon('mdi:cart-outline'), label: '收银台' },
  { key: '/dashboard', icon: renderIcon('mdi:view-dashboard-outline'), label: '仪表盘' },
  { key: '/products', icon: renderIcon('mdi:package-variant-closed'), label: '商品管理' },
  { key: '/orders', icon: renderIcon('mdi:receipt-text-outline'), label: '订单管理' },
  { key: '/operations', icon: renderIcon('mdi:clipboard-text-clock-outline'), label: '操作日志' }
]

function mapMenu(menu) {
  const item = {
    key: menu.type === 1 ? 'group-' + menu.id : menu.path,
    label: menu.name,
    icon: menu.icon ? renderIcon(menu.icon) : undefined
  }
  if (menu.children && menu.children.length > 0) {
    item.children = menu.children.map(mapMenu)
  }
  return item
}

onMounted(async () => {
  checkMaximized()
  window.addEventListener('resize', checkMaximized)
  try {
    const menus = await menuApi.menus()
    if (menus && menus.length > 0) {
      menuOptions.value = menus.map(mapMenu)
    } else {
      menuOptions.value = defaultMenuOptions
    }
  } catch (e) {
    console.error('加载菜单失败:', e)
    menuOptions.value = defaultMenuOptions
  }
})
</script>

<template>
  <n-layout style="height: 100vh" class="bg-surface dark:bg-[#1a1a1a] text-on-surface dark:text-inverse-on-surface">
    <!-- 标题栏 -->
    <n-layout-header style="height: 40px; -webkit-app-region: drag"
      class="flex items-center justify-between bg-surface-container dark:bg-[#252525] text-on-surface dark:text-inverse-on-surface select-none shrink-0 border-b border-outline-variant/50 dark:border-[#333]">
      <div class="flex items-center gap-2 pl-4 text-sm font-body font-semibold tracking-wider uppercase">
        <span>STOCK</span>
      </div>
      <div class="flex h-full items-center gap-1 pr-2" style="-webkit-app-region: no-drag">
        <n-tooltip trigger="hover" :placement="'bottom'" :delay="500">
          <template #trigger>
            <button
              class="w-9 h-[28px] flex items-center justify-center rounded-md hover:bg-black/10 dark:hover:bg-white/10"
              @click="refreshPage">
              <Icon icon="mdi:refresh" width="16" />
            </button>
          </template>
          <span>刷新页面</span>
        </n-tooltip>
        <n-tooltip trigger="hover" :placement="'bottom'" :delay="500">
          <template #trigger>
            <button
              class="w-9 h-[28px] flex items-center justify-center rounded-md hover:bg-black/10 dark:hover:bg-white/10"
              @click="toggleTheme">
              <Icon v-if="!isDark" icon="mdi:weather-night" width="16" />
              <Icon v-else icon="mdi:weather-sunny" width="16" />
            </button>
          </template>
          <span>{{ isDark ? '亮色模式' : '暗色模式' }}</span>
        </n-tooltip>
        <div class="w-px h-4 bg-outline-variant/40 dark:bg-[#444] mx-1"></div>
        <n-tooltip trigger="hover" :placement="'bottom'" :delay="500">
          <template #trigger>
            <button
              class="w-11 h-[28px] flex items-center justify-center rounded-md hover:bg-black/10 dark:hover:bg-white/10"
              @click="electronAPI.minimize">
              <Icon icon="mdi:window-minimize" width="14" />
            </button>
          </template>
          <span>最小化</span>
        </n-tooltip>
        <n-tooltip trigger="hover" :placement="'bottom'" :delay="500">
          <template #trigger>
            <button
              class="w-11 h-[28px] flex items-center justify-center rounded-md hover:bg-black/10 dark:hover:bg-white/10"
              @click="electronAPI.maximize">
              <Icon v-if="!isMaximized" icon="mdi:window-maximize" width="14" />
              <Icon v-else icon="mdi:window-restore" width="14" />
            </button>
          </template>
          <span>{{ isMaximized ? '还原' : '最大化' }}</span>
        </n-tooltip>
        <n-tooltip trigger="hover" :placement="'bottom'" :delay="500">
          <template #trigger>
            <button class="w-11 h-[28px] flex items-center justify-center rounded-md hover:bg-red-500"
              @click="electronAPI.close">
              <Icon icon="mdi:close" width="16" />
            </button>
          </template>
          <span>关闭</span>
        </n-tooltip>
      </div>
    </n-layout-header>

    <n-layout has-sider>
      <!-- 侧边栏 -->
      <n-layout-sider v-model:collapsed="sidebarCollapsed" :width="224" :collapsed-width="64" collapse-mode="width"
        :native-scrollbar="false" class="bg-surface dark:bg-[#1a1a1a]"
        content-style="display:flex;flex-direction:column;">
        <div style="height: calc(100vh - 40px); display: flex; flex-direction: column;">
          <n-menu
            :value="selectedKey"
            :collapsed="sidebarCollapsed"
            :collapsed-width="64"
            :collapsed-icon-size="20"
            :options="menuOptions"
            :indent="18"
            @update:value="handleMenuUpdate"
            class="flex-1 pt-2"
          />

          <div class="border-t border-outline-variant/50 dark:border-[#333] p-3 flex flex-col gap-1">
            <router-link to="/settings"
              class="group flex items-center justify-start p-2.5 no-underline rounded-xl text-sm font-medium w-full relative"
              :class="route.path === '/settings' ? 'bg-black dark:bg-white text-white dark:text-black' : 'text-on-surface dark:text-inverse-on-surface hover:bg-surface-container dark:hover:bg-white/10'">
              <Icon icon="mdi:cog-outline" width="20" class="shrink-0" />
              <span
                class="overflow-hidden whitespace-nowrap transition-[max-width,opacity,margin] duration-300 ease-in-out"
                :class="sidebarCollapsed ? 'max-w-0 opacity-0 ml-0' : 'max-w-[200px] opacity-100 ml-3'">设置</span>
              <div v-if="sidebarCollapsed"
                class="fixed left-[64px] px-3 py-1.5 rounded-lg text-xs font-body font-semibold whitespace-nowrap z-[100] pointer-events-none opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150"
                :class="route.path === '/settings'
                  ? 'bg-black dark:bg-white text-white dark:text-black'
                  : 'bg-surface-container dark:bg-[#333] text-on-surface dark:text-inverse-on-surface shadow-lg dark:shadow-black/30'">
                设置
              </div>
            </router-link>

            <button
              class="flex items-center justify-start p-2.5 w-full rounded-xl text-sm font-medium text-on-surface dark:text-inverse-on-surface hover:bg-surface-container dark:hover:bg-white/10 mt-1"
              :title="sidebarCollapsed ? '展开' : '收起'" @click="sidebarCollapsed = !sidebarCollapsed">
              <Icon :icon="sidebarCollapsed ? 'mdi:chevron-right' : 'mdi:chevron-left'" width="20" class="shrink-0" />
              <span
                class="overflow-hidden whitespace-nowrap transition-[max-width,opacity,margin] duration-300 ease-in-out"
                :class="sidebarCollapsed ? 'max-w-0 opacity-0 ml-0' : 'max-w-[200px] opacity-100 ml-3'">收起</span>
            </button>
          </div>
        </div>
      </n-layout-sider>

      <!-- 主内容 -->
      <n-layout-content class="bg-surface dark:bg-[#1a1a1a]">
        <main class="flex-1 flex flex-col p-8 overflow-auto" style="min-height: 0; height: calc(100vh - 40px);">
          <router-view v-slot="{ Component }">
            <Transition name="page" mode="out-in">
              <component :is="Component" :key="`${$route.fullPath}_${refreshTick}`" />
            </Transition>
          </router-view>
        </main>
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<style scoped>
/* page transition */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>

<style>
.n-menu-item-content.n-menu-item-content--selected:hover::before {
  background-color: var(--n-item-color-active, #000000) !important;
}
.n-menu-item-content.n-menu-item-content--selected:hover .n-menu-item-content-header,
.n-menu-item-content.n-menu-item-content--selected:hover .n-menu-item-content__icon,
.n-menu-item-content.n-menu-item-content--selected:hover .n-menu-item-content__arrow {
  color: var(--n-item-text-color-active, #ffffff) !important;
}

.dark .n-menu-item-content.n-menu-item-content--selected:hover::before {
  background-color: #ffffff !important;
}
.dark .n-menu-item-content.n-menu-item-content--selected:hover .n-menu-item-content-header,
.dark .n-menu-item-content.n-menu-item-content--selected:hover .n-menu-item-content__icon,
.dark .n-menu-item-content.n-menu-item-content--selected:hover .n-menu-item-content__arrow {
  color: #1a1a1a !important;
}

/* 折叠态选中背景（亮色黑 / 暗色白） */
.n-menu.n-menu--collapsed .n-menu-item-content.n-menu-item-content--selected::before {
  background: #000000 !important;
}
.dark .n-menu.n-menu--collapsed .n-menu-item-content.n-menu-item-content--selected::before {
  background: #ffffff !important;
}
</style>
