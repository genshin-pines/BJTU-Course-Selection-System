<template>
  <div class="admin-container">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="评价审核" name="reviews">
        <el-table :data="pendingReviews" v-loading="reviewLoading" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="courseName" label="课程" width="180" />
          <el-table-column prop="teacherName" label="教师" width="120" />
          <el-table-column label="评分" width="170">
            <template #default="{ row }">
              给分: {{ row.gradingScore }} | 授课: {{ row.teachingScore }} | 作业: {{ row.workloadScore }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="220" show-overflow-tooltip />
          <el-table-column prop="anonymousId" label="用户" width="140" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="approveReview(row.id)">通过</el-button>
              <el-button type="danger" size="small" @click="rejectReview(row.id)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!reviewLoading && pendingReviews.length === 0" description="暂无待审核评价" />
      </el-tab-pane>

      <el-tab-pane label="举报管理" name="reports">
        <el-table :data="reports" v-loading="reportLoading" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="reviewContent" label="被举报内容" min-width="220" show-overflow-tooltip />
          <el-table-column prop="reason" label="举报原因" width="220" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="reportStatusType(row.status)">
                {{ reportStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button type="primary" size="small" @click="resolveReport(row.id)">
                  采纳
                </el-button>
                <el-button type="warning" size="small" @click="dismissReport(row.id)">
                  驳回
                </el-button>
              </template>
              <span v-else class="muted">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!reportLoading && reports.length === 0" description="暂无举报" />
      </el-tab-pane>

      <el-tab-pane label="标签管理" name="tags">
        <div class="tag-header">
          <el-input v-model="newTagName" placeholder="输入新标签名" style="width: 240px" />
          <el-button type="primary" @click="handleCreateTag">添加标签</el-button>
        </div>
        <div class="tag-list">
          <el-tag
            v-for="tag in tags"
            :key="tag.id"
            closable
            size="large"
            @close="handleDeleteTag(tag.id)"
          >
            {{ tag.tagName }}
          </el-tag>
        </div>
      </el-tab-pane>

      <el-tab-pane label="审计日志" name="auditLogs">
        <el-table :data="auditLogs" v-loading="auditLogLoading" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="adminId" label="管理员 ID" width="110" />
          <el-table-column prop="operateType" label="操作类型" width="150">
            <template #default="{ row }">
              {{ operateTypeText(row.operateType) }}
            </template>
          </el-table-column>
          <el-table-column prop="reviewId" label="评价 ID" width="100" />
          <el-table-column prop="reportId" label="举报 ID" width="100" />
          <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作时间" width="190">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!auditLogLoading && auditLogs.length === 0" description="暂无审计日志" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { tagApi } from '@/api/tag'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const activeTab = ref('reviews')

const pendingReviews = ref([])
const reviewLoading = ref(false)

const reports = ref([])
const reportLoading = ref(false)

const tags = ref([])
const newTagName = ref('')

const auditLogs = ref([])
const auditLogLoading = ref(false)

function ensureAdmin() {
  authStore.syncFromStorage()
  if (!authStore.isAdmin) {
    ElMessage.warning('请先登录管理员账号')
    return false
  }
  return true
}

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
  if (!ensureAdmin()) return
  reviewLoading.value = true
  try {
    const res = await adminApi.getPendingReviews()
    pendingReviews.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    reviewLoading.value = false
  }
}

async function approveReview(id) {
  if (!ensureAdmin()) return
  try {
    const reason = await askReason('审核通过', '请填写通过该评价的原因')
    await adminApi.approveReview(id, reason)
    ElMessage.success('已通过')
    await Promise.all([loadPendingReviews(), loadAuditLogsIfActive()])
  } catch (e) {
    // Cancel or global request error.
  }
}

async function rejectReview(id) {
  if (!ensureAdmin()) return
  try {
    const reason = await askReason('审核拒绝', '请填写拒绝该评价的原因')
    await adminApi.rejectReview(id, reason)
    ElMessage.success('已拒绝')
    await Promise.all([loadPendingReviews(), loadAuditLogsIfActive()])
  } catch (e) {
    // Cancel or global request error.
  }
}

async function loadReports() {
  if (!ensureAdmin()) return
  reportLoading.value = true
  try {
    const res = await adminApi.getAllReports()
    reports.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    reportLoading.value = false
  }
}

async function resolveReport(id) {
  if (!ensureAdmin()) return
  try {
    const reason = await askReason('采纳举报', '请填写采纳原因，采纳后将隐藏/删除对应评价')
    await adminApi.resolveReport(id, reason)
    ElMessage.success('举报已采纳')
    await Promise.all([loadReports(), loadAuditLogsIfActive()])
  } catch (e) {
    // Cancel or global request error.
  }
}

async function dismissReport(id) {
  if (!ensureAdmin()) return
  try {
    const reason = await askReason('驳回举报', '请填写驳回该举报的原因')
    await adminApi.dismissReport(id, reason)
    ElMessage.success('举报已驳回')
    await Promise.all([loadReports(), loadAuditLogsIfActive()])
  } catch (e) {
    // Cancel or global request error.
  }
}

async function loadTags() {
  if (!ensureAdmin()) return
  try {
    const res = await tagApi.getAll()
    tags.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function handleCreateTag() {
  if (!ensureAdmin()) return
  if (!newTagName.value.trim()) {
    ElMessage.warning('请输入标签名')
    return
  }
  try {
    await adminApi.createTag(newTagName.value.trim())
    ElMessage.success('标签添加成功')
    newTagName.value = ''
    await loadTags()
  } catch (e) {
    // Global interceptor handles request errors.
  }
}

async function handleDeleteTag(id) {
  if (!ensureAdmin()) return
  try {
    await adminApi.deleteTag(id)
    ElMessage.success('标签已删除')
    await loadTags()
  } catch (e) {
    // Global interceptor handles request errors.
  }
}

async function loadAuditLogs() {
  if (!ensureAdmin()) return
  auditLogLoading.value = true
  try {
    const res = await adminApi.getAuditLogs()
    auditLogs.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    auditLogLoading.value = false
  }
}

function loadAuditLogsIfActive() {
  if (activeTab.value !== 'auditLogs') {
    return Promise.resolve()
  }
  return loadAuditLogs()
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

function operateTypeText(type) {
  const map = {
    APPROVE_REVIEW: '审核通过',
    REJECT_REVIEW: '审核拒绝',
    DELETE_REVIEW: '删除评价',
    RESOLVE_REPORT: '采纳举报',
    DISMISS_REPORT: '驳回举报'
  }
  return map[type] || type || '-'
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

watch(activeTab, (tab) => {
  if (tab === 'reviews') loadPendingReviews()
  if (tab === 'reports') loadReports()
  if (tab === 'tags') loadTags()
  if (tab === 'auditLogs') loadAuditLogs()
})

onMounted(() => {
  loadPendingReviews()
})
</script>

<style scoped>
.admin-container {
  max-width: 1100px;
  margin: 0 auto;
}

.tag-header {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tag-list {
  margin-top: 16px;
}

.tag-list .el-tag {
  margin: 4px;
}

.muted {
  color: #999;
}
</style>
