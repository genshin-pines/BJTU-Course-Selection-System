import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = window.sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function isAdminRequest(config) {
  const url = config?.url || ''
  const role = window.sessionStorage.getItem('userRole')
  return role === 'ADMIN' || url.startsWith('/admin') || url.startsWith('/auth/admin')
}

function clearSession() {
  window.sessionStorage.removeItem('token')
  window.sessionStorage.removeItem('userRole')
  window.sessionStorage.removeItem('userInfo')
}

request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 200) {
      if (!response.config?.silentError) {
        ElMessage.error(data.message || '请求失败')
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  error => {
    const silentError = error.config?.silentError
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const redirectTo = isAdminRequest(error.config) ? '/admin/login' : '/login'
        clearSession()
        if (!error.config?.skipAuthRedirect) {
          ElMessage.error('登录已过期，请重新登录')
          window.location.href = redirectTo
        }
      } else if (status === 403) {
        if (!silentError) {
          ElMessage.error('无权限访问，请确认当前账号角色')
        }
      } else if (!silentError) {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (!silentError) {
      ElMessage.error('网络错误，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request
