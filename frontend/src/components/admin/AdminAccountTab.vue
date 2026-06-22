<template>
  <div>
    <div class="section-toolbar">
      <el-input v-model="accountForm.username" placeholder="账号，如 dept_op2" style="width: 180px" />
      <el-input v-model="accountForm.password" type="password" show-password placeholder="至少 6 位" style="width: 180px" />
      <el-select v-model="accountForm.role" style="width: 160px">
        <el-option v-for="role in adminRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
      </el-select>
      <el-input v-model="accountForm.department" placeholder="所属院系（DEPT_OP）" style="width: 220px" />
      <el-button type="primary" @click="handleCreateAccount">新增管理员</el-button>
    </div>
    <el-table :data="adminAccounts" v-loading="accountLoading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="账号" min-width="160" />
      <el-table-column label="角色" width="180">
        <template #default="{ row }">
          <el-select :model-value="row.role" size="small" @change="(role) => handleUpdateAccountRole(row, role)">
            <el-option v-for="role in adminRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="department" label="所属院系" min-width="180">
        <template #default="{ row }">{{ row.department || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleUpdateAccountDepartment(row)">设置院系</el-button>
          <el-button size="small" @click="handleResetAccountPassword(row)">重置密码</el-button>
          <el-button
            type="danger"
            size="small"
            :disabled="row.username === authStore.userInfo?.username"
            @click="handleDeleteAccount(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const adminRoleOptions = [
  { value: 'SUPER_ADMIN', label: '超级管理员' },
  { value: 'DEPT_OP', label: '院系维护员' },
  { value: 'AUDITOR', label: '内容审核员' }
]

const adminAccounts = ref([])
const accountLoading = ref(false)
const accountForm = ref({ username: '', password: '', role: 'AUDITOR', department: '' })

async function loadAdminAccounts() {
  accountLoading.value = true
  try {
    const res = await adminApi.getAdminAccounts()
    adminAccounts.value = res.data || []
  } finally {
    accountLoading.value = false
  }
}

async function handleCreateAccount() {
  if (!accountForm.value.username.trim() || !accountForm.value.password.trim()) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  await adminApi.createAdminAccount({
    username: accountForm.value.username.trim(),
    password: accountForm.value.password.trim(),
    role: accountForm.value.role,
    department: accountForm.value.department.trim()
  })
  ElMessage.success('管理员账号已创建')
  accountForm.value = { username: '', password: '', role: 'AUDITOR', department: '' }
  await loadAdminAccounts()
}

async function handleUpdateAccountRole(row, role) {
  if (row.role === role) return
  try {
    await adminApi.updateAdminRole(row.id, role, row.department || '')
    ElMessage.success('角色已更新')
  } finally {
    await loadAdminAccounts()
  }
}

async function handleUpdateAccountDepartment(row) {
  try {
    const { value } = await ElMessageBox.prompt(`请输入 ${row.username} 的所属院系`, '设置院系范围', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValue: row.department || ''
    })
    await adminApi.updateAdminRole(row.id, row.role, value.trim())
    ElMessage.success('院系范围已更新')
    await loadAdminAccounts()
  } catch (e) {}
}

async function handleResetAccountPassword(row) {
  try {
    const { value } = await ElMessageBox.prompt(`请输入 ${row.username} 的新密码`, '重置密码', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /\S{6,}/,
      inputErrorMessage: '密码至少 6 位'
    })
    await adminApi.resetAdminPassword(row.id, value.trim())
    ElMessage.success('密码已重置')
  } catch (e) {}
}

async function handleDeleteAccount(row) {
  try {
    await ElMessageBox.confirm(`确认删除管理员账号 ${row.username}？`, '删除管理员', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminApi.deleteAdminAccount(row.id)
    ElMessage.success('管理员账号已删除')
    await loadAdminAccounts()
  } catch (e) {}
}

onMounted(loadAdminAccounts)
</script>
