<template>
  <div class="detail-page" v-loading="loading">
    <!-- 课程信息 Hero -->
    <section class="course-hero" v-if="course">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-left">
          <div class="hero-tags">
            <span class="code-chip">{{ course.courseCode }}</span>
            <span class="dept-chip">{{ course.department }}</span>
          </div>
          <h1 class="hero-title">{{ course.courseName }}</h1>
          <div class="hero-meta">
            <span class="meta-pill"><el-icon><Avatar /></el-icon>{{ course.teacherName || '待定教师' }}</span>
            <span class="meta-pill"><el-icon><Reading /></el-icon>{{ course.credit }} 学分</span>
          </div>
        </div>
        <div class="hero-action">
          <el-button type="primary" size="large" round @click="goPostReview">
            <el-icon><EditPen /></el-icon>
            {{ myReview ? '修改评价' : '写评价' }}
          </el-button>
        </div>
      </div>
    </section>

    <!-- 评分汇总 -->
    <section class="summary-section" v-if="course">
      <RatingSummary :course="course" />
    </section>

    <el-alert
      v-if="myReview?.status === 'HIDDEN'"
      type="warning"
      :closable="false"
      show-icon
      :title="`您的评价已被隐藏${myReview.hideReason ? `（${myReview.hideReason}）` : ''}，请修改后重新提交`"
      class="hide-alert"
    />

    <!-- 评价列表 -->
    <section class="reviews-section">
      <ReviewToolbar
        v-model:sortBy="reviewSortBy"
        v-model:selectedTagIds="selectedTagIds"
        :tags="tags"
        @change="loadReviews"
      />

      <div class="review-list">
        <el-empty v-if="reviews.length === 0" description="暂无评价，快来写第一条吧！" />
        <ReviewCard
          v-for="review in reviews"
          :key="review.id"
          :review="review"
          :voting="votingReviewIds.has(review.id)"
          :show-vote="true"
          :show-report="true"
          @like="handleLike(review)"
          @downvote="handleDownvote(review)"
          @report="showReportDialog(review)"
          @edit="goEditReview(review.id)"
        />
      </div>
    </section>

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
import { Avatar, Reading, EditPen } from '@element-plus/icons-vue'
import { courseApi } from '@/api/course'
import { reviewApi } from '@/api/review'
import { reportApi } from '@/api/report'
import { tagApi } from '@/api/tag'
import { useAuthStore } from '@/stores/auth'
import RatingSummary from '@/components/review/RatingSummary.vue'
import ReviewToolbar from '@/components/review/ReviewToolbar.vue'
import ReviewCard from '@/components/review/ReviewCard.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const instanceId = ref(Number(route.params.instanceId))
const course = ref(null)
const reviews = ref([])
const reviewSortBy = ref('quality')
const selectedTagIds = ref([])
const tags = ref([])
const votingReviewIds = ref(new Set())
const loading = ref(false)

const reportVisible = ref(false)
const currentReview = ref(null)
const reportForm = ref({ reason: '' })
const myReview = ref(null)

async function loadData() {
  loading.value = true
  try {
    const courseRes = await courseApi.getInstanceDetail(instanceId.value)
    course.value = courseRes.data
    await loadReviews()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  const reviewRes = await reviewApi.getByInstance(
    instanceId.value,
    reviewSortBy.value,
    selectedTagIds.value
  )
  const list = reviewRes.data || []
  // 按发布时间升序分配序号，生成"交大学子_N"匿名展示名
  const chrono = [...list].sort((a, b) => {
    const ta = a.createTime ? new Date(a.createTime).getTime() : 0
    const tb = b.createTime ? new Date(b.createTime).getTime() : 0
    return ta - tb
  })
  const seqMap = new Map()
  chrono.forEach((r, idx) => {
    seqMap.set(r.id, idx + 1)
  })
  list.forEach((r) => {
    r.displayName = `交大学子_${seqMap.get(r.id) || 0}`
  })
  reviews.value = list
  await Promise.all([markVoteStates(), loadMyReview()])
}

async function loadMyReview() {
  myReview.value = null
  const valid = await authStore.verifySession()
  if (!valid || !authStore.isStudent) return
  if (!course.value) return
  try {
    const res = await reviewApi.getMine({
      courseId: course.value.id,
      courseInstanceId: instanceId.value,
      teacherId: course.value.teacherId
    })
    myReview.value = res.data || null
  } catch (error) {
    myReview.value = null
  }
}

async function loadTags() {
  try {
    const res = await tagApi.getAll()
    tags.value = res.data || []
  } catch (e) { console.error(e) }
}

async function markVoteStates() {
  const valid = await authStore.verifySession()
  if (!valid || !authStore.isStudent || reviews.value.length === 0) return
  const [likedRes, downvotedRes] = await Promise.all([
    reviewApi.getLikedByInstance(instanceId.value),
    reviewApi.getDownvotedByInstance(instanceId.value)
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
  if (!await ensureStudent()) return
  if (myReview.value?.id) { goEditReview(myReview.value.id); return }
  router.push(`/post-review/${instanceId.value}`)
}

async function goEditReview(reviewId) {
  if (!await ensureStudent()) return
  router.push(`/edit-review/${reviewId}`)
}

async function handleLike(review) {
  if (!await ensureStudent()) return
  if (votingReviewIds.value.has(review.id)) return
  setReviewVoting(review.id, true)
  try {
    const res = await reviewApi.like(review.id)
    applyVoteResult(review, res.data)
  } catch (e) { showVoteError(e) } finally { setReviewVoting(review.id, false) }
}

async function handleDownvote(review) {
  if (!await ensureStudent()) return
  if (votingReviewIds.value.has(review.id)) return
  setReviewVoting(review.id, true)
  try {
    const res = await reviewApi.downvote(review.id)
    applyVoteResult(review, res.data)
  } catch (e) { showVoteError(e) } finally { setReviewVoting(review.id, false) }
}

function setReviewVoting(reviewId, voting) {
  const next = new Set(votingReviewIds.value)
  if (voting) next.add(reviewId); else next.delete(reviewId)
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
  if (!await ensureStudent()) return
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
    await reportApi.submit({ reviewId: currentReview.value.id, reason: reportForm.value.reason.trim() })
    ElMessage.success('举报已提交')
    reportVisible.value = false
  } catch (e) {}
}

onMounted(async () => {
  await Promise.all([loadTags(), loadData()])
})
</script>

<style scoped>
.detail-page {
  max-width: 940px;
  margin: 0 auto;
  padding: 0 0 48px;
}

/* —— 课程 Hero —— */
.course-hero {
  position: relative;
  border-radius: var(--r-lg);
  overflow: hidden;
  margin: 0 16px 20px;
  box-shadow: var(--shadow-md);
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: var(--brand-gradient-vibrant);
}
.hero-bg::before,
.hero-bg::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.45;
}
.hero-bg::before {
  width: 280px; height: 280px;
  background: #7dd3fc;
  top: -80px; right: -40px;
}
.hero-bg::after {
  width: 240px; height: 240px;
  background: #60a5fa;
  bottom: -80px; left: -40px;
}
.hero-content {
  position: relative;
  z-index: 2;
  padding: 32px 36px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  color: #fff;
}
.hero-left {
  flex: 1;
  min-width: 0;
}
.hero-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.code-chip {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: var(--r-pill);
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(8px);
}
.dept-chip {
  font-size: 12px;
  padding: 3px 12px;
  border-radius: var(--r-pill);
  background: rgba(0,0,0,0.18);
}
.hero-title {
  font-size: 30px;
  font-weight: 800;
  margin-bottom: 14px;
  line-height: 1.25;
}
.hero-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  padding: 5px 14px;
  border-radius: var(--r-pill);
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(8px);
}
.hero-action {
  flex-shrink: 0;
}

/* —— 汇总区 —— */
.summary-section {
  margin: 0 16px 20px;
}

.hide-alert {
  margin: 0 16px 20px;
  border-radius: var(--r-md);
}

/* —— 评价区 —— */
.reviews-section {
  margin: 0 16px;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .course-hero,
  .summary-section,
  .hide-alert,
  .reviews-section {
    margin-left: 12px;
    margin-right: 12px;
  }
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
    padding: 24px 22px;
  }
  .hero-title {
    font-size: 24px;
  }
  .hero-action {
    width: 100%;
  }
  .hero-action .el-button {
    width: 100%;
  }
}
</style>
