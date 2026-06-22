<template>
  <div class="profile-page" v-loading="loading">
    <!-- 用户信息卡 -->
    <section class="profile-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="avatar-lg">{{ avatarChar }}</div>
        <div class="hero-info">
          <h1>{{ userInfo?.anonymousId || '同学' }}</h1>
          <p>学号 {{ userInfo?.studentNo || '-' }}</p>
        </div>
        <div class="hero-stats">
          <div class="stat-block">
            <span class="stat-num">{{ reviews.length }}</span>
            <span class="stat-lbl">总评价</span>
          </div>
          <div class="stat-block">
            <span class="stat-num">{{ publishedCount }}</span>
            <span class="stat-lbl">已发布</span>
          </div>
          <div class="stat-block">
            <span class="stat-num">{{ pendingCount }}</span>
            <span class="stat-lbl">待审核</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 筛选 -->
    <div class="filter-bar">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        class="filter-tab"
        :class="{ active: activeFilter === tab.value }"
        @click="activeFilter = tab.value"
      >
        {{ tab.label }}
        <span class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 评价列表 -->
    <section class="my-reviews">
      <el-empty v-if="filteredReviews.length === 0 && !loading" description="还没有评价记录" />

      <div class="review-item" v-for="review in filteredReviews" :key="review.id">
        <!-- 状态横幅 -->
        <div class="item-status" :class="statusClass(review.status)">
          <el-icon><component :is="statusIcon(review.status)" /></el-icon>
          <span>{{ statusText(review.status) }}</span>
          <span class="status-extra" v-if="review.status === 'HIDDEN' && review.hideReason">
            · {{ review.hideReason }}
          </span>
        </div>

        <!-- 评价内容 -->
        <div class="item-body">
          <div class="item-head">
            <span class="item-course">{{ review.courseName || '未知课程' }}</span>
            <span class="item-teacher" v-if="review.teacherName">{{ review.teacherName }}</span>
          </div>
          <div class="item-scores">
            <span class="chip">给分 {{ review.gradingScore }}</span>
            <span class="chip">授课 {{ review.teachingScore }}</span>
            <span class="chip">作业 {{ review.workloadScore }}</span>
          </div>
          <p class="item-content">{{ review.content }}</p>
          <div class="item-time">{{ formatTime(review.createTime) }}</div>
        </div>

        <!-- 操作 -->
        <div class="item-actions">
          <button class="act-btn edit" @click="goEdit(review)">
            <el-icon><EditPen /></el-icon>修改
          </button>
          <button class="act-btn view" @click="goView(review)">
            <el-icon><View /></el-icon>查看
          </button>
          <button class="act-btn del" @click="handleDelete(review)">
            <el-icon><Delete /></el-icon>删除
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, View, Delete, CircleCheck, Clock, Warning, Hide } from '@element-plus/icons-vue'
import { reviewApi } from '@/api/review'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const reviews = ref([])
const loading = ref(false)
const activeFilter = ref('ALL')

const userInfo = computed(() => authStore.userInfo)
const avatarChar = computed(() => {
  const id = userInfo.value?.anonymousId || '?'
  return id.charAt(0).toUpperCase()
})

const publishedCount = computed(() =>
  reviews.value.filter(r => r.status === 'PUBLISHED' || r.status === 'APPROVED').length
)
const pendingCount = computed(() => reviews.value.filter(r => r.status === 'PENDING').length)

const statusTabs = computed(() => [
  { value: 'ALL', label: '全部', count: reviews.value.length },
  { value: 'PUBLISHED', label: '已发布', count: publishedCount.value },
  { value: 'PENDING', label: '待审核', count: pendingCount.value },
  { value: 'HIDDEN', label: '已隐藏', count: reviews.value.filter(r => r.status === 'HIDDEN').length }
])

const filteredReviews = computed(() => {
  if (activeFilter.value === 'ALL') return reviews.value
  if (activeFilter.value === 'PUBLISHED') {
    return reviews.value.filter(r => r.status === 'PUBLISHED' || r.status === 'APPROVED')
  }
  return reviews.value.filter(r => r.status === activeFilter.value)
})

async function loadReviews() {
  loading.value = true
  try {
    const res = await reviewApi.getMyAllReviews()
    reviews.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function statusText(status) {
  return { PUBLISHED: '已发布', APPROVED: '审核通过', PENDING: '待审核', HIDDEN: '已隐藏', REJECTED: '已拒绝' }[status] || status
}
function statusClass(status) {
  return {
    PUBLISHED: 'st-ok', APPROVED: 'st-ok', PENDING: 'st-pending', HIDDEN: 'st-hidden', REJECTED: 'st-reject'
  }[status] || 'st-default'
}
function statusIcon(status) {
  if (status === 'PUBLISHED' || status === 'APPROVED') return CircleCheck
  if (status === 'PENDING') return Clock
  if (status === 'HIDDEN') return Hide
  return Warning
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function goEdit(review) {
  router.push(`/edit-review/${review.id}`)
}

function goView(review) {
  if (review.courseInstanceId) {
    router.push(`/course/instance/${review.courseInstanceId}`)
  }
}

async function handleDelete(review) {
  try {
    await ElMessageBox.confirm(`确认删除对「${review.courseName || '该课程'}」的评价？删除后不可恢复。`, '删除评价', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await reviewApi.delete(review.id)
    ElMessage.success('评价已删除')
    await loadReviews()
  } catch (e) {
    if (e !== 'cancel') {
      // Global interceptor handles errors
    }
  }
}

onMounted(loadReviews)
</script>

<style scoped>
.profile-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 0 0 48px;
}

/* —— Hero —— */
.profile-hero {
  position: relative;
  border-radius: var(--r-lg);
  overflow: hidden;
  margin: 0 16px 24px;
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
  opacity: 0.4;
}
.hero-bg::before {
  width: 260px; height: 260px;
  background: #7dd3fc;
  top: -80px; right: -40px;
}
.hero-bg::after {
  width: 220px; height: 220px;
  background: #60a5fa;
  bottom: -70px; left: -40px;
}
.hero-content {
  position: relative;
  z-index: 2;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  gap: 20px;
  color: #fff;
}
.avatar-lg {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 800;
  flex-shrink: 0;
}
.hero-info {
  flex: 1;
}
.hero-info h1 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
}
.hero-info p {
  font-size: 13px;
  opacity: 0.85;
}
.hero-stats {
  display: flex;
  gap: 24px;
}
.stat-block {
  text-align: center;
}
.stat-num {
  display: block;
  font-size: 26px;
  font-weight: 800;
}
.stat-lbl {
  font-size: 12px;
  opacity: 0.8;
}

/* —— 筛选 —— */
.filter-bar {
  display: flex;
  gap: 8px;
  margin: 0 16px 20px;
  padding: 8px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-xs);
}
.filter-tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 12px;
  border-radius: var(--r-sm);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-2);
  transition: all 0.2s;
  font-family: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.filter-tab:hover {
  background: var(--bg-page);
}
.filter-tab.active {
  background: var(--brand-primary);
  color: #fff;
}
.tab-count {
  font-size: 12px;
  opacity: 0.7;
  background: rgba(0,0,0,0.08);
  padding: 1px 7px;
  border-radius: var(--r-pill);
}
.filter-tab.active .tab-count {
  background: rgba(255,255,255,0.25);
  opacity: 1;
}

/* —— 评价项 —— */
.my-reviews {
  margin: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.review-item {
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--shadow-xs);
  transition: box-shadow 0.25s, transform 0.25s;
}
.review-item:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.item-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  font-size: 12px;
  font-weight: 600;
}
.item-status.st-ok {
  background: #f0fdf4;
  color: var(--c-success);
}
.item-status.st-pending {
  background: #fffbeb;
  color: var(--c-warning);
}
.item-status.st-hidden {
  background: #fef2f2;
  color: var(--c-danger);
}
.item-status.st-reject {
  background: #fef2f2;
  color: var(--c-danger);
}
.status-extra {
  font-weight: 400;
  opacity: 0.8;
}

.item-body {
  padding: 16px 20px;
}
.item-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.item-course {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
}
.item-teacher {
  font-size: 13px;
  color: var(--text-3);
}
.item-scores {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.chip {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: var(--r-pill);
  background: var(--brand-primary-light-9);
  color: var(--brand-primary);
}
.item-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-2);
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-time {
  font-size: 12px;
  color: var(--text-3);
}

.item-actions {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--border-soft);
  background: var(--bg-page);
}
.act-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--border-mid);
  background: #fff;
  color: var(--text-1);
  font-size: 13px;
  padding: 6px 14px;
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.act-btn:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
  box-shadow: var(--shadow-xs);
}
.act-btn.del:hover {
  border-color: var(--c-danger);
  color: var(--c-danger);
}

@media (max-width: 640px) {
  .hero-content {
    flex-direction: column;
    text-align: center;
    gap: 14px;
  }
  .hero-stats {
    justify-content: center;
  }
  .filter-bar {
    overflow-x: auto;
  }
  .item-actions {
    flex-wrap: wrap;
  }
}
</style>
