<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { roleApi, menuApi } from '../../api'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const message = useMessage()

const roles = ref([])
const menus = ref([])
const loading = ref(false)

// ── 弹窗 ──
const modalShow = ref(false)
const modalMode = ref('create')
const editingId = ref(null)
const form = reactive({
  name: '',
  code: '',
  remark: '',
  status: 1,
  menuIds: []
})

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

async function fetchRoles() {
  loading.value = true
  try {
    roles.value = await roleApi.list()
  } catch { }
  loading.value = false
}

async function fetchMenus() {
  try {
    menus.value = await menuApi.tree()
  } catch { }
}

function openCreate() {
  modalMode.value = 'create'
  editingId.value = null
  form.name = ''
  form.code = ''
  form.remark = ''
  form.status = 1
  form.menuIds = []
  modalShow.value = true
}

async function openEdit(id) {
  try {
    const data = await roleApi.get(id)
    const role = data.role || data
    modalMode.value = 'edit'
    editingId.value = id
    form.name = role.name
    form.code = role.code
    form.remark = role.remark || ''
    form.status = role.status ?? 1
    form.menuIds = data.menuIds || []
    modalShow.value = true
  } catch {
    message.error('获取角色信息失败')
  }
}

async function handleSubmit() {
  if (!form.name || !form.code) {
    message.error('请填写角色名称和编码')
    return
  }
  try {
    if (modalMode.value === 'create') {
      await roleApi.create({
        name: form.name, code: form.code,
        remark: form.remark || undefined,
        status: form.status, menuIds: form.menuIds
      })
      message.success('角色已创建')
    } else {
      if (!editingId.value) {
        message.error('编辑状态异常，请重试')
        return
      }
      await roleApi.update(editingId.value, {
        name: form.name, code: form.code,
        remark: form.remark || undefined,
        status: form.status, menuIds: form.menuIds
      })
      message.success('角色已更新')
    }
    modalShow.value = false
    await fetchRoles()
  } catch (e) {
    message.error(e.response?.data?.msg || '操作失败')
  }
}

function handleDelete(id) {
  useConfirm('确认删除', '确定要删除该角色吗？此操作不可撤销。', async () => {
    try {
      await roleApi.delete(id)
      message.success('角色已删除')
      await fetchRoles()
    } catch {
      message.error('删除失败')
    }
  })
}

onMounted(() => {
  fetchRoles()
  fetchMenus()
})
</script>

<template>
  <div class="flex-1 flex flex-col gap-6">
    <!-- 顶栏 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">角色管理</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">管理系统角色和菜单权限分配</p>
      </div>
      <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-1.5" @click="openCreate">
        <Icon icon="mdi:plus" width="16" /> 新增角色
      </button>
    </div>

    <!-- 角色列表 -->
    <n-card size="small" :bordered="false" style="flex:1">
      <n-data-table :bordered="false" :loading="loading" size="small"
        :columns="[
          { title: 'ID', key: 'id' },
          { title: '角色名称', key: 'name' },
          { title: '角色编码', key: 'code' },
          { title: '备注', key: 'remark', ellipsis: { tooltip: true }, width: 200 },
          {
            title: '状态', key: 'status',
            render(row) {
              return h('span', {
                class: row.status === 1
                  ? 'text-xs font-semibold rounded-md text-emerald-600 dark:text-emerald-400'
                  : 'text-xs font-semibold rounded-md text-red-600 dark:text-red-400'
              }, row.status === 1 ? '启用' : '禁用')
            }
          },
          {
            title: '操作', key: 'actions', width: 130,
            render(row) {
              return h('div', { class: 'inline-flex items-center gap-0.5' }, [
                h('button', {
                  class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-500/10',
                  onClick: () => openEdit(row.id)
                }, [h(Icon, { icon: 'mdi:pencil-outline', width: 14 }), '编辑']),
                h('button', {
                  class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-red-500 hover:bg-red-500/10',
                  onClick: () => handleDelete(row.id)
                }, [h(Icon, { icon: 'mdi:delete-outline', width: 14 }), '删除'])
              ])
            }
          }
        ]" :data="roles" />
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <ConfirmDialog v-model:show="modalShow" :title="modalMode === 'create' ? '新增角色' : '编辑角色'" width="520px" :hide-default-footer="true">
      <div class="flex flex-col gap-4">
        <div class="flex gap-3">
          <div class="flex-1">
            <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">角色名称 *</label>
            <n-input v-model:value="form.name" placeholder="例如：管理员" />
          </div>
          <div class="flex-1">
            <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">角色编码 *</label>
            <n-input v-model:value="form.code" placeholder="例如：admin" />
          </div>
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">备注</label>
          <n-input v-model:value="form.remark" placeholder="角色描述" />
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">状态</label>
          <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">菜单权限</label>
          <n-tree v-model:checked-keys="form.menuIds" :data="menus" checkable multiple cascade
            :default-expand-all="true" key-field="id" label-field="name" style="max-height: 300px; overflow: auto;" />
        </div>
      </div>
      <template #footer>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" @click="modalShow = false">取消</button>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity" @click="handleSubmit">{{ modalMode === 'create' ? '创建' : '保存' }}</button>
      </template>
    </ConfirmDialog>

    <ConfirmDialog v-model:show="confirmShow" :title="confirmTitle" :content="confirmContent" type="error" icon-type="warning" @confirm="onConfirmOk" />
  </div>
</template>
