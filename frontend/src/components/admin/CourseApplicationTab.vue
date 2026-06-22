<template>
  <div>
    <el-table :data="applications" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="课程" min-width="160">
        <template #default="{ row }">
          <div class="cell-course">
            <strong>{{ row.courseName }}</strong>
            <span class="cell-code">{{ row.courseCode }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="学院/学分" width="140">
        <template #default="{ row }">
          {{ row.department || '-' }} / {{ row.credit }}学分
        </template>
      </el-table-column>
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column label="评分" width="150">
        <template #default="{ row }">
          给{{ row.gradingScore }} 授{{ row.teachingScore }} 作{{ row.workloadScore }}
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="studentAnonymousId" label="申请人" width="120" />
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="handleApprove(row.id)">通过</el-button>
          <el-button type="danger" size="small" @click="handleReject(row.id)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && applications.length === 0" description="暂无待审核的课程申请" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseAppApi } from '@/api/courseApplication'

const applications = ref([])
const loading = ref(false)

async function loadApplications() {
  loading.value = true
  try {
    const res = await courseAppApi.getPending()
    applications.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleApprove(id) {
  try {
    const { value } = await ElMessageBox.prompt('审核备注（可选）', '通过课程申请', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '审核通过后将自动创建课程并发布评价'
    })
    await courseAppApi.approve(id, value || '')
    ElMessage.success('已通过，课程和评价已创建')
    await loadApplications()
  } catch (e) { /* cancel */ }
}

async function handleReject(id) {
  try {
    const { value } = await ElMessageBox.prompt('请填写拒绝原因', '拒绝课程申请', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPattern: /\S+/,
      inputErrorMessage: '请填写拒绝原因'
    })
    await courseAppApi.reject(id, value.trim())
    ElMessage.success('已拒绝')
    await loadApplications()
  } catch (e) { /* cancel */ }
}

function formatTime(t) {
  return t ? new Date(t).toLocaleString('zh-CN') : ''
}

onMounted(loadApplications)
</script>

<style scoped>
.cell-course { display: flex; flex-direction: column; }
.cell-code { font-size: 12px; color: var(--text-3); font-family: monospace; }
</style>
