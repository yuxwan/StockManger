import request from './request'

export const authApi = {
  login(data) {
    return request.post('/auth/login', data)
  },
  info() {
    return request.get('/auth/info')
  },
  logout() {
    return request.post('/auth/logout')
  }
}

export const productApi = {
  list() {
    return request.get('/products')
  },
  get(id) {
    return request.get(`/products/${id}`)
  },
  getByBarcode(barcode) {
    return request.get(`/products/barcode/${barcode}`)
  },
  create(data) {
    return request.post('/products', data)
  },
  update(id, data) {
    return request.put(`/products/${id}`, data)
  },
  delete(id) {
    return request.delete(`/products/${id}`)
  },
  adjustStock(id, delta) {
    return request.patch(`/products/${id}/stock`, { delta })
  }
}

export const orderApi = {
  list() {
    return request.get('/orders')
  },
  get(id) {
    return request.get(`/orders/${id}`)
  },
  create(data) {
    return request.post('/orders', data)
  },
  delete(id) {
    return request.delete(`/orders/${id}`)
  },
  refund(id) {
    return request.post(`/orders/${id}/refund`)
  },
  refundItem(orderId, itemId, quantity) {
    return request.post(`/orders/${orderId}/items/${itemId}/refund`, { quantity })
  }
}

export const reportApi = {
  summary(dateRange) {
    return request.get('/reports/summary', { params: { dateRange } })
  }
}

export const categoryApi = {
  list() {
    return request.get('/categories')
  }
}

export const operationLogApi = {
  list(limit = 200) {
    return request.get('/operation-logs', { params: { limit } })
  }
}

export const userApi = {
  changePassword(data) {
    return request.put('/user/password', data)
  }
}

export const systemUserApi = {
  list() {
    return request.get('/system/users')
  },
  get(id) {
    return request.get(`/system/users/${id}`)
  },
  create(data) {
    return request.post('/system/users', data)
  },
  update(id, data) {
    return request.put(`/system/users/${id}`, data)
  },
  delete(id) {
    return request.delete(`/system/users/${id}`)
  },
  toggleStatus(id, status) {
    return request.put(`/system/users/${id}/status`, { status })
  }
}

export const roleApi = {
  list() {
    return request.get('/system/roles')
  },
  get(id) {
    return request.get(`/system/roles/${id}`)
  },
  create(data) {
    return request.post('/system/roles', data)
  },
  update(id, data) {
    return request.put(`/system/roles/${id}`, data)
  },
  delete(id) {
    return request.delete(`/system/roles/${id}`)
  }
}

export const menuApi = {
  /** 获取当前用户的菜单树（按角色过滤） */
  menus() {
    return request.get('/system/menus/my')
  },
  tree() {
    return request.get('/system/menus/tree')
  },
  list() {
    return request.get('/system/menus')
  },
  get(id) {
    return request.get(`/system/menus/${id}`)
  },
  create(data) {
    return request.post('/system/menus', data)
  },
  update(id, data) {
    return request.put(`/system/menus/${id}`, data)
  },
  delete(id) {
    return request.delete(`/system/menus/${id}`)
  }
}
