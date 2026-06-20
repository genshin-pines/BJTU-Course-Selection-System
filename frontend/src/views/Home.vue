<template>
  <div class="home-container">
    <section class="search-section">
      <h1>BJTU 课程评价系统</h1>
      <p class="subtitle">查看课程评价，按评分、标签和学期快速筛选课程</p>
      <el-input
        v-model="keyword"
        size="large"
        placeholder="搜索课程名 / 课程代码 / 教师名..."
        clearable
        @keyup.enter="applyFilters"
        @clear="checkHasChanges"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </section>

    <!-- 已激活的筛选条件 -->
    <section class="active-filters" v-if="activeFilters.length > 0">
      <span class="filters-label">已激活筛选：</span>
      <el-tag
        v-for="filter in activeFilters"
        :key="filter.key"
        closable
        size="large"
        @close="removeFilter(filter.key)"
      >
        {{ filter.name }}
      </el-tag>
      <el-button link type="danger" @click="resetFilters">
        清除所有筛选
      </el-button>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <!-- 统一筛选下拉框 -->
        <el-select
          v-model="filterCategory"
          placeholder="筛选"
          clearable
          @change="onFilterCategoryChange"
          class="filter-main-select"
        >
          <el-option label="学院" value="department" />
          <el-option label="教师" value="teacherName" />
          <el-option label="综合评分" value="scorePreset" />
          <el-option label="维度评分" value="dimensionFilter" />
          <el-option label="最低评价数" value="minReviewCount" />
          <el-option label="标签" value="tagIds" />
        </el-select>

        <!-- 根据选择的筛选类别显示对应的选项 -->
        <div v-if="filterCategory === 'department'" class="filter-sub-select">
          <el-select v-model="department" placeholder="选择学院" clearable @change="searchFromFirstPage">
            <el-option v-if="departments.length === 0" label="加载中..." value="" disabled />
            <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </div>

        <div v-if="filterCategory === 'teacherName'" class="filter-sub-select">
          <el-select v-model="teacherName" placeholder="选择教师" clearable @change="searchFromFirstPage">
            <el-option v-if="teachers.length === 0" label="加载中..." value="" disabled />
            <el-option v-for="teacher in teachers" :key="teacher" :label="teacher" :value="teacher" />
          </el-select>
        </div>

        <div v-if="filterCategory === 'scorePreset'" class="filter-sub-select">
          <el-select v-model="scorePreset" placeholder="综合评分" clearable @change="applyScorePreset">
            <el-option label="4.5 分及以上" value="4.5" />
            <el-option label="4.0 分及以上" value="4.0" />
            <el-option label="3.5 分及以上" value="3.5" />
            <el-option label="3.0 分及以上" value="3.0" />
          </el-select>
        </div>

        <div v-if="filterCategory === 'dimensionFilter'" class="filter-sub-select">
          <el-select v-model="dimensionFilter" placeholder="维度评分" clearable @change="applyDimensionFilter">
            <el-option label="给分 ≥ 4.0" value="grading:4" />
            <el-option label="授课 ≥ 4.0" value="teaching:4" />
            <el-option label="作业轻松 ≥ 4.0" value="workload:4" />
          </el-select>
        </div>

        <div v-if="filterCategory === 'minReviewCount'" class="filter-sub-select">
          <el-select v-model="minReviewCount" placeholder="最低评价数" clearable @change="searchFromFirstPage">
            <el-option label="至少 1 条" :value="1" />
            <el-option label="至少 3 条" :value="3" />
            <el-option label="至少 5 条" :value="5" />
            <el-option label="至少 10 条" :value="10" />
          </el-select>
        </div>

        <div v-if="filterCategory === 'tagIds'" class="filter-sub-select">
          <el-select
            v-model="selectedTagIds"
            class="tag-select"
            placeholder="选择标签"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            @change="searchFromFirstPage"
          >
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.tagName" :value="tag.id" />
          </el-select>
        </div>

        <!-- 排序下拉框 -->
        <el-select v-model="sortKey" placeholder="排序" clearable @change="searchFromFirstPage" class="sort-select">
          <el-option label="评价数从高到低" value="reviewCount:desc" />
          <el-option label="评价数从低到高" value="reviewCount:asc" />
          <el-option label="综合评分从高到低" value="avgScore:desc" />
          <el-option label="综合评分从低到高" value="avgScore:asc" />
          <el-option label="给分从高到低" value="gradingScore:desc" />
          <el-option label="给分从低到高" value="gradingScore:asc" />
          <el-option label="授课从高到低" value="teachingScore:desc" />
          <el-option label="授课从低到高" value="teachingScore:asc" />
          <el-option label="作业轻松从高到低" value="workloadScore:desc" />
          <el-option label="作业轻松从低到高" value="workloadScore:asc" />
          <el-option label="课程代码 A-Z" value="courseCode:asc" />
          <el-option label="课程代码 Z-A" value="courseCode:desc" />
        </el-select>

        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="course-list" v-loading="loading">
      <el-empty v-if="!loading && courses.length === 0" description="暂无课程数据" />

      <el-card
        v-for="course in courses"
        :key="course.courseInstanceId || course.id"
        class="course-card"
        shadow="hover"
        @click="goCourseDetail(course)"
      >
        <div class="course-main">
          <div class="course-info">
            <h3 class="course-name">
              {{ course.courseName }}
              <el-tag size="small">{{ course.courseCode }}</el-tag>
            </h3>
            <p class="course-meta">
              <span>{{ course.teacherName || '未分配教师' }}</span>
              <el-divider direction="vertical" />
              <span>{{ course.department }}</span>
              <el-divider direction="vertical" />
              <span>{{ course.credit }} 学分</span>
              <template v-if="course.semester">
                <el-divider direction="vertical" />
                <span>{{ course.semester }}</span>
              </template>
              <template v-if="course.className">
                <el-divider direction="vertical" />
                <span>{{ course.className }}</span>
              </template>
            </p>
          </div>

          <div class="course-scores">
            <div class="score-item highlight">
              <span class="score-label">综合</span>
              <span class="score-value">{{ formatScore(course.avgScore) }}</span>
            </div>
            <div class="score-item">
              <span class="score-label">给分</span>
              <span class="score-value">{{ formatScore(course.gradingScore) }}</span>
            </div>
            <div class="score-item">
              <span class="score-label">授课</span>
              <span class="score-value">{{ formatScore(course.avgTeachingScore) }}</span>
            </div>
            <div class="score-item">
              <span class="score-label">作业</span>
              <span class="score-value">{{ formatScore(course.avgWorkloadScore) }}</span>
            </div>
          </div>
        </div>

        <div class="course-footer">
          <span class="review-count">{{ course.reviewCount || 0 }} 条评价</span>
          <div v-if="course.topTags?.length" class="top-tags">
            <el-tag v-for="tag in course.topTags" :key="tag" size="small" type="info">
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </el-card>

      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { courseApi } from '@/api/course'
import { tagApi } from '@/api/tag'

const router = useRouter()

// API基础URL
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const keyword = ref('')
const filterCategory = ref('') // 当前选择的筛选类别
const department = ref('')
const teacherName = ref('')
const semester = ref('')
const scorePreset = ref('')
const dimensionFilter = ref('')
const minReviewCount = ref(null)
const selectedTagIds = ref([])
const sortKey = ref('reviewCount:desc')
const tagMatchMode = ref('OR') // 'OR' 或 'AND'

const tags = ref([])
const departments = ref([])
const teachers = ref([])
const semesters = ref([])
const courses = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const minScore = ref(null)
const maxScore = ref(null)
const minGradingScore = ref(null)
const maxGradingScore = ref(null)
const minTeachingScore = ref(null)
const maxTeachingScore = ref(null)
const minWorkloadScore = ref(null)
const maxWorkloadScore = ref(null)

// 已激活的筛选条件
const activeFilters = ref([])

function searchFromFirstPage() {
  page.value = 1
  handleSearch()
}

function onFilterCategoryChange() {
  // 当切换筛选类别时，清空之前的筛选条件
  if (!filterCategory.value) {
    // 清空所有筛选
    department.value = ''
    teacherName.value = ''
    scorePreset.value = ''
    dimensionFilter.value = ''
    minReviewCount.value = null
    selectedTagIds.value = []
    updateActiveFilters()
    searchFromFirstPage()
  }
}

function applyFilters() {
  searchFromFirstPage()
  showApplyButton.value = false
}

function applyScorePreset() {
  if (scorePreset.value) {
    minScore.value = Number(scorePreset.value)
    maxScore.value = 5.0
  } else {
    minScore.value = null
    maxScore.value = null
  }
  updateActiveFilters()
  searchFromFirstPage()
}

function applyDimensionFilter() {
  if (dimensionFilter.value) {
    const [dimension, score] = dimensionFilter.value.split(':')
    const value = Number(score)
    if (dimension === 'grading') {
      minGradingScore.value = value
      maxGradingScore.value = 5.0
    }
    if (dimension === 'teaching') {
      minTeachingScore.value = value
      maxTeachingScore.value = 5.0
    }
    if (dimension === 'workload') {
      minWorkloadScore.value = value
      maxWorkloadScore.value = 5.0
    }
  } else {
    minGradingScore.value = null
    maxGradingScore.value = null
    minTeachingScore.value = null
    maxTeachingScore.value = null
    minWorkloadScore.value = null
    maxWorkloadScore.value = null
  }
  updateActiveFilters()
  searchFromFirstPage()
}

function resetFilters() {
  keyword.value = ''
  department.value = ''
  teacherName.value = ''
  semester.value = ''
  scorePreset.value = ''
  dimensionFilter.value = ''
  minReviewCount.value = null
  selectedTagIds.value = []
  tagMatchMode.value = 'OR'
  sortKey.value = 'reviewCount:desc'
  minScore.value = null
  maxScore.value = null
  minGradingScore.value = null
  maxGradingScore.value = null
  minTeachingScore.value = null
  maxTeachingScore.value = null
  minWorkloadScore.value = null
  maxWorkloadScore.value = null
  activeFilters.value = []
  searchFromFirstPage()
}

function buildSearchParams() {
  const [sortBy, sortOrder] = sortKey.value.split(':')
  return {
    keyword: keyword.value || undefined,
    department: department.value || undefined,
    teacherName: teacherName.value || undefined,
    semester: semester.value || undefined,
    minScore: minScore.value ?? undefined,
    maxScore: maxScore.value ?? undefined,
    minGradingScore: minGradingScore.value ?? undefined,
    maxGradingScore: maxGradingScore.value ?? undefined,
    minTeachingScore: minTeachingScore.value ?? undefined,
    maxTeachingScore: maxTeachingScore.value ?? undefined,
    minWorkloadScore: minWorkloadScore.value ?? undefined,
    maxWorkloadScore: maxWorkloadScore.value ?? undefined,
    minReviewCount: minReviewCount.value ?? undefined,
    tagIds: selectedTagIds.value.length ? selectedTagIds.value.join(',') : undefined,
    tagMatchMode: tagMatchMode.value || undefined,
    sortBy,
    sortOrder,
    page: page.value,
    size: pageSize.value
  }
}

async function handleSearch() {
  loading.value = true
  try {
    const res = await courseApi.search(buildSearchParams())
    courses.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    const res = await tagApi.getAll()
    tags.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function loadFilterOptions() {
  try {
    const res = await fetch(`${API_BASE}/course/filters/options`)
    const data = await res.json()
    if (data.code === 200) {
      departments.value = data.data.departments || []
      teachers.value = data.data.teachers || []
      semesters.value = data.data.semesters || []
    }
  } catch (e) {
    console.error('Failed to load filter options:', e)
  }
}

function updateActiveFilters() {
  activeFilters.value = []
  if (department.value) {
    activeFilters.value.push({ name: department.value, key: 'department', value: department.value })
  }
  if (teacherName.value) {
    activeFilters.value.push({ name: `教师: ${teacherName.value}`, key: 'teacherName', value: teacherName.value })
  }
  if (semester.value) {
    activeFilters.value.push({ name: `学期: ${semester.value}`, key: 'semester', value: semester.value })
  }
  if (minScore.value !== null) {
    const label = maxScore.value !== null ? `${minScore.value}-${maxScore.value}分` : `${minScore.value}+分`
    activeFilters.value.push({ name: label, key: 'minScore', value: minScore.value })
  }
  if (minGradingScore.value !== null) {
    const label = maxGradingScore.value !== null ? `给分${minGradingScore.value}-${maxGradingScore.value}` : `给分≥${minGradingScore.value}`
    activeFilters.value.push({ name: label, key: 'minGradingScore', value: minGradingScore.value })
  }
  if (minTeachingScore.value !== null) {
    const label = maxTeachingScore.value !== null ? `授课${minTeachingScore.value}-${maxTeachingScore.value}` : `授课≥${minTeachingScore.value}`
    activeFilters.value.push({ name: label, key: 'minTeachingScore', value: minTeachingScore.value })
  }
  if (minWorkloadScore.value !== null) {
    const label = maxWorkloadScore.value !== null ? `作业${minWorkloadScore.value}-${maxWorkloadScore.value}` : `作业≥${minWorkloadScore.value}`
    activeFilters.value.push({ name: label, key: 'minWorkloadScore', value: minWorkloadScore.value })
  }
  if (minReviewCount.value !== null) {
    activeFilters.value.push({ name: `≥${minReviewCount.value}条评价`, key: 'minReviewCount', value: minReviewCount.value })
  }
  if (selectedTagIds.value.length > 0) {
    const modeText = tagMatchMode.value === 'AND' ? '全部' : '任一'
    activeFilters.value.push({ name: `标签(${modeText}匹配)`, key: 'tags', value: selectedTagIds.value })
  }
}

function removeFilter(key) {
  switch (key) {
    case 'department':
      department.value = ''
      break
    case 'teacherName':
      teacherName.value = ''
      break
    case 'semester':
      semester.value = ''
      break
    case 'minScore':
      minScore.value = null
      maxScore.value = null
      break
    case 'minGradingScore':
      minGradingScore.value = null
      maxGradingScore.value = null
      break
    case 'minTeachingScore':
      minTeachingScore.value = null
      maxTeachingScore.value = null
      break
    case 'minWorkloadScore':
      minWorkloadScore.value = null
      maxWorkloadScore.value = null
      break
    case 'minReviewCount':
      minReviewCount.value = null
      break
    case 'tags':
      selectedTagIds.value = []
      tagMatchMode.value = 'OR'
      break
  }
  updateActiveFilters()
  searchFromFirstPage()
}

function goCourseDetail(course) {
  router.push({
    path: `/course/${course.id || course.courseBaseId}`,
    query: course.courseInstanceId ? { instanceId: course.courseInstanceId } : {}
  })
}

function formatScore(score) {
  return score === null || score === undefined ? '-' : Number(score).toFixed(1)
}

onMounted(() => {
  loadTags()
  loadFilterOptions()
  handleSearch()
})
</script>

<style scoped>
.home-container {
  min-height: 100%;
}

.search-section {
  text-align: center;
  padding: 48px 0 24px;
}

.search-section h1 {
  font-size: 32px;
  color: #1a1a2e;
  margin-bottom: 12px;
}

.subtitle {
  color: #666;
  font-size: 16px;
  margin-bottom: 32px;
}

.search-section .el-input {
  max-width: 640px;
}

.filter-panel {
  margin: 0 40px 24px;
  padding: 18px;
  border: 1px solid #edf0f5;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
}

.active-filters {
  margin: 0 40px 16px;
  padding: 12px 18px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 10px;
  border: 1px solid #bae6fd;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filters-label {
  font-size: 14px;
  font-weight: 600;
  color: #0369a1;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-main-select {
  width: 140px;
}

.filter-sub-select {
  flex: 1;
  min-width: 200px;
}

.sort-select {
  width: 180px;
}

.tag-select {
  width: 100%;
}

.filter-row + .filter-row {
  margin-top: 12px;
}

.tag-select {
  grid-column: span 1;
}

.course-list {
  padding: 0 40px 40px;
}

.course-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.course-card:hover {
  transform: translateY(-2px);
}

.course-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.course-info {
  flex: 1;
  min-width: 0;
}

.course-name {
  font-size: 18px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-meta {
  color: #888;
  font-size: 14px;
  line-height: 1.8;
}

.course-scores {
  display: flex;
  gap: 22px;
}

.score-item {
  text-align: center;
}

.score-item.highlight .score-value {
  color: #f59e0b;
}

.score-label {
  display: block;
  font-size: 12px;
  color: #999;
}

.score-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #1a73e8;
}

.course-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.review-count {
  color: #999;
  font-size: 13px;
}

.top-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .filter-panel,
  .course-list {
    margin-left: 16px;
    margin-right: 16px;
    padding-left: 0;
    padding-right: 0;
  }

  .filter-panel {
    padding: 14px;
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-main-select,
  .filter-sub-select,
  .sort-select {
    width: 100%;
  }

  .course-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .course-scores {
    width: 100%;
    justify-content: space-between;
  }

  .course-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
