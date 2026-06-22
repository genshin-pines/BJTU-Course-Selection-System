<template>
  <div class="home-page">
    <!-- Hero 渐变区 -->
    <section class="hero">
      <div class="hero-bg"></div>
      <div class="hero-inner">
        <div class="hero-badge">北京交通大学 · 课程评价平台</div>
        <h1 class="hero-title">选课前，<span class="hl">先听听过来人怎么说</span></h1>
        <p class="hero-sub">真实匿名评价 · 多维评分体系 · 标签智能筛选，帮你找到值得上的好课</p>
      </div>
      <div class="hero-wave"></div>
    </section>

    <div class="home-body">
      <CourseFilter
        :filters="filters"
        :departments="departments"
        :teachers="teachers"
        :tags="tags"
        @search="searchFromFirstPage"
        @reset="resetFilters"
      />

      <!-- 结果统计条 -->
      <div class="result-bar">
        <div class="result-count">
          <span class="count-num">{{ total }}</span>
          <span class="count-lbl">门课程</span>
        </div>
        <div class="result-hint" v-if="hasActiveFilter">已应用筛选条件</div>
        <button class="apply-btn" @click="$router.push('/course-application')">
          <el-icon><Plus /></el-icon>添加新课程
        </button>
      </div>

      <section class="course-grid" v-loading="loading">
        <el-empty v-if="!loading && courses.length === 0" description="暂无课程数据，试试调整筛选条件" />
        <CourseCard
          v-for="course in courses"
          :key="course.courseInstanceId || course.id"
          :course="course"
          @click="goCourseDetail(course)"
        />
      </section>

      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handleSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { courseApi } from '@/api/course'
import { tagApi } from '@/api/tag'
import CourseFilter from '@/components/course/CourseFilter.vue'
import CourseCard from '@/components/course/CourseCard.vue'

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const tags = ref([])
const departments = ref([])
const teachers = ref([])
const courses = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = reactive({
  keyword: '', filterCategory: '', department: '', teacherName: '',
  scorePreset: '', dimensionFilter: '', minReviewCount: null,
  selectedTagIds: [], sortKey: 'reviewCount:desc', tagMatchMode: 'OR',
  minScore: null, maxScore: null, minGradingScore: null, maxGradingScore: null,
  minTeachingScore: null, maxTeachingScore: null, minWorkloadScore: null, maxWorkloadScore: null
})

const hasActiveFilter = computed(() =>
  filters.keyword || filters.department || filters.teacherName ||
  filters.minScore !== null || filters.minGradingScore !== null ||
  filters.minTeachingScore !== null || filters.minWorkloadScore !== null ||
  filters.minReviewCount !== null || filters.selectedTagIds.length > 0
)

function searchFromFirstPage() { page.value = 1; handleSearch() }
function resetFilters() {
  Object.assign(filters, {
    keyword: '', filterCategory: '', department: '', teacherName: '',
    scorePreset: '', dimensionFilter: '', minReviewCount: null,
    selectedTagIds: [], sortKey: 'reviewCount:desc', tagMatchMode: 'OR',
    minScore: null, maxScore: null, minGradingScore: null, maxGradingScore: null,
    minTeachingScore: null, maxTeachingScore: null, minWorkloadScore: null, maxWorkloadScore: null
  })
  searchFromFirstPage()
}

function buildSearchParams() {
  const [sortBy, sortOrder] = filters.sortKey.split(':')
  return {
    keyword: filters.keyword || undefined,
    department: filters.department || undefined,
    teacherName: filters.teacherName || undefined,
    minScore: filters.minScore ?? undefined, maxScore: filters.maxScore ?? undefined,
    minGradingScore: filters.minGradingScore ?? undefined, maxGradingScore: filters.maxGradingScore ?? undefined,
    minTeachingScore: filters.minTeachingScore ?? undefined, maxTeachingScore: filters.maxTeachingScore ?? undefined,
    minWorkloadScore: filters.minWorkloadScore ?? undefined, maxWorkloadScore: filters.maxWorkloadScore ?? undefined,
    minReviewCount: filters.minReviewCount ?? undefined,
    tagIds: filters.selectedTagIds.length ? filters.selectedTagIds.join(',') : undefined,
    tagMatchMode: filters.tagMatchMode || undefined, sortBy, sortOrder,
    page: page.value, size: pageSize.value
  }
}

async function handleSearch() {
  loading.value = true
  try {
    const res = await courseApi.search(buildSearchParams())
    courses.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function loadTags() {
  try { const res = await tagApi.getAll(); tags.value = res.data || [] } catch (e) { console.error(e) }
}
async function loadFilterOptions() {
  try {
    const res = await fetch(`${API_BASE}/course/filters/options`)
    const data = await res.json()
    if (data.code === 200) {
      departments.value = data.data.departments || []
      teachers.value = data.data.teachers || []
    }
  } catch (e) { console.error('Failed to load filter options:', e) }
}

function goCourseDetail(course) {
  router.push(`/course/instance/${course.courseInstanceId || course.id}`)
}

onMounted(() => { loadTags(); loadFilterOptions(); handleSearch() })
</script>

<style scoped>
.home-page {
  min-height: 100%;
}

/* —— Hero —— */
.hero {
  position: relative;
  padding: 64px 24px 80px;
  overflow: hidden;
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
  filter: blur(80px);
  opacity: 0.5;
}
.hero-bg::before {
  width: 460px; height: 460px;
  background: #7dd3fc;
  top: -140px; right: 10%;
}
.hero-bg::after {
  width: 380px; height: 380px;
  background: #60a5fa;
  bottom: -120px; left: -60px;
}
.hero-inner {
  position: relative;
  z-index: 2;
  max-width: 820px;
  margin: 0 auto;
  text-align: center;
  color: #fff;
}
.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: var(--r-pill);
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.3);
  backdrop-filter: blur(8px);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 20px;
}
.hero-title {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.3;
  margin-bottom: 14px;
  letter-spacing: 1px;
}
.hero-title .hl {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hero-sub {
  font-size: 16px;
  opacity: 0.9;
  line-height: 1.7;
}
.hero-wave {
  position: absolute;
  left: 0; right: 0; bottom: -1px;
  height: 40px;
  background: var(--bg-page);
  border-radius: 50% 50% 0 0 / 100% 100% 0 0;
}

/* —— 主体 —— */
.home-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

.result-bar {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 24px 4px 16px;
}
.count-num {
  font-size: 26px;
  font-weight: 800;
  color: var(--brand-primary);
}
.count-lbl {
  font-size: 14px;
  color: var(--text-2);
  margin-left: 4px;
}
.result-hint {
  font-size: 12px;
  color: var(--text-3);
  padding: 3px 10px;
  background: var(--brand-primary-light-9);
  border-radius: var(--r-pill);
  color: var(--brand-primary);
}
.apply-btn {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--brand-primary);
  background: #fff;
  color: var(--brand-primary);
  font-size: 14px;
  font-weight: 600;
  padding: 8px 18px;
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.apply-btn:hover {
  background: var(--brand-primary);
  color: #fff;
  box-shadow: var(--shadow-glow);
}

.course-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .hero { padding: 44px 20px 64px; }
  .hero-title { font-size: 28px; }
  .hero-sub { font-size: 14px; }
  .home-body { padding: 0 16px 40px; }
}
</style>
