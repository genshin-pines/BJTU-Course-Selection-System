import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const userRole = ref(localStorage.getItem('userRole') || '')

  // 学生登录
  async function studentLogin(studentNo, password) {
    const res = await request.post('/auth/login', { studentNo, password })
    token.value = res.data.token
    userRole.value = 'STUDENT'
    userInfo.value = {
      anonymousId: res.data.anonymousId,
      studentNo: res.data.studentNo,
      name: res.data.name
    }
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userRole', 'STUDENT')
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  // 管理员登录
  async function adminLogin(username, password) {
    const res = await request.post('/auth/admin/login', { username, password })
    token.value = res.data.token
    userRole.value = 'ADMIN'
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userRole', 'ADMIN')
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    userRole.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userRole')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, userRole, studentLogin, adminLogin, logout }
})
