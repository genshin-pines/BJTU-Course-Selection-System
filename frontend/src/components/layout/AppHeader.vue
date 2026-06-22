<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/" class="brand">
        <span class="brand-mark">
          <el-icon><Reading /></el-icon>
        </span>
        <span class="brand-text">
          <span class="brand-name">BJTU 课程评价</span>
          <span class="brand-sub">Course Review</span>
        </span>
      </router-link>

      <nav class="header-nav" v-if="authStore.isStudent">
        <router-link to="/home" class="nav-link" active-class="active">
          <el-icon><Search /></el-icon>找课程
        </router-link>
        <router-link to="/profile" class="nav-link" active-class="active">
          <el-icon><User /></el-icon>我的评价
        </router-link>
      </nav>

      <div class="header-right">
        <template v-if="authStore.isLoggedIn">
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-chip">
              <span class="user-avatar" :class="{ admin: !authStore.isStudent }">
                <el-icon><UserFilled /></el-icon>
              </span>
              <span class="user-name">
                {{ authStore.isStudent
                  ? (authStore.userInfo?.anonymousId || authStore.userInfo?.studentNo || '同学')
                  : '管理员' }}
              </span>
              <el-icon class="caret"><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="authStore.isStudent" command="home">
                  <el-icon><Search /></el-icon>找课程
                </el-dropdown-item>
                <el-dropdown-item v-if="authStore.isStudent" command="profile">
                  <el-icon><User /></el-icon>我的评价
                </el-dropdown-item>
                <el-dropdown-item v-if="authStore.isAdmin" command="admin">
                  <el-icon><Setting /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button text class="ghost-btn" @click="$router.push('/login')">学生登录</el-button>
          <el-button class="login-btn" @click="$router.push('/admin/login')">
            <el-icon><Lock /></el-icon>管理员
          </el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Reading, Search, User, UserFilled, CaretBottom,
  Setting, SwitchButton, Lock
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()

function onCommand(cmd) {
  if (cmd === 'home') router.push('/home')
  else if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'admin') router.push('/admin')
  else if (cmd === 'logout') handleLogout()
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '退出', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }).catch(() => {})
}
</script>

<style scoped>
.app-header {
  height: 56px;
  background: var(--brand-gradient);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 16px rgba(102, 126, 234, 0.25);
}

.header-inner {
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-name {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-sub {
  color: rgba(255, 255, 255, 0.7);
  font-size: 10px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.header-nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 5px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  padding: 7px 14px;
  border-radius: var(--r-pill);
  transition: all 0.2s;
}
.nav-link:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}
.nav-link.active {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-weight: 600;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 5px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: all 0.2s;
  color: #fff;
}
.user-chip:hover {
  background: rgba(255, 255, 255, 0.28);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.user-avatar.admin {
  background: rgba(251, 191, 36, 0.5);
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  font-size: 11px;
  opacity: 0.8;
}

.ghost-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  font-weight: 500;
}
.ghost-btn:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.15) !important;
}

.login-btn {
  background: rgba(255, 255, 255, 0.18) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  color: #fff !important;
  backdrop-filter: blur(8px);
  font-weight: 500;
}
.login-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  border-color: rgba(255, 255, 255, 0.6) !important;
}
</style>
