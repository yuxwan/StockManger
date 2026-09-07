import { createRouter, createWebHashHistory } from 'vue-router'
import { loadingBarRef } from '../utils/loadingBar'
import MainLayout from '../layouts/MainLayout.vue'
import Login from '../views/Login.vue'
import Checkout from '../views/Checkout.vue'
import Dashboard from '../views/Dashboard.vue'
import Products from '../views/Products.vue'
import AddProduct from '../views/AddProduct.vue'
import Orders from '../views/Orders.vue'
import Operations from '../views/Operations.vue'
import Settings from '../views/Settings.vue'
import Users from '../views/system/Users.vue'
import Roles from '../views/system/Roles.vue'
import Menus from '../views/system/Menus.vue'

const routes = [
  { path: '/login', name: 'login', component: Login, meta: { requiresAuth: false } },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/checkout' },
      { path: 'checkout', name: 'checkout', component: Checkout },
      { path: 'dashboard', name: 'dashboard', component: Dashboard },
      { path: 'products', name: 'products', component: Products },
      { path: 'products/add', name: 'add-product', component: AddProduct },
      { path: 'products/edit/:id', name: 'edit-product', component: AddProduct, props: true },
      { path: 'orders', name: 'orders', component: Orders },
      { path: 'operations', name: 'operations', component: Operations },
      { path: 'settings', name: 'settings', component: Settings },
      { path: 'users', name: 'users', component: Users },
      { path: 'roles', name: 'roles', component: Roles },
      { path: 'menus', name: 'menus', component: Menus }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta?.requiresAuth === false) return
  if (to.path === '/login') return
  const token = localStorage.getItem('token')
  if (!token) {
    return { path: '/login', replace: true }
  }
  loadingBarRef.value?.start()
})
router.afterEach(() => { loadingBarRef.value?.finish() })
router.onError(() => { loadingBarRef.value?.error() })

export default router
