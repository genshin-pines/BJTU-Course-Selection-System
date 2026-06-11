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
  const sessionChecked = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => !!token.value && userRole.value === 'STUDENT')
  const isAdmin = computed(() => !!token.value && userRole.value === 'ADMIN')

  function persist(nextToken, nextRole, nextUserInfo) {
    token.value = nextToken || ''
    userRole.value = nextRole || ''
    userInfo.value = nextUserInfo || null

    if (token.value) {
      STORAGE.setItem('token', token.value)
      STORAGE.setItem('userRole', userRole.value)
      if (userInfo.value) {
        STORAGE.setItem('userInfo', JSON.stringify(userInfo.value))
      } else {
        STORAGE.removeItem('userInfo')
      }
    } else {
      STORAGE.removeItem('token')
      STORAGE.removeItem('userRole')
      STORAGE.removeItem('userInfo')
    }
  }

  function syncFromStorage() {
    token.value = STORAGE.getItem('token') || ''
    userRole.value = STORAGE.getItem('userRole') || ''
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

  async function adminLogin(username, password) {
    const res = await request.post('/auth/admin/login', { username, password })
    persist(res.data.token, 'ADMIN', {
      username
    })
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
      persist(token.value, session.role, nextUserInfo)
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
    sessionChecked,
    isLoggedIn,
    isStudent,
    isAdmin,
    syncFromStorage,
    verifySession,
    studentLogin,
    adminLogin,
    logout
  }
})
