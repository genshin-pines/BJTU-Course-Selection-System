<template>
  <div class="detail-container" v-loading="loading">
    <!-- 课程信息 -->
    <el-card class="course-header" v-if="course">
      <div class="header-main">
        <div class="header-info">
          <h1>
            {{ course.courseName }}
            <el-tag>{{ course.courseCode }}</el-tag>
          </h1>
          <p class="meta">
            <el-icon><User /></el-icon>
            {{ course.teacherName || '未分配教师' }}
            <el-divider direction="vertical" />
            {{ course.department }}
            <el-divider direction="vertical" />
            {{ course.credit }} 学分
            <el-divider direction="vertical" />
            {{ course.reviewCount }} 条评价
          </p>
        </div>
        <div class="header-scores">
          <div class="big-score">
            <span class="big-number">{{ course.avgScore?.toFixed(1) || '-' }}</span>
            <span class="big-label">综合评分</span>
          </div>
          <div class="sub-scores">
            <span>难度 {{ course.difficultyScore?.toFixed(1) || '-' }}</span>
            <span>给分 {{ course.gradingScore?.toFixed(1) || '-' }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="goPostReview">写评价</el-button>
      </div>
    </el-card>

    <!-- 评价列表 -->
    <div class="reviews-section">
      <h2>课程评价</h2>
      <el-empty v-if="reviews.length === 0" description="暂无评价，快来写第一条吧!" />

      <el-card v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-header">
          <div class="reviewer">
            <el-avatar :size="36" icon="UserFilled" />
            <div>
              <div class="reviewer-name">{{ review.anonymousId }}</div>
              <div class="review-time">{{ formatTime(review.createTime) }}</div>
            </div>
          </div>
          <div class="review-scores">
            <el-tag type="primary">综合 {{ review.overallScore }}</el-tag>
            <el-tag type="warning">难度 {{ review.difficultyScore }}</el-tag>
            <el-tag type="success">给分 {{ review.gradingScore }}</el-tag>
          </div>
        </div>

        <div class="review-content">{{ review.content }}</div>

        <div class="review-tags" v-if="review.tags?.length">
          <el-tag v-for="tag in review.tags" :key="tag.id" size="small" type="info">
            {{ tag.tagName }}
          </el-tag>
        </div>

        <div class="review-meta" v-if="review.studyTips">
          <strong>学习建议：</strong>{{ review.studyTips }}
        </div>
        <div class="review-meta" v-if="review.examType">
          <strong>考核方式：</strong>{{ review.examType }}
        </div>

        <div class="review-actions">
          <el-button text @click="handleLike(review)">
            <el-icon><CaretTop /></el-icon>
            {{ review.likeCount || 0 }}
          </el-button>
          <el-button text type="danger" @click="showReportDialog(review)">
            <el-icon><Warning /></el-icon>
            举报
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportVisible" title="举报评价" width="480px">
      <el-form :model="reportForm">
        <el-form-item label="举报原因">
          <el-input v-model="reportForm.reason" type="textarea" :rows="3" placeholder="请描述举报原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { courseApi } from '@/api/course'
import { reviewApi } from '@/api/review'
import { reportApi } from '@/api/report'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const course = ref(null)
const reviews = ref([])
const loading = ref(false)

const reportVisible = ref(false)
const currentReview = ref(null)
const reportForm = ref({ reason: '' })

async function loadData() {
  loading.value = true
  try {
    const [courseRes, reviewRes] = await Promise.all([
      courseApi.getDetail(route.params.id),
      reviewApi.getByCourse(route.params.id)
    ])
    course.value = courseRes.data
    reviews.value = reviewRes.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function goPostReview() {
  if (!authStore.token) {
    ElMessage.warning('请先登录再写评价')
    router.push('/login')
    return
  }
  router.push(`/post-review/${route.params.id}`)
}

function handleLike(review) {
  if (!authStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  reviewApi.like(review.id).then(() => {
    review.likeCount = (review.likeCount || 0) + 1
  })
}

function showReportDialog(review) {
  if (!authStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  currentReview.value = review
  reportForm.value.reason = ''
  reportVisible.value = true
}

async function handleReport() {
  if (!reportForm.value.reason.trim()) {
    ElMessage.warning('请输入举报原因')
    return
  }
  try {
    await reportApi.submit({
      reviewId: currentReview.value.id,
      reason: reportForm.value.reason
    })
    ElMessage.success('举报已提交')
    reportVisible.value = false
  } catch (e) {
    // error handled globally
  }
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>

<style scoped>
.detail-container {
  max-width: 900px;
  margin: 0 auto;
}

.course-header {
  margin-bottom: 24px;
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-info h1 {
  font-size: 24px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta {
  color: #888;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-scores {
  text-align: center;
}

.big-score {
  background: linear-gradient(135deg, #1a73e8, #4a90d9);
  color: white;
  padding: 16px 24px;
  border-radius: 12px;
  margin-bottom: 8px;
}

.big-number {
  font-size: 36px;
  font-weight: 700;
  display: block;
}

.big-label {
  font-size: 13px;
  opacity: 0.9;
}

.sub-scores {
  display: flex;
  gap: 16px;
  color: #666;
  font-size: 13px;
  justify-content: center;
}

.header-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.reviews-section h2 {
  font-size: 20px;
  margin-bottom: 16px;
  color: #333;
}

.review-card {
  margin-bottom: 16px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.reviewer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reviewer-name {
  font-weight: 600;
  color: #333;
}

.review-time {
  font-size: 12px;
  color: #999;
}

.review-scores {
  display: flex;
  gap: 6px;
}

.review-content {
  margin: 12px 0;
  line-height: 1.8;
  color: #444;
}

.review-tags {
  margin: 8px 0;
  display: flex;
  gap: 6px;
}

.review-meta {
  margin: 8px 0;
  font-size: 14px;
  color: #666;
}

.review-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 16px;
}
</style>
