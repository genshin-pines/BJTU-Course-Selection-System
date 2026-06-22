<template>
  <div>
    <div class="section-toolbar search-bar">
      <el-select v-model="allReviewFilter.status" placeholder="状态" style="width: 160px" clearable>
        <el-option
          v-for="item in reviewStatusOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-input v-model="allReviewFilter.courseName" placeholder="课程名称" style="width: 180px" clearable
        @keyup.enter="searchAllReviews" />
      <el-input v-model="allReviewFilter.teacherName" placeholder="教师姓名" style="width: 160px" clearable
        @keyup.enter="searchAllReviews" />
      <el-input v-model="allReviewFilter.department" placeholder="学院" style="width: 160px" clearable
        @keyup.enter="searchAllReviews" />
      <el-date-picker
        v-model="allReviewFilter.timeRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        format="YYYY-MM-DD HH:mm"
        value-format="YYYY-MM-DDTHH:mm:ss"
        style="width: 360px"
      />
      <el-button type="primary" @click="searchAllReviews">搜索</el-button>
      <el-button @click="resetAllReviewFilter">重置</el-button>
    </div>
    <el-table :data="allReviews" v-loading="allReviewLoading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="courseName" label="课程" width="180" />
      <el-table-column prop="teacherName" label="教师" width="120" />
      <el-table-column prop="department" label="学院" width="160">
        <template #default="{ row }">{{ row.department || '-' }}</template>
      </el-table-column>
      <el-table-column label="开课实例" width="190">
        <template #default="{ row }">{{ formatInstance(row) }}</template>
      </el-table-column>
      <el-table-column label="评分" width="120">
        <template #default="{ row }">{{ row.overallScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="anonymousId" label="用户" width="140" />
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="reviewStatusType(row.status)">{{ reviewStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="hideReason" label="隐藏原因" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.hideReason || '-' }}</template>
      </el-table-column>
      <el-table-column label="发布时间" width="190">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDeleteAllReview(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-pagination">
      <el-pagination
        v-model:current-page="allReviewPage.current"
        v-model:page-size="allReviewPage.size"
        :page-sizes="pageSizeOptions"
        :total="allReviewPage.total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleAllReviewSizeChange"
        @current-change="handleAllReviewPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const pageSizeOptions = [10, 20, 50, 100]

const reviewStatusOptions = [
  { value: 'PUBLISHED', label: '已发布待审核' },
  { value: 'APPROVED', label: '审核通过' },
  { value: 'HIDDEN', label: '已隐藏' }
]

const allReviews = ref([])
const allReviewLoading = ref(false)
const allReviewFilter = ref(blankAllReviewFilter())
const allReviewPage = ref(blankPageState())

function blankPageState() {
  return { current: 1, size: 10, total: 0 }
}

function blankAllReviewFilter() {
  return {
    status: '',
    courseName: '',
    teacherName: '',
    department: '',
    timeRange: []
  }
}

function applyPageResult(data, recordsRef, pageRef) {
  recordsRef.value = data?.records || []
  pageRef.value.total = Number(data?.total || 0)
  pageRef.value.current = Number(data?.page || pageRef.value.current || 1)
  pageRef.value.size = Number(data?.size || pageRef.value.size || 10)
}

function buildAllReviewQueryParams() {
  const params = {
    page: allReviewPage.value.current,
    pageSize: allReviewPage.value.size
  }
  if (allReviewFilter.value.status) params.status = allReviewFilter.value.status
  if (allReviewFilter.value.courseName?.trim()) params.courseName = allReviewFilter.value.courseName.trim()
  if (allReviewFilter.value.teacherName?.trim()) params.teacherName = allReviewFilter.value.teacherName.trim()
  if (allReviewFilter.value.department?.trim()) params.department = allReviewFilter.value.department.trim()
  if (allReviewFilter.value.timeRange?.length === 2) {
    params.startTime = allReviewFilter.value.timeRange[0]
    params.endTime = allReviewFilter.value.timeRange[1]
  }
  return params
}

async function loadAllReviews() {
  allReviewLoading.value = true
  try {
    const res = await adminApi.getAdminReviews(buildAllReviewQueryParams())
    applyPageResult(res.data, allReviews, allReviewPage)
  } finally {
    allReviewLoading.value = false
  }
}

function searchAllReviews() {
  allReviewPage.value.current = 1
  loadAllReviews()
}

function resetAllReviewFilter() {
  allReviewFilter.value = blankAllReviewFilter()
  allReviewPage.value.current = 1
  loadAllReviews()
}

function handleAllReviewPageChange(page) {
  allReviewPage.value.current = page
  loadAllReviews()
}

function handleAllReviewSizeChange(size) {
  allReviewPage.value.size = size
  allReviewPage.value.current = 1
  loadAllReviews()
}

async function handleDeleteAllReview(id) {
  try {
    await ElMessageBox.confirm('确认删除该评价？删除后不可恢复', '删除评价', { type: 'warning' })
    await adminApi.deleteReview(id)
    ElMessage.success('已删除')
    await loadAllReviews()
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

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadAllReviews)
</script>
