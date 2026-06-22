import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '课程搜索' }
  },
  {
    path: '/course/instance/:instanceId',
    name: 'CourseDetail',
    component: () => import('@/views/CourseDetail.vue'),
    meta: { title: '课程详情' }
  },
  {
    path: '/post-review/:instanceId',
    name: 'PostReview',
    component: () => import('@/views/PostReview.vue'),
    meta: { title: '发布评价', requiresStudent: true }
  },
  {
    path: '/edit-review/:reviewId',
    name: 'EditReview',
    component: () => import('@/views/PostReview.vue'),
    meta: { title: '修改评价', requiresStudent: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '我的评价', requiresStudent: true }
  },
  {
    path: '/course-application',
    name: 'CourseApplication',
    component: () => import('@/views/CourseApplication.vue'),
    meta: { title: '申请新课程', requiresStudent: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/AdminLogin.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/AdminDashboard.vue'),
    meta: { title: '管理后台', requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `BJTU - ${to.meta.title}` : 'BJTU 课程评价系统'

  const authStore = useAuthStore()
  const needsAuth = to.meta.requiresStudent || to.meta.requiresAdmin
  if (needsAuth) {
    await authStore.verifySession(true)
  } else {
    authStore.syncFromStorage()
  }

  if (to.meta.requiresStudent && !authStore.isStudent) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/admin/login')
    return
  }

  next()
})

export default router
