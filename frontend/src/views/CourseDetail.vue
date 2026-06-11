<template>
  <div class="detail-container" v-loading="loading">
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
            <span>给分 {{ course.gradingScore?.toFixed(1) || '-' }}</span>
            <span>授课 {{ course.avgTeachingScore?.toFixed(1) || '-' }}</span>
            <span>作业 {{ course.avgWorkloadScore?.toFixed(1) || '-' }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="goPostReview">写评价</el-button>
      </div>
    </el-card>

    <div class="reviews-section">
      <h2>课程评价</h2>
      <el-empty v-if="reviews.length === 0" description="暂无评价，快来写第一条吧！" />

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
            <el-tag type="primary">给分 {{ review.gradingScore }}</el-tag>
            <el-tag type="success">授课 {{ review.teachingScore }}</el-tag>
            <el-tag type="warning">作业 {{ review.workloadScore }}</el-tag>
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
          <el-button text :type="review.liked ? 'primary' : ''" @click="handleLike(review)">
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

    <el-dialog v-model="reportVisible" title="举报评价" width="480px">
      <el-form :model="reportForm">
        <el-form-item label="举报原因">
          <el-input
            v-model="reportForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请描述举报原因"
          />
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
    await markLikedReviews()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function markLikedReviews() {
  const valid = await authStore.verifySession()
  if (!valid || !authStore.isStudent || reviews.value.length === 0) {
    return
  }
  const likedRes = await reviewApi.getLikedByCourse(route.params.id)
  const likedIds = new Set(likedRes.data || [])
  reviews.value = reviews.value.map((review) => ({
    ...review,
    liked: likedIds.has(review.id)
  }))
}

async function ensureStudent() {
  const valid = await authStore.verifySession(true)
  if (!valid || !authStore.isLoggedIn) {
    ElMessage.warning('请先登录学生账号')
    router.push('/login')
    return false
  }
  if (!authStore.isStudent) {
    ElMessage.warning('该操作需要学生账号，请切换到学生登录')
    return false
  }
  return true
}

async function goPostReview() {
  if (!await ensureStudent()) {
    return
  }
  router.push(`/post-review/${route.params.id}`)
}

async function handleLike(review) {
  if (!await ensureStudent()) {
    return
  }
  const res = await reviewApi.like(review.id)
  review.likeCount = res.data.likeCount
  review.liked = res.data.liked
}

async function showReportDialog(review) {
  if (!await ensureStudent()) {
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
      reason: reportForm.value.reason.trim()
    })
    ElMessage.success('举报已提交')
    reportVisible.value = false
  } catch (e) {
    // Global interceptor handles request errors.
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
