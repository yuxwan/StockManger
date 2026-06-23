<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { systemUserApi, roleApi } from '../../api'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const message = useMessage()

const users = ref([])
const roles = ref([])
const loading = ref(false)
const searchQuery = ref('')

// ── 弹窗 ──
const modalShow = ref(false)
const modalMode = ref('create') // create | edit
const editingId = ref(null)
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  status: 1,
  roleIds: []
})

// ── 角色分配弹窗 ──
const roleModalShow = ref(false)
const roleTargetUser = ref(null)
const selectedRoleIds = ref([])

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

async function fetchUsers() {
  loading.value = true
  try {
    users.value = await systemUserApi.list()
  } catch { }
  loading.value = false
}

async function fetchRoles() {
  try {
    roles.value = await roleApi.list()
  } catch { }
}

const filteredUsers = computed(() => {
  if (!searchQuery.value) return users.value
  const q = searchQuery.value.toLowerCase()
  return users.value.filter(u =>
    u.username?.toLowerCase().includes(q) ||
    u.nickname?.toLowerCase().includes(q)
  )
})

function openCreate() {
  modalMode.value = 'create'
  editingId.value = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.status = 1
  form.roleIds = []
  modalShow.value = true
}

async function openEdit(id) {
  try {
    const data = await systemUserApi.get(id)
    const user = data.user || data
    modalMode.value = 'edit'
    editingId.value = id
    form.username = user.username
    form.password = ''
    form.nickname = user.nickname || ''
    form.status = user.status ?? 1
    form.roleIds = data.roleIds || []
    modalShow.value = true
  } catch {
    message.error('获取用户信息失败')
  }
}

async function handleSubmit() {
  if (!form.username) {
    message.error('请输入用户名')
    return
  }
  if (modalMode.value === 'create' && !form.password) {
    message.error('请输入密码')
    return
  }
  try {
    if (modalMode.value === 'create') {
      await systemUserApi.create({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined,
        status: form.status,
        roleIds: form.roleIds.length > 0 ? form.roleIds : undefined
      })
      message.success('用户已创建')
    } else {
      const payload = { username: form.username, nickname: form.nickname, status: form.status, roleIds: form.roleIds }
      if (form.password) payload.password = form.password
      await systemUserApi.update(editingId.value, payload)
      message.success('用户已更新')
    }
    modalShow.value = false
    await fetchUsers()
  } catch (e) {
    message.error(e.response?.data?.msg || '操作失败')
  }
}

function handleDelete(id) {
  useConfirm('确认删除', '确定要删除该用户吗？此操作不可撤销。', async () => {
    try {
      await systemUserApi.delete(id)
      message.success('用户已删除')
      await fetchUsers()
    } catch {
      message.error('删除失败')
    }
  })
}

async function toggleStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await systemUserApi.toggleStatus(user.id, newStatus)
    user.status = newStatus
    message.success(newStatus === 1 ? '用户已启用' : '用户已禁用')
  } catch {
    message.error('操作失败')
  }
}

// ── 角色分配 ──
function openRoleAssign(user) {
  roleTargetUser.value = user
  selectedRoleIds.value = []
  fetchRoles()
  // 获取用户已有角色
  systemUserApi.get(user.id).then(data => {
    selectedRoleIds.value = data.roleIds || []
  }).catch(() => {})
  roleModalShow.value = true
}

async function saveRoleAssign() {
  try {
    await systemUserApi.update(roleTargetUser.value.id, { roleIds: selectedRoleIds.value })
    message.success('角色已分配')
    roleModalShow.value = false
    await fetchUsers()
  } catch {
    message.error('分配失败')
  }
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>

<template>
  <div class="flex-1 flex flex-col gap-6">
    <!-- 顶栏 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">用户管理</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">管理系统用户账号和角色分配</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="relative">
          <Icon icon="mdi:magnify" width="16"
            class="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50 dark:text-gray-500" />
          <input v-model="searchQuery" type="text" placeholder="搜索用户名..."
            class="w-52 h-9 pl-9 pr-3 rounded-xl bg-surface dark:bg-[#1a1a1a] text-sm text-on-surface dark:text-inverse-on-surface outline-none placeholder:text-on-surface-variant/40 dark:placeholder:text-gray-600 font-body" />
        </div>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-1.5" @click="openCreate">
          <Icon icon="mdi:plus" width="16" /> 新增用户
        </button>
      </div>
    </div>

    <!-- 用户列表 -->
    <n-card size="small" :bordered="false" style="flex:1">
      <n-data-table :bordered="false" :loading="loading" size="small"
        :columns="[
          { title: 'ID', key: 'id' },
          { title: '用户名', key: 'username' },
          { title: '昵称', key: 'nickname' },
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
            title: '创建时间', key: 'createTime',
            render(row) { return row.createTime || '-' }
          },
          {
            title: '操作', key: 'actions', width: 230,
            render(row) {
              return h('div', { class: 'inline-flex items-center gap-0.5' }, [
                h('button', {
                  class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-500/10',
                  onClick: () => openEdit(row.id)
                }, [h(Icon, { icon: 'mdi:pencil-outline', width: 14 }), '编辑']),
                h('button', {
                  class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-purple-600 dark:text-purple-400 hover:bg-purple-500/10',
                  onClick: () => openRoleAssign(row)
                }, [h(Icon, { icon: 'mdi:shield-account-outline', width: 14 }), '角色']),
                h('button', {
                  class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold ' + (row.status === 1
                    ? 'text-amber-600 dark:text-amber-400 hover:bg-amber-500/10'
                    : 'text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10'),
                  onClick: () => toggleStatus(row)
                }, [h(Icon, { icon: row.status === 1 ? 'mdi:block-helper' : 'mdi:check-circle-outline', width: 14 }), row.status === 1 ? '禁用' : '启用']),
                h('button', {
                  class: 'inline-flex items-center gap-0.5 px-1.5 py-1 rounded-lg text-xs font-semibold text-red-500 hover:bg-red-500/10',
                  onClick: () => handleDelete(row.id)
                }, [h(Icon, { icon: 'mdi:delete-outline', width: 14 }), '删除'])
              ])
            }
          }
        ]" :data="filteredUsers" />
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <ConfirmDialog v-model:show="modalShow" :title="modalMode === 'create' ? '新增用户' : '编辑用户'" width="480px" :hide-default-footer="true">
      <div class="flex flex-col gap-4">
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">用户名 *</label>
          <n-input v-model:value="form.username" placeholder="请输入用户名" />
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">
            {{ modalMode === 'create' ? '密码 *' : '新密码（留空不修改）' }}
          </label>
          <n-input v-model:value="form.password" type="password" placeholder="请输入密码" />
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">昵称</label>
          <n-input v-model:value="form.nickname" placeholder="请输入昵称" />
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">状态</label>
          <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </div>
        <div>
          <label class="text-xs font-body font-semibold text-on-surface-variant dark:text-gray-400 mb-1 block">角色</label>
          <n-select v-model:value="form.roleIds" :options="roles.map(r => ({ label: r.name + ' (' + r.code + ')', value: r.id }))" multiple placeholder="选择角色" />
        </div>
      </div>
      <template #footer>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" @click="modalShow = false">取消</button>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity" @click="handleSubmit">{{ modalMode === 'create' ? '创建' : '保存' }}</button>
      </template>
    </ConfirmDialog>

    <!-- 角色分配弹窗 -->
    <ConfirmDialog v-model:show="roleModalShow" title="分配角色" width="420px" :hide-default-footer="true">
      <p class="text-sm text-on-surface-variant dark:text-gray-400 mb-4">为用户「{{ roleTargetUser?.nickname || roleTargetUser?.username }}」分配角色</p>
      <n-checkbox-group v-model:value="selectedRoleIds">
        <div class="flex flex-col gap-3">
          <label v-for="r in roles" :key="r.id" class="flex items-center gap-3 cursor-pointer p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/5">
            <n-checkbox :value="r.id" />
            <div>
              <div class="text-sm font-body font-semibold">{{ r.name }}</div>
              <div class="text-xs text-on-surface-variant dark:text-gray-400">{{ r.remark }}</div>
            </div>
          </label>
        </div>
      </n-checkbox-group>
      <template #footer>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-on-surface-variant dark:text-gray-400 hover:bg-black/5 dark:hover:bg-white/5 transition-colors" @click="roleModalShow = false">取消</button>
        <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity" @click="saveRoleAssign">保存</button>
      </template>
    </ConfirmDialog>

    <ConfirmDialog v-model:show="confirmShow" :title="confirmTitle" :content="confirmContent" type="error" icon-type="warning" @confirm="onConfirmOk" />
  </div>
</template>
