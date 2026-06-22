<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <aside class="auth-brand">
      <div class="brand-bg"></div>
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon><Reading /></el-icon>
          <span>BJTU 课程评价</span>
        </div>
        <h1 class="brand-headline">选课前<br/>先看看过来人怎么说</h1>
        <p class="brand-desc">匿名评价 · 真实课程体验 · 评分与标签速览<br/>帮你做出更明智的选课决定</p>
        <div class="brand-stats">
          <div class="stat">
            <div class="stat-num">真实</div>
            <div class="stat-lbl">匿名评价</div>
          </div>
          <div class="stat">
            <div class="stat-num">多维</div>
            <div class="stat-lbl">评分体系</div>
          </div>
          <div class="stat">
            <div class="stat-num">智能</div>
            <div class="stat-lbl">标签筛选</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧表单区 -->
    <main class="auth-form-area">
      <div class="form-wrap">
        <el-tabs v-model="activeTab" stretch class="auth-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-position="top" class="auth-form">
              <el-form-item label="学号" prop="studentNo">
                <el-input v-model="loginForm.studentNo" placeholder="请输入学号" size="large" prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password size="large" prefix-icon="Lock" />
              </el-form-item>
              <el-button type="primary" :loading="loading" @click="handleLogin" size="large" class="submit-btn">
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top" class="auth-form">
              <el-form-item label="学校邮箱" prop="email">
                <el-input v-model="registerForm.email" placeholder="8位学号@bjtu.edu.cn" size="large" prefix-icon="Message" />
              </el-form-item>
              <el-form-item label="学号" prop="studentNo">
                <el-input v-model="registerForm.studentNo" placeholder="请输入学号" size="large" prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" type="password" placeholder="请设置密码（至少 6 位）" show-password size="large" prefix-icon="Lock" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password size="large" prefix-icon="Lock" />
              </el-form-item>
              <el-form-item label="邮箱验证码" prop="code">
                <div class="code-row">
                  <el-input v-model="registerForm.code" placeholder="6 位验证码" maxlength="6" size="large" prefix-icon="Key" />
                  <el-button :disabled="sending || countdown > 0" @click="handleSendCode" size="large" class="code-btn">
                    {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-button type="primary" :loading="registering" @click="handleRegister" size="large" class="submit-btn">
                注册账号
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Reading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const registering = ref(false)
const sending = ref(false)
const countdown = ref(0)
const activeTab = ref('login')

let countdownTimer = null

const loginForm = reactive({ studentNo: '', password: '' })
const registerForm = reactive({ email: '', studentNo: '', password: '', confirmPassword: '', code: '' })

const loginRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) callback(new Error('两次输入密码不一致'))
  else callback()
}

const registerRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^\d{8}@bjtu\.edu\.cn$/, message: '邮箱格式必须为8位学号@bjtu.edu.cn', trigger: 'blur' }
  ],
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{8}$/, message: '学号必须为8位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleSendCode() {
  if (!registerForm.email || !/^\d{8}@bjtu\.edu\.cn$/.test(registerForm.email)) {
    ElMessage.warning('请先输入正确的邮箱格式')
    return
  }
  if (!registerForm.studentNo || !/^\d{8}$/.test(registerForm.studentNo)) {
    ElMessage.warning('请先输入8位学号')
    return
  }
  sending.value = true
  try {
    await authStore.sendCode(registerForm.email, registerForm.studentNo)
    ElMessage.success('验证码已发送至邮箱')
    startCountdown(60)
  } catch (e) {
    ElMessage.error(e.message || '发送验证码失败')
  } finally {
    sending.value = false
  }
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.studentLogin(loginForm.studentNo, loginForm.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  registering.value = true
  try {
    await authStore.studentRegister(registerForm.email, registerForm.studentNo, registerForm.password, registerForm.code)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    Object.assign(registerForm, { email: '', studentNo: '', password: '', confirmPassword: '', code: '' })
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 56px - 56px);
  display: flex;
}

/* 左侧品牌区 */
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
  background: var(--brand-gradient-vibrant);
}
.brand-bg::before,
.brand-bg::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
}
.brand-bg::before {
  width: 420px;
  height: 420px;
  background: #7dd3fc;
  top: -120px;
  right: -80px;
}
.brand-bg::after {
  width: 360px;
  height: 360px;
  background: #60a5fa;
  bottom: -100px;
  left: -60px;
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
  font-size: 44px;
  font-weight: 800;
  line-height: 1.25;
  margin-bottom: 20px;
  letter-spacing: 1px;
}

.brand-desc {
  font-size: 15px;
  line-height: 1.9;
  opacity: 0.85;
  margin-bottom: 48px;
}

.brand-stats {
  display: flex;
  gap: 40px;
}
.stat-num {
  font-size: 26px;
  font-weight: 800;
}
.stat-lbl {
  font-size: 13px;
  opacity: 0.8;
  margin-top: 2px;
}

/* 右侧表单区 */
.auth-form-area {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
}

.form-wrap {
  width: 100%;
  max-width: 380px;
}

.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 32px;
}
.auth-tabs :deep(.el-tabs__item) {
  font-size: 17px;
  font-weight: 600;
  padding: 0 0 14px;
}
.auth-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
}

.auth-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-2);
  padding-bottom: 4px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-row .el-input {
  flex: 1;
}
.code-btn {
  flex-shrink: 0;
  width: 130px;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
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
    font-size: 30px;
  }
  .brand-stats {
    gap: 24px;
  }
  .auth-form-area {
    width: 100%;
    padding: 32px 24px 48px;
  }
}
</style>
