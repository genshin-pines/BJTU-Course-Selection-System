<template>
  <div id="app-container">
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <router-link to="/" class="logo">BJTU 课程评价</router-link>
        </div>
        <div class="header-right">
          <template v-if="authStore.token">
            <span class="user-info" v-if="authStore.userRole === 'STUDENT'">
              {{ authStore.userInfo?.anonymousId }}
            </span>
            <span class="user-info" v-else>管理员</span>
            <el-button text @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">学生登录</el-button>
            <el-button text @click="$router.push('/admin/login')">管理员登录</el-button>
          </template>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const router = useRouter()

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}

.app-header {
  background: #1a73e8;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 56px;
}

.app-header .logo {
  color: white;
  font-size: 20px;
  font-weight: 700;
  text-decoration: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  color: #e8f0fe;
  font-size: 14px;
}

.header-right .el-button--text {
  color: white;
}

.el-main {
  min-height: calc(100vh - 56px);
  padding: 24px 32px;
}
</style>
