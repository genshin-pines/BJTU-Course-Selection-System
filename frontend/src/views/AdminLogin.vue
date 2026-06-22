<template>
  <div class="auth-page">
    <aside class="auth-brand">
      <div class="brand-bg"></div>
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon><Lock /></el-icon>
          <span>管理后台</span>
        </div>
        <h1 class="brand-headline">课程评价<br/>运营管理平台</h1>
        <p class="brand-desc">评价审核 · 举报处理 · 标签维护<br/>基础数据管理与审计日志</p>
        <div class="brand-roles">
          <div class="role-chip">超级管理员</div>
          <div class="role-chip">院系维护员</div>
          <div class="role-chip">内容审核员</div>
        </div>
      </div>
    </aside>

    <main class="auth-form-area">
      <div class="form-wrap">
        <div class="form-title">
          <span class="title-icon"><el-icon><UserFilled /></el-icon></span>
          <h2>管理员登录</h2>
          <p>请使用管理员账号登录后台</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="auth-form">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入管理员用户名" size="large" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" prefix-icon="Lock" @keyup.enter="handleLogin" />
          </el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" size="large" class="submit-btn">
            登录后台
          </el-button>
          <div class="back-home">
            <router-link to="/">返回首页</router-link>
          </div>
        </el-form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.adminLogin(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 56px - 56px);
  display: flex;
}

.auth-brand {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  min-height: 560px;
}

.brand-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #0c4a6e 0%, #075985 50%, #0369a1 100%);
}
.brand-bg::before,
.brand-bg::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
}
.brand-bg::before {
  width: 400px;
  height: 400px;
  background: #0ea5e9;
  top: -100px;
  right: -60px;
}
.brand-bg::after {
  width: 320px;
  height: 320px;
  background: #3b82f6;
  bottom: -80px;
  left: -40px;
}

.brand-content {
  position: relative;
  z-index: 2;
  padding: 60px;
  color: #fff;
  max-width: 520px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 48px;
}
.brand-logo .el-icon {
  font-size: 26px;
}

.brand-headline {
  font-size: 42px;
  font-weight: 800;
  line-height: 1.3;
  margin-bottom: 20px;
}

.brand-desc {
  font-size: 15px;
  line-height: 1.9;
  opacity: 0.8;
  margin-bottom: 40px;
}

.brand-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.role-chip {
  padding: 6px 16px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.25);
  font-size: 13px;
  backdrop-filter: blur(8px);
}

.auth-form-area {
  width: 460px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
}

.form-wrap {
  width: 100%;
  max-width: 360px;
}

.form-title {
  text-align: center;
  margin-bottom: 32px;
}
.title-icon {
  display: inline-flex;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--brand-gradient-soft);
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--brand-primary);
  margin-bottom: 16px;
}
.form-title h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 6px;
}
.form-title p {
  font-size: 14px;
  color: var(--text-3);
}

.auth-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-2);
  padding-bottom: 4px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.back-home {
  text-align: center;
  margin-top: 20px;
}
.back-home a {
  font-size: 13px;
  color: var(--text-3);
  transition: color 0.2s;
}
.back-home a:hover {
  color: var(--brand-primary);
}

@media (max-width: 860px) {
  .auth-page {
    flex-direction: column;
  }
  .auth-brand {
    min-height: auto;
    padding: 40px 28px;
  }
  .brand-content {
    padding: 0;
    max-width: 100%;
  }
  .brand-headline {
    font-size: 28px;
  }
  .auth-form-area {
    width: 100%;
    padding: 32px 24px 48px;
  }
}
</style>
