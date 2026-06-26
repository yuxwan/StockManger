import axios from 'axios'
import message from '../utils/message'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'https://yuxwan.com/stock/api/',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

request.interceptors.response.use(
  res => res.data,
  err => {
    if (err.code === 'ECONNABORTED' && err.message.includes('timeout')) {
      message.error('请求超时，请检查网络连接')
    } else if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userName')
      localStorage.removeItem('userNickname')
      localStorage.removeItem('userRole')
      message.error('登录已过期，请重新登录')
      setTimeout(() => {
        window.location.hash = '#/login'
        window.location.reload()
      }, 1000)
    } else {
      const msg = err.response?.data?.msg || err.message || '请求失败'
      message.error(msg)
    }
    return Promise.reject(err)
  }
)

export default request
