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

/**
 * 回显 checked-keys 前的“半选祖先”剔除。
 * naive-ui cascade 语义：checked-keys 里含父节点 key 时，会把其整棵子树都级联勾选。
 * 而 sys_role_menu 历史上可能存有“仅为保证菜单树完整”的父节点（只勾了部分子菜单也会被落库），
 * 若不剔除，第二次打开编辑时父目录下所有子菜单都会被自动全选。
 * 剔除规则：若某节点子树中存在不在选中集合中的节点，说明它只是半选/补链，不应作为已勾选回显；
 * 用户整组勾选的节点（全部子孙都在集合中）不受影响。
 */
function sanitizeCheckedKeys(ids, tree) {
  const idSet = new Set(ids)
  const nodeMap = new Map()
  const walk = (nodes) => {
    for (const n of nodes || []) {
      nodeMap.set(n.id, n)
      if (n.children && n.children.length) walk(n.children)
    }
  }
  walk(tree)
  const subtreeMissing = (node) => {
    for (const c of node.children || []) {
      if (!idSet.has(c.id)) return true
      if (subtreeMissing(c)) return true
    }
    return false
  }
  return ids.filter((id) => {
    const node = nodeMap.get(id)
    if (!node || !node.children || !node.children.length) return true
    return !subtreeMissing(node)
  })
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
    // 回显需要基于完整菜单树剔除“半选祖先”，先确保菜单已加载
    if (!menus.value.length) await fetchMenus()
    const data = await roleApi.get(id)
    const role = data.role || data
    modalMode.value = 'edit'
    editingId.value = id
    form.name = role.name
    form.code = role.code
    form.remark = role.remark || ''
    form.status = role.status ?? 1
    form.menuIds = sanitizeCheckedKeys(data.menuIds || [], menus.value)
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
  } catch {
  }
}

function handleDelete(id) {
  useConfirm('确认删除', '确定要删除该角色吗？此操作不可撤销。', async () => {
    try {
      await roleApi.delete(id)
      message.success('角色已删除')
      await fetchRoles()
    } catch {
    }
  })
}

onMounted(() => {
  fetchRoles()
  fetchMenus()
})
</script>

<template>
  <div class="flex-1 min-h-0 flex flex-col gap-6">
    <!-- 顶栏 -->
    <div class="flex items-center justify-between shrink-0">
      <div>
        <h1 class="text-2xl font-body font-bold tracking-tight">角色管理</h1>
        <p class="text-sm text-on-surface-variant dark:text-gray-400 font-body mt-1">管理系统角色和菜单权限分配</p>
      </div>
      <button class="h-9 px-4 rounded-xl text-sm font-body font-semibold text-white bg-black dark:bg-white dark:text-black hover:opacity-80 transition-opacity flex items-center gap-1.5" @click="openCreate">
        <Icon icon="mdi:plus" width="16" /> 新增角色
      </button>
    </div>

    <!-- 角色列表 -->
    <n-card class="flex-1 min-h-0 flex flex-col" content-style="flex:1;display:flex;flex-direction:column;min-height:0">
      <div class="flex-1 min-h-0">
      <n-data-table flex-height :bordered="false" :loading="loading" size="small" scroll-x="800" style="height:100%"
        :columns="[
          { title: 'ID', key: 'id', minWidth: 60 },
          { title: '角色名称', key: 'name', minWidth: 120 },
          { title: '角色编码', key: 'code', minWidth: 120 },
          { title: '备注', key: 'remark', ellipsis: { tooltip: true }, width: 200 },
          {
            title: '状态', key: 'status', minWidth: 60,
            render(row) {
              return h('span', {
                class: row.status === 1
                  ? 'text-xs font-semibold rounded-md text-emerald-600 dark:text-emerald-400'
                  : 'text-xs font-semibold rounded-md text-red-600 dark:text-red-400'
              }, row.status === 1 ? '启用' : '禁用')
            }
          },
          {
            title: '操作', key: 'actions', width: 130, fixed: 'right',
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
      </div>
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
