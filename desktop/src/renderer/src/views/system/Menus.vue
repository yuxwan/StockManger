<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { menuApi } from '../../api'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const message = useMessage()

const menus = ref([])
const loading = ref(false)

// ── 弹窗 ──
const modalShow = ref(false)
const modalMode = ref('create')
const editingId = ref(null)
const form = reactive({
  name: '',
  icon: '',
  path: '',
  permission: '',
  parentId: 0,
  sort: 0,
  type: 2
})

const typeOptions = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '按钮/权限', value: 3 }
]

// ── 图标选择器 ──
const iconPickerShow = ref(false)
const commonIcons = [
  'mdi:cart-outline', 'mdi:view-dashboard-outline', 'mdi:package-variant-closed',
  'mdi:receipt-text-outline', 'mdi:clipboard-text-clock-outline', 'mdi:cog-outline',
  'mdi:account-group-outline', 'mdi:shield-account-outline', 'mdi:menu-open',
  'mdi:home-outline', 'mdi:chart-box-outline', 'mdi:store-outline',
  'mdi:tag-outline', 'mdi:account-outline', 'mdi:bell-outline',
  'mdi:bookmark-outline', 'mdi:briefcase-outline', 'mdi:calendar-outline',
  'mdi:clock-outline', 'mdi:cloud-outline', 'mdi:database-outline',
  'mdi:email-outline', 'mdi:file-outline', 'mdi:flag-outline',
  'mdi:folder-outline', 'mdi:heart-outline', 'mdi:key-outline',
  'mdi:lightbulb-outline', 'mdi:link-outline', 'mdi:lock-outline',
  'mdi:map-marker-outline', 'mdi:music-outline', 'mdi:pencil-outline',
  'mdi:phone-outline', 'mdi:printer-outline', 'mdi:search-outline',
  'mdi:settings-outline', 'mdi:shopping-outline', 'mdi:star-outline',
  'mdi:thumb-up-outline', 'mdi:trash-can-outline', 'mdi:truck-outline',
  'mdi:wallet-outline', 'mdi:chart-pie', 'mdi:clipboard-list-outline',
  'mdi:credit-card-outline', 'mdi:cube-outline', 'mdi:download-outline',
  'mdi:finance', 'mdi:gift-outline', 'mdi:handshake-outline', 'mdi:inbox-outline',
  'mdi:layers-outline', 'mdi:message-outline', 'mdi:note-outline',
  'mdi:open-in-new', 'mdi:percent-outline', 'mdi:refresh-outline',
  'mdi:scale-balance', 'mdi:signal-cellular-outline', 'mdi:table-outline',
  'mdi:upload-outline', 'mdi:video-outline', 'mdi:web-outline'
]

// ── 确认弹窗 ──
const confirmShow = ref(false)
const confirmTitle = ref('')
const confirmContent = ref('')
let confirmCallback = null

function useConfirm(title, content, callback) {
  confirmTitle.value = title
  confirmContent.value = content
  confirmCallback = callback
  confirmShow.value = true
}

function onConfirmOk() {
  confirmShow.value = false
  confirmCallback?.()
}

async function fetchMenus() {
  loading.value = true
  try {
    const data = await menuApi.tree()
    menus.value = cleanTree(data)
  } catch { }
  loading.value = false
}

function cleanTree(nodes) {
  return (nodes || []).map(n => ({
    ...n,
    children: n.children && n.children.length ? cleanTree(n.children) : undefined
  }))
}

// 获取扁平菜单列表（用于父菜单选择）
const flatMenus = ref([])
async function fetchFlatMenus() {
  try {
    flatMenus.value = await menuApi.list()
  } catch { }
}

// 构建父菜单选择选项
const parentOptions = computed(() => {
  // 只允许目录和菜单作为父级
  return flatMenus.value
    .filter(m => m.type !== 3 && m.id !== editingId.value)
    .map(m => ({ label: m.name, value: m.id }))
})


function openCreate(parentId) {
  modalMode.value = 'create'
  editingId.value = null
  form.name = ''
  form.icon = ''
  form.path = ''
  form.permission = ''
  form.parentId = parentId || 0
  form.sort = 0
  form.type = parentId ? 2 : 1
  modalShow.value = true
}

async function openEdit(id) {
  try {
    const menu = await menuApi.get(id)
    modalMode.value = 'edit'
    editingId.value = id
    form.name = menu.name
    form.icon = menu.icon || ''
    form.path = menu.path || ''
    form.permission = menu.permission || ''
    form.parentId = menu.parentId || 0
    form.sort = menu.sort || 0
    form.type = menu.type || 2
    modalShow.value = true
  } catch {
    message.error('获取菜单信息失败')
  }
}

async function handleSubmit() {
  if (!form.name) {
    message.error('请输入菜单名称')
    return
  }
  try {
    const payload = {
      name: form.name, icon: form.icon || undefined,
      path: form.path || undefined, permission: form.permission || undefined,
      parentId: form.parentId || 0, sort: form.sort, type: form.type
    }
    if (modalMode.value === 'create') {
      await menuApi.create(payload)
      message.success('菜单已创建')
    } else {
      if (!editingId.value) { message.error('编辑状态异常，请重试'); return }
      await menuApi.update(editingId.value, payload)
      message.success('菜单已更新')
    }
    modalShow.value = false
    await fetchMenus()
    await fetchFlatMenus()
  } catch {
  }
}

function handleDelete(id) {
  useConfirm('确认删除', '确定要删除该菜单及其子菜单吗？此操作不可撤销。', async () => {
    try {
      await menuApi.delete(id)
      message.success('菜单已删除')
      await fetchMenus()
      await fetchFlatMenus()
    } catch {
    }
  })
}

onMounted(() => {
  fetchMenus()
  fetchFlatMenus()
})

function renderMenuSuffix({ option }) {
  return h('div', { class: 'flex items-center gap-1' }, [
    h('button', {
      class: 'inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10',
      onClick: (e) => { e.stopPropagation(); openCreate(option.key ?? option.id) }
    }, [h(Icon, { icon: 'mdi:plus', width: 12 }), '新增']),
    h('button', {
      class: 'inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-500/10',
      onClick: (e) => { e.stopPropagation(); openEdit(option.key ?? option.id) }
    }, [h(Icon, { icon: 'mdi:pencil-outline', width: 12 }), '编辑']),
    h('button', {
      class: 'inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded text-xs font-semibold text-red-500 hover:bg-red-500/10',
      onClick: (e) => { e.stopPropagation(); handleDelete(option.key ?? option.id) }
    }, [h(Icon, { icon: 'mdi:delete-outline', width: 12 }), '删除'])
  ])
}
</script>

<template>
  <div class="flex-1 flex flex-col gap-6">
    <!-- 顶栏 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">菜单管理</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">配置系统导航菜单和权限标识</p>
      </div>
      <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-1.5" @click="openCreate(0)">
        <Icon icon="mdi:plus" width="16" /> 新增菜单
      </button>
    </div>

    <!-- 菜单树 -->
    <n-card  style="flex:1">
      <div v-if="loading" class="flex items-center justify-center py-12">
        <Icon icon="mdi:loading" width="24" class="animate-spin text-on-surface-variant dark:text-gray-400" />
      </div>
      <div v-else-if="menus.length === 0" class="flex items-center justify-center py-12 text-sm text-on-surface-variant dark:text-gray-500">
        暂无菜单，点击上方按钮新增
      </div>
      <n-tree v-else :data="menus" :default-expand-all="true" block-line key-field="id" label-field="name"
        :render-suffix="renderMenuSuffix" children-field="children"
        style="padding: 8px 0;" />
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <ConfirmDialog v-model:show="modalShow" :title="modalMode === 'create' ? '新增菜单' : '编辑菜单'" width="500px" :hide-default-footer="true">
      <div class="flex flex-col gap-4">
        <div class="flex gap-3">
          <div class="flex-1">
            <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">菜单名称 *</label>
            <n-input v-model:value="form.name" placeholder="名称" />
          </div>
          <div class="flex-1">
            <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">类型</label>
            <n-select v-model:value="form.type" :options="typeOptions" />
          </div>
        </div>
        <div class="flex gap-3">
          <div class="flex-[3]">
            <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">图标</label>
            <div class="flex gap-2 items-center">
              <div class="flex-1 relative">
                <n-input v-model:value="form.icon" placeholder="mdi:home" />
                <div v-if="form.icon" class="absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none">
                  <Icon :icon="form.icon" width="18" class="text-on-surface-variant dark:text-gray-400" />
                </div>
              </div>
              <n-button size="small" secondary @click="iconPickerShow = true">
                <template #icon><Icon icon="mdi:apps" width="16" /></template>
                浏览
              </n-button>
            </div>
          </div>
          <div class="flex-1">
            <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">排序</label>
            <n-input-number v-model:value="form.sort" :min="0" style="width:100%" />
          </div>
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">路由路径</label>
          <n-input v-model:value="form.path" placeholder="/example" />
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">权限标识</label>
          <n-input v-model:value="form.permission" placeholder="system:example" />
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">父菜单</label>
          <n-select v-model:value="form.parentId" :options="[{ label: '顶级菜单', value: 0 }, ...parentOptions]" clearable />
        </div>
      </div>
      <template #footer>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" @click="modalShow = false">取消</button>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity" @click="handleSubmit">{{ modalMode === 'create' ? '创建' : '保存' }}</button>
      </template>
    </ConfirmDialog>

    <!-- 图标选择器 -->
    <ConfirmDialog v-model:show="iconPickerShow" title="选择图标" width="640px" :hide-default-footer="true">
      <div class="grid grid-cols-8 gap-2 max-h-80 overflow-y-auto">
        <div v-for="icon in commonIcons" :key="icon"
          class="flex flex-col items-center gap-1 p-2 rounded-lg cursor-pointer border border-transparent hover:border-gray-300 dark:hover:border-gray-600 transition-colors"
          :class="form.icon === icon ? 'bg-black/5 dark:bg-white/10 border-gray-400 dark:border-gray-500' : 'hover:bg-gray-50 dark:hover:bg-white/5'"
          @click="form.icon = icon; iconPickerShow = false">
          <Icon :icon="icon" width="24" class="text-on-surface-variant dark:text-gray-300" />
          <span class="text-[10px] text-center text-on-surface-variant dark:text-gray-400 leading-tight truncate w-full">{{ icon.replace('mdi:', '') }}</span>
        </div>
      </div>
      <template #footer>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" @click="iconPickerShow = false">取消</button>
      </template>
    </ConfirmDialog>

    <ConfirmDialog v-model:show="confirmShow" :title="confirmTitle" :content="confirmContent" type="error" icon-type="warning" @confirm="onConfirmOk" />
  </div>
</template>
