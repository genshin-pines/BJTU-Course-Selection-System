<template>
  <div class="admin-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 评价审核 -->
      <el-tab-pane label="评价审核" name="reviews">
        <el-table :data="pendingReviews" v-loading="reviewLoading" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="courseName" label="课程" width="180" />
          <el-table-column prop="teacherName" label="教师" width="120" />
          <el-table-column label="评分" width="150">
            <template #default="{ row }">
              综合: {{ row.overallScore }} | 难度: {{ row.difficultyScore }} | 给分: {{ row.gradingScore }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
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

      <!-- 举报管理 -->
      <el-tab-pane label="举报管理" name="reports">
        <el-table :data="reports" v-loading="reportLoading" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="reviewContent" label="被举报内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="reason" label="举报原因" width="200" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'RESOLVED' ? 'success' : 'info'">
                {{ row.status === 'PENDING' ? '待处理' : row.status === 'RESOLVED' ? '已处理' : '已驳回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" type="primary" size="small" @click="resolveReport(row.id)">
                采纳（删除评价）
              </el-button>
              <el-button v-if="row.status === 'PENDING'" type="warning" size="small" @click="dismissReport(row.id)">
                驳回
              </el-button>
              <span v-else style="color: #999">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!reportLoading && reports.length === 0" description="暂无举报" />
      </el-tab-pane>

      <!-- 标签管理 -->
      <el-tab-pane label="标签管理" name="tags">
        <div class="tag-header">
          <el-input v-model="newTagName" placeholder="输入新标签名" style="width: 240px" />
          <el-button type="primary" @click="handleCreateTag">添加标签</el-button>
        </div>
        <div class="tag-list" style="margin-top: 16px">
          <el-tag
            v-for="tag in tags"
            :key="tag.id"
            closable
            size="large"
            @close="handleDeleteTag(tag.id)"
            style="margin: 4px"
          >
            {{ tag.tagName }}
          </el-tag>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { tagApi } from '@/api/tag'

const activeTab = ref('reviews')

// 评价审核
const pendingReviews = ref([])
const reviewLoading = ref(false)

// 举报管理
const reports = ref([])
const reportLoading = ref(false)

// 标签
const tags = ref([])
const newTagName = ref('')

async function loadPendingReviews() {
  reviewLoading.value = true
  try {
    const res = await adminApi.getPendingReviews()
    pendingReviews.value = res.data || []
  } catch (e) { console.error(e) }
  finally { reviewLoading.value = false }
}

async function approveReview(id) {
  try {
    await adminApi.approveReview(id)
    ElMessage.success('已通过')
    loadPendingReviews()
  } catch (e) { /* handled */ }
}

async function rejectReview(id) {
  try {
    await adminApi.rejectReview(id)
    ElMessage.success('已拒绝')
    loadPendingReviews()
  } catch (e) { /* handled */ }
}

async function loadReports() {
  reportLoading.value = true
  try {
    const res = await adminApi.getAllReports()
    reports.value = res.data || []
  } catch (e) { console.error(e) }
  finally { reportLoading.value = false }
}

async function resolveReport(id) {
  try {
    await ElMessageBox.confirm('采纳举报将删除该评价，确定？', '确认', { type: 'warning' })
    await adminApi.resolveReport(id)
    ElMessage.success('已处理')
    loadReports()
  } catch (e) { /* cancel or error */ }
}

async function dismissReport(id) {
  try {
    await adminApi.dismissReport(id)
    ElMessage.success('已驳回')
    loadReports()
  } catch (e) { /* handled */ }
}

async function loadTags() {
  try {
    const res = await tagApi.getAll()
    tags.value = res.data || []
  } catch (e) { console.error(e) }
}

async function handleCreateTag() {
  if (!newTagName.value.trim()) {
    ElMessage.warning('请输入标签名')
    return
  }
  try {
    await adminApi.createTag(newTagName.value.trim())
    ElMessage.success('标签添加成功')
    newTagName.value = ''
    loadTags()
  } catch (e) { /* handled */ }
}

async function handleDeleteTag(id) {
  try {
    await adminApi.deleteTag(id)
    ElMessage.success('标签已删除')
    loadTags()
  } catch (e) { /* handled */ }
}

// 切换 Tab 时加载对应数据
watch(activeTab, (tab) => {
  if (tab === 'reviews') loadPendingReviews()
  if (tab === 'reports') loadReports()
  if (tab === 'tags') loadTags()
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
</style>
