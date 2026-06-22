<template>
  <div>
    <div class="section-toolbar search-bar audit-search-bar">
      <el-select v-model="auditFilter.operateType" placeholder="操作类型" style="width: 190px" clearable>
        <el-option
          v-for="item in auditOperateOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-input v-model="auditFilter.operatorId" placeholder="管理员 ID" style="width: 130px" clearable
        @keyup.enter="searchAuditLogs" />
      <el-input v-model="auditFilter.reviewId" placeholder="评价 ID" style="width: 130px" clearable
        @keyup.enter="searchAuditLogs" />
      <el-input v-model="auditFilter.courseName" placeholder="课程名称" style="width: 180px" clearable
        @keyup.enter="searchAuditLogs" />
      <el-date-picker
        v-model="auditFilter.timeRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        format="YYYY-MM-DD HH:mm"
        value-format="YYYY-MM-DDTHH:mm:ss"
        style="width: 360px"
      />
      <el-button type="primary" @click="searchAuditLogs">搜索</el-button>
      <el-button @click="resetAuditFilter">重置</el-button>
    </div>
    <el-table :data="auditLogs" v-loading="auditLogLoading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="adminId" label="管理员 ID" width="110" />
      <el-table-column prop="operateType" label="操作类型" width="150">
        <template #default="{ row }">{{ operateTypeText(row.operateType) }}</template>
      </el-table-column>
      <el-table-column prop="reviewId" label="评价 ID" width="100" />
      <el-table-column prop="reportId" label="举报 ID" width="100" />
      <el-table-column prop="courseName" label="课程" width="180" />
      <el-table-column prop="teacherName" label="教师" width="120" />
      <el-table-column label="开课实例" width="190">
        <template #default="{ row }">{{ formatInstance(row) }}</template>
      </el-table-column>
      <el-table-column prop="anonymousId" label="评价用户" width="140" />
      <el-table-column prop="reviewContent" label="评价摘要" min-width="180" show-overflow-tooltip />
      <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作时间" width="190">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'

const auditLogs = ref([])
const auditLogLoading = ref(false)
const auditFilter = ref(blankAuditFilter())

const auditOperateOptions = [
  'APPROVE_REVIEW',
  'REJECT_REVIEW',
  'HIDE_REVIEW',
  'DELETE_REVIEW',
  'RESOLVE_REPORT',
  'DISMISS_REPORT',
  'CREATE_TAG',
  'DELETE_TAG',
  'CREATE_COURSE',
  'UPDATE_COURSE',
  'DELETE_COURSE',
  'CREATE_TEACHER',
  'UPDATE_TEACHER',
  'DELETE_TEACHER',
  'CREATE_COURSE_INSTANCE',
  'UPDATE_COURSE_INSTANCE',
  'DELETE_COURSE_INSTANCE',
  'IMPORT_COURSES',
  'CREATE_ADMIN',
  'UPDATE_ADMIN_ROLE',
  'RESET_ADMIN_PASSWORD',
  'DELETE_ADMIN'
].map(value => ({ value, label: operateTypeText(value) }))

function blankAuditFilter() {
  return {
    operateType: '',
    operatorId: '',
    reviewId: '',
    courseName: '',
    timeRange: []
  }
}

function buildAuditParams() {
  const params = {}
  if (auditFilter.value.operateType) params.operateType = auditFilter.value.operateType
  if (auditFilter.value.operatorId) params.operatorId = auditFilter.value.operatorId
  if (auditFilter.value.reviewId) params.reviewId = auditFilter.value.reviewId
  if (auditFilter.value.courseName) params.courseName = auditFilter.value.courseName
  if (auditFilter.value.timeRange?.length === 2) {
    params.startTime = auditFilter.value.timeRange[0]
    params.endTime = auditFilter.value.timeRange[1]
  }
  return params
}

async function loadAuditLogs() {
  auditLogLoading.value = true
  try {
    const res = await adminApi.getAuditLogs(buildAuditParams())
    auditLogs.value = res.data || []
  } finally {
    auditLogLoading.value = false
  }
}

function searchAuditLogs() {
  loadAuditLogs()
}

function resetAuditFilter() {
  auditFilter.value = blankAuditFilter()
  loadAuditLogs()
}

function operateTypeText(type) {
  const map = {
    APPROVE_REVIEW: '审核通过',
    REJECT_REVIEW: '审核拒绝',
    HIDE_REVIEW: '审核拒绝',
    DELETE_REVIEW: '删除评价',
    RESOLVE_REPORT: '采纳举报',
    DISMISS_REPORT: '驳回举报',
    CREATE_TAG: '新增标签',
    DELETE_TAG: '删除标签',
    CREATE_COURSE: '新增课程',
    UPDATE_COURSE: '更新课程',
    DELETE_COURSE: '删除课程',
    CREATE_TEACHER: '新增教师',
    UPDATE_TEACHER: '更新教师',
    DELETE_TEACHER: '删除教师',
    CREATE_COURSE_INSTANCE: '新增开课实例',
    UPDATE_COURSE_INSTANCE: '更新开课实例',
    DELETE_COURSE_INSTANCE: '删除开课实例',
    IMPORT_COURSES: '导入课程数据',
    CREATE_ADMIN: '新增管理员',
    UPDATE_ADMIN_ROLE: '更新管理员角色',
    RESET_ADMIN_PASSWORD: '重置管理员密码',
    DELETE_ADMIN: '删除管理员'
  }
  return map[type] || type || '-'
}

function formatInstance(row) {
  const parts = []
  if (row.courseInstanceId) parts.push(`#${row.courseInstanceId}`)
  return parts.length ? parts.join(' / ') : '-'
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadAuditLogs)
</script>
