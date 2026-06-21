<template>
  <div class="login-container">
    <el-card class="login-card">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="80px">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="loginForm.studentNo" placeholder="请输入学号" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="100px">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="8位学号@bjtu.edu.cn" />
            </el-form-item>
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="registerForm.studentNo" placeholder="请输入学号" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请设置密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <div style="display: flex; gap: 8px; width: 100%;">
                <el-input v-model="registerForm.code" placeholder="请输入验证码" style="flex: 1;" maxlength="6" />
                <el-button :disabled="sending || countdown > 0" @click="handleSendCode" style="white-space: nowrap; flex-shrink: 0;">
                  {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="registering" @click="handleRegister" style="width: 100%">
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

const loginForm = reactive({
  studentNo: '',
  password: ''
})

const registerForm = reactive({
  email: '',
  studentNo: '',
  password: '',
  confirmPassword: '',
  code: ''
})

const loginRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
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
    registerForm.email = ''
    registerForm.studentNo = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.code = ''
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 120px);
}

.login-card {
  width: 420px;
  padding: 24px 0;
}

.login-card :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.login-card :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 600;
}
</style>
