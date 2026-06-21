import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import request from '@/utils/request'

const STORAGE = window.sessionStorage

function readUserInfo() {
  try {
    return JSON.parse(STORAGE.getItem('userInfo') || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(STORAGE.getItem('token') || '')
  const userInfo = ref(readUserInfo())
  const userRole = ref(STORAGE.getItem('userRole') || '')
  const adminRole = ref(STORAGE.getItem('adminRole') || '')
  const sessionChecked = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => !!token.value && userRole.value === 'STUDENT')
  const isAdmin = computed(() => !!token.value && userRole.value === 'ADMIN')

  function persist(nextToken, nextRole, nextUserInfo, nextAdminRole = '') {
    token.value = nextToken || ''
    userRole.value = nextRole || ''
    userInfo.value = nextUserInfo || null
    adminRole.value = nextAdminRole || ''

    if (token.value) {
      STORAGE.setItem('token', token.value)
      STORAGE.setItem('userRole', userRole.value)
      if (adminRole.value) {
        STORAGE.setItem('adminRole', adminRole.value)
      } else {
        STORAGE.removeItem('adminRole')
      }
      if (userInfo.value) {
        STORAGE.setItem('userInfo', JSON.stringify(userInfo.value))
      } else {
        STORAGE.removeItem('userInfo')
      }
    } else {
      STORAGE.removeItem('token')
      STORAGE.removeItem('userRole')
      STORAGE.removeItem('adminRole')
      STORAGE.removeItem('userInfo')
    }
  }

  function syncFromStorage() {
    token.value = STORAGE.getItem('token') || ''
    userRole.value = STORAGE.getItem('userRole') || ''
    adminRole.value = STORAGE.getItem('adminRole') || ''
    userInfo.value = readUserInfo()
  }

  async function studentLogin(studentNo, password) {
    const res = await request.post('/auth/login', { studentNo, password })
    persist(res.data.token, 'STUDENT', {
      anonymousId: res.data.anonymousId,
      studentNo: res.data.studentNo,
      name: res.data.name
    })
    sessionChecked.value = true
  }

  async function studentRegister(email, studentNo, password, code) {
    await request.post('/auth/register', { email, studentNo, password, code })
  }

  async function sendCode(email, studentNo) {
    await request.post('/auth/send-code', { email, studentNo })
  }

  async function adminLogin(username, password) {
    const res = await request.post('/auth/admin/login', { username, password })
    persist(res.data.token, 'ADMIN', {
      username
    }, res.data.adminRole || 'SUPER_ADMIN')
    sessionChecked.value = true
  }

  async function verifySession(force = false) {
    syncFromStorage()
    if (!token.value) {
      sessionChecked.value = true
      return false
    }
    if (sessionChecked.value && !force) {
      return true
    }

    try {
      const res = await request.get('/auth/session', { skipAuthRedirect: true })
      const session = res.data
      const nextUserInfo = session.role === 'STUDENT'
        ? {
            anonymousId: session.anonymousId,
            studentNo: session.username,
            name: session.name
          }
        : {
            username: session.username
          }
      persist(token.value, session.role, nextUserInfo, session.adminRole || '')
      sessionChecked.value = true
      return true
    } catch {
      logout()
      sessionChecked.value = true
      return false
    }
  }

  function logout() {
    persist('', '', null)
    sessionChecked.value = false
  }

  return {
    token,
    userInfo,
    userRole,
    adminRole,
    sessionChecked,
    isLoggedIn,
    isStudent,
    isAdmin,
    syncFromStorage,
    verifySession,
    studentLogin,
    studentRegister,
    sendCode,
    adminLogin,
    logout
  }
})
