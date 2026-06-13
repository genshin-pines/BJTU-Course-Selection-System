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
        <el-button type="primary" :disabled="!selectedCourseInstanceId" @click="goPostReview">写评价</el-button>
      </div>
    </el-card>

    <el-card class="instances-card" v-if="courseInstances.length">
      <template #header>
        <div class="instances-header">
          <div class="section-title">开课实例</div>
          <el-radio-group v-model="selectedCourseInstanceId" size="small" @change="loadReviews">
            <el-radio-button :label="null">全部评价</el-radio-button>
            <el-radio-button
              v-for="instance in courseInstances"
              :key="instance.id"
              :label="instance.id"
            >
              {{ instance.semester }} / {{ instance.teacherName || '未分配教师' }}
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="courseInstances" size="small">
        <el-table-column prop="semester" label="学期" min-width="120" />
        <el-table-column prop="teacherName" label="授课教师" min-width="120" />
        <el-table-column prop="className" label="班级" min-width="120">
          <template #default="{ row }">
            {{ row.className || '未设置' }}
          </template>
        </el-table-column>
        <el-table-column prop="reviewCount" label="评价数" width="90" />
        <el-table-column label="评分" width="90">
          <template #default="{ row }">
            {{ row.avgScore?.toFixed(1) || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="reviews-section">
      <div class="reviews-toolbar">
        <h2>课程评价</h2>
        <div class="reviews-filters">
          <el-select
            v-model="selectedTagIds"
            size="small"
            placeholder="按标签筛选"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            style="width: 220px"
            @change="loadReviews"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.tagName"
              :value="tag.id"
            />
          </el-select>
          <el-select
            v-model="reviewSortBy"
            size="small"
            style="width: 180px"
            @change="loadReviews"
          >
            <el-option label="质量优先" value="quality" />
            <el-option label="最新优先" value="latest" />
            <el-option label="高分优先" value="highScore" />
            <el-option label="有用优先" value="useful" />
            <el-option label="争议优先" value="controversial" />
          </el-select>
        </div>
      </div>
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

        <div class="review-meta" v-if="review.keyChapters">
          <strong>重点章节：</strong>{{ review.keyChapters }}
        </div>
        <div class="review-meta" v-if="review.cheatSheetAllowed !== null && review.cheatSheetAllowed !== undefined">
          <strong>可带资料：</strong>{{ review.cheatSheetAllowed ? '可以' : '不可以' }}
        </div>

        <div class="review-actions">
          <el-button
            text
            :type="review.liked ? 'primary' : ''"
            :loading="votingReviewIds.has(review.id)"
            :disabled="votingReviewIds.has(review.id)"
            @click="handleLike(review)"
          >
            <el-icon><CaretTop /></el-icon>
            有用 {{ review.likeCount || 0 }}
          </el-button>
          <el-button
            text
            :type="review.downvoted ? 'warning' : ''"
            :loading="votingReviewIds.has(review.id)"
            :disabled="votingReviewIds.has(review.id)"
            @click="handleDownvote(review)"
          >
            <el-icon><CaretBottom /></el-icon>
            没用 {{ review.downvoteCount || 0 }}
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
import { tagApi } from '@/api/tag'
import { useAuthStore } from '@/stores/auth'
import { CaretBottom, CaretTop, User, Warning } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const course = ref(null)
const courseInstances = ref([])
const selectedCourseInstanceId = ref(null)
const currentCourseId = ref(Number(route.params.id))
const reviews = ref([])
const reviewSortBy = ref('quality')
const selectedTagIds = ref([])
const tags = ref([])
const votingReviewIds = ref(new Set())
const loading = ref(false)

const reportVisible = ref(false)
const currentReview = ref(null)
const reportForm = ref({ reason: '' })

async function loadData() {
  loading.value = true
  try {
    const routeInstanceId = route.query.instanceId ? Number(route.query.instanceId) : null
    const courseRes = routeInstanceId
      ? await courseApi.getInstanceDetail(routeInstanceId)
      : await courseApi.getDetail(route.params.id)
    course.value = courseRes.data
    currentCourseId.value = course.value?.id || null
    if (currentCourseId.value) {
      const instanceRes = await courseApi.getInstances(currentCourseId.value).catch((error) => {
        console.warn('Failed to load course instances:', error)
        return { data: [] }
      })
      courseInstances.value = instanceRes.data || []
    } else {
      courseInstances.value = [{
        id: course.value.courseInstanceId,
        courseBaseId: course.value.courseBaseId,
        legacyCourseId: null,
        teacherId: course.value.teacherId,
        teacherName: course.value.teacherName,
        semester: course.value.semester,
        className: course.value.className,
        avgScore: course.value.avgScore,
        gradingScore: course.value.gradingScore,
        avgTeachingScore: course.value.avgTeachingScore,
        avgWorkloadScore: course.value.avgWorkloadScore,
        reviewCount: course.value.reviewCount
      }].filter((instance) => instance.id)
    }
    selectedCourseInstanceId.value = routeInstanceId || course.value?.courseInstanceId || courseInstances.value[0]?.id || null
    await loadReviews()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  if (!currentCourseId.value && !selectedCourseInstanceId.value) {
    reviews.value = []
    return
  }
  const reviewRes = currentCourseId.value
    ? await reviewApi.getByCourse(
        currentCourseId.value,
        selectedCourseInstanceId.value,
        reviewSortBy.value,
        selectedTagIds.value
      )
    : await reviewApi.getByInstance(selectedCourseInstanceId.value, reviewSortBy.value, selectedTagIds.value)
  reviews.value = reviewRes.data || []
  await markVoteStates()
}

async function loadTags() {
  try {
    const res = await tagApi.getAll()
    tags.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function markVoteStates() {
  const valid = await authStore.verifySession()
  if (!valid || !authStore.isStudent || reviews.value.length === 0) {
    return
  }
  const [likedRes, downvotedRes] = currentCourseId.value
    ? await Promise.all([
        reviewApi.getLikedByCourse(currentCourseId.value, selectedCourseInstanceId.value),
        reviewApi.getDownvotedByCourse(currentCourseId.value, selectedCourseInstanceId.value)
      ])
    : await Promise.all([
        reviewApi.getLikedByInstance(selectedCourseInstanceId.value),
        reviewApi.getDownvotedByInstance(selectedCourseInstanceId.value)
      ])
  const likedIds = new Set(likedRes.data || [])
  const downvotedIds = new Set(downvotedRes.data || [])
  reviews.value = reviews.value.map((review) => ({
    ...review,
    liked: likedIds.has(review.id),
    downvoted: downvotedIds.has(review.id)
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
  if (!selectedCourseInstanceId.value) {
    ElMessage.warning('请选择开课实例')
    return
  }
  if (!await ensureStudent()) {
    return
  }
  if (currentCourseId.value) {
    router.push({
      path: `/post-review/${currentCourseId.value}`,
      query: { instanceId: selectedCourseInstanceId.value }
    })
    return
  }
  router.push(`/post-review/instance/${selectedCourseInstanceId.value}`)
}

async function handleLike(review) {
  if (!await ensureStudent()) {
    return
  }
  if (votingReviewIds.value.has(review.id)) {
    return
  }
  setReviewVoting(review.id, true)
  try {
    const res = await reviewApi.like(review.id)
    applyVoteResult(review, res.data)
  } catch (e) {
    showVoteError(e)
  } finally {
    setReviewVoting(review.id, false)
  }
}

async function handleDownvote(review) {
  if (!await ensureStudent()) {
    return
  }
  if (votingReviewIds.value.has(review.id)) {
    return
  }
  setReviewVoting(review.id, true)
  try {
    const res = await reviewApi.downvote(review.id)
    applyVoteResult(review, res.data)
  } catch (e) {
    showVoteError(e)
  } finally {
    setReviewVoting(review.id, false)
  }
}

function setReviewVoting(reviewId, voting) {
  const next = new Set(votingReviewIds.value)
  if (voting) {
    next.add(reviewId)
  } else {
    next.delete(reviewId)
  }
  votingReviewIds.value = next
}

function applyVoteResult(review, result) {
  review.likeCount = result.likeCount
  review.downvoteCount = result.downvoteCount
  review.liked = result.liked
  review.downvoted = result.downvoted
}

function showVoteError(error) {
  ElMessage.warning(error.response?.data?.message || error.message || '投票失败，请稍后重试')
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

onMounted(async () => {
  await Promise.all([loadTags(), loadData()])
})
</script>

<style scoped>
.detail-container {
  max-width: 900px;
  margin: 0 auto;
}

.course-header {
  margin-bottom: 24px;
}

.instances-card {
  margin-bottom: 24px;
}

.instances-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.section-title {
  font-weight: 700;
  color: #333;
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

.reviews-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.reviews-toolbar h2 {
  font-size: 20px;
  margin-bottom: 0;
  color: #333;
}

.reviews-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
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

@media (max-width: 640px) {
  .reviews-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .reviews-filters {
    justify-content: flex-start;
    width: 100%;
  }
}
</style>
