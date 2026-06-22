<template>
  <div>
    <el-table :data="reports" v-loading="reportLoading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="courseName" label="课程" width="180" />
      <el-table-column prop="teacherName" label="教师" width="120" />
      <el-table-column label="开课实例" width="190">
        <template #default="{ row }">{{ formatInstance(row) }}</template>
      </el-table-column>
      <el-table-column prop="anonymousId" label="评价用户" width="140" />
      <el-table-column prop="reporterAnonymousId" label="举报用户" width="140" />
      <el-table-column prop="reviewContent" label="被举报内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="reason" label="举报原因" width="220" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="reportStatusType(row.status)">{{ reportStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button type="primary" size="small" @click="resolveReport(row.id)">采纳</el-button>
            <el-button type="warning" size="small" @click="dismissReport(row.id)">驳回</el-button>
          </template>
          <span v-else class="muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!reportLoading && reports.length === 0" description="暂无举报" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const reports = ref([])
const reportLoading = ref(false)

async function askReason(title, placeholder) {
  const { value } = await ElMessageBox.prompt(placeholder, title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPattern: /\S+/,
    inputErrorMessage: '请填写操作原因'
  })
  return value.trim()
}

async function loadReports() {
  reportLoading.value = true
  try {
    const res = await adminApi.getAllReports()
    reports.value = res.data || []
  } finally {
    reportLoading.value = false
  }
}

async function resolveReport(id) {
  try {
    const reason = await askReason('采纳举报', '请填写采纳原因，采纳后将隐藏/删除对应评价')
    await adminApi.resolveReport(id, reason)
    ElMessage.success('举报已采纳')
    await loadReports()
  } catch (e) {}
}

async function dismissReport(id) {
  try {
    const reason = await askReason('驳回举报', '请填写驳回该举报的原因')
    await adminApi.dismissReport(id, reason)
    ElMessage.success('举报已驳回')
    await loadReports()
  } catch (e) {}
}

function formatInstance(row) {
  const parts = []
  if (row.courseInstanceId) parts.push(`#${row.courseInstanceId}`)
  return parts.length ? parts.join(' / ') : '-'
}

function reportStatusType(status) {
  if (status === 'PENDING') return 'warning'
  if (status === 'RESOLVED') return 'success'
  return 'info'
}

function reportStatusText(status) {
  if (status === 'PENDING') return '待处理'
  if (status === 'RESOLVED') return '已处理'
  if (status === 'DISMISSED') return '已驳回'
  return status || '-'
}

onMounted(loadReports)
</script>

<style scoped>
.muted {
  color: #999;
}
</style>
