import { ref } from 'vue'
import { menuApi } from '../api'

const STORAGE_KEY = 'permissions'

// 当前用户拥有的权限标识集合（含超管 "*"），来自菜单树中配置的 permission
const permissions = ref(readStorage())
// 当前用户菜单树缓存（含按钮节点，侧边栏渲染前自行过滤）
let menusCache = []
let pendingPromise = null

function readStorage() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch {
    return []
  }
}

function collectPermissions(menus = []) {
  const result = []
  const walk = (nodes) => {
    for (const n of nodes || []) {
      if (n.permission) result.push(n.permission)
      if (n.children && n.children.length) walk(n.children)
    }
  }
  walk(menus)
  return result
}

/**
 * 幂等加载当前用户菜单树，并同步刷新权限集合。
 * 重复调用（含并发）只发一次请求；返回缓存树。
 */
export function loadMenus(force = false) {
  if (!force && pendingPromise) return pendingPromise
  if (!force && menusCache.length) return Promise.resolve(menusCache)
  pendingPromise = menuApi.menus()
    .then(tree => {
      menusCache = tree || []
      const perms = collectPermissions(menusCache)
      permissions.value = perms
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(perms))
      } catch {}
      return menusCache
    })
    .catch(err => {
      // 拉取失败：保留缓存/存储中的旧权限，避免误隐藏
      if (!menusCache.length) permissions.value = readStorage()
      throw err
    })
    .finally(() => { pendingPromise = null })
  return pendingPromise
}

/** 按钮级权限判断：超管（"*"）或权限标识命中 */
export function hasPermission(perm) {
  const list = permissions.value
  return list.includes('*') || list.includes(perm)
}

/** 过滤 type=3（按钮/权限）节点，仅用于侧边栏等菜单渲染 */
export function filterMenuButtons(menus = []) {
  // 兼容 type 可能为字符串/空的情况：数值化后再判，避免按钮(type=3)漏网显示成菜单
  return menus
    .filter(m => Number(m.type) !== 3)
    .map(m => ({
      ...m,
      children: m.children && m.children.length ? filterMenuButtons(m.children) : undefined
    }))
}

export function clearPermissions() {
  permissions.value = []
  menusCache = []
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {}
}
