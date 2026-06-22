<template>
  <div>
    <el-table :data="pendingReviews" v-loading="reviewLoading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="courseName" label="课程" width="180" />
      <el-table-column prop="teacherName" label="教师" width="120" />
      <el-table-column label="开课实例" width="190">
        <template #default="{ row }">{{ formatInstance(row) }}</template>
      </el-table-column>
      <el-table-column label="评分" width="170">
        <template #default="{ row }">
          给分: {{ row.gradingScore }} | 授课: {{ row.teachingScore }} | 作业: {{ row.workloadScore }}
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="anonymousId" label="用户" width="140" />
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="reviewStatusType(row.status)">{{ reviewStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="approveReview(row.id)">通过</el-button>
          <el-button type="danger" size="small" @click="rejectReview(row.id)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!reviewLoading && pendingReviews.length === 0" description="暂无待审核评价" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const pendingReviews = ref([])
const reviewLoading = ref(false)

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

async function loadPendingReviews() {
  reviewLoading.value = true
  try {
    const res = await adminApi.getPendingReviews()
    pendingReviews.value = res.data || []
  } finally {
    reviewLoading.value = false
  }
}

async function approveReview(id) {
  try {
    await ElMessageBox.confirm('确认通过该评价？', '审核通过', {
      type: 'success',
      confirmButtonText: '确认通过',
      cancelButtonText: '取消'
    })
    await adminApi.approveReview(id)
    ElMessage.success('已通过')
    await loadPendingReviews()
  } catch (e) {}
}

async function rejectReview(id) {
  try {
    const reason = await askReason('审核拒绝', '请填写拒绝该评价的原因')
    await adminApi.rejectReview(id, reason)
    ElMessage.success('已拒绝')
    await loadPendingReviews()
  } catch (e) {}
}

function formatInstance(row) {
  const parts = []
  if (row.courseInstanceId) parts.push(`#${row.courseInstanceId}`)
  return parts.length ? parts.join(' / ') : '-'
}

function reviewStatusType(status) {
  if (status === 'PUBLISHED' || status === 'PENDING_AUDIT' || status === 'PENDING_MANUAL' || status === 'PENDING') {
    return 'warning'
  }
  if (status === 'APPROVED') return 'success'
  if (status === 'HIDDEN' || status === 'REJECTED') return 'danger'
  return 'info'
}

function reviewStatusText(status) {
  const map = {
    PUBLISHED: '已发布待审核',
    APPROVED: '审核通过',
    HIDDEN: '已隐藏',
    PENDING_AUDIT: '待审核',
    PENDING_MANUAL: '待人工复核',
    PENDING: '待审核',
    REJECTED: '已拒绝',
    ARCHIVED: '已归档'
  }
  return map[status] || status || '-'
}

onMounted(loadPendingReviews)
</script>
