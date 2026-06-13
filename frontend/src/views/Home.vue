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
        @keyup.enter="searchFromFirstPage"
        @clear="searchFromFirstPage"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <el-select v-model="department" placeholder="学院筛选" clearable @change="searchFromFirstPage">
          <el-option label="计算机与信息技术学院" value="计算机与信息技术学院" />
          <el-option label="软件学院" value="软件学院" />
          <el-option label="电子信息工程学院" value="电子信息工程学院" />
        </el-select>

        <el-input
          v-model="teacherName"
          placeholder="教师筛选"
          clearable
          @keyup.enter="searchFromFirstPage"
          @clear="searchFromFirstPage"
        />

        <el-select v-model="semester" placeholder="学期筛选" clearable @change="searchFromFirstPage">
          <el-option label="UNKNOWN" value="UNKNOWN" />
          <el-option label="2025-2026-1" value="2025-2026-1" />
          <el-option label="2025-2026-2" value="2025-2026-2" />
          <el-option label="2024-2025-1" value="2024-2025-1" />
          <el-option label="2024-2025-2" value="2024-2025-2" />
        </el-select>

        <el-select v-model="scorePreset" placeholder="综合评分" clearable @change="applyScorePreset">
          <el-option label="4.5 分及以上" value="4.5" />
          <el-option label="4.0 分及以上" value="4.0" />
          <el-option label="3.5 分及以上" value="3.5" />
          <el-option label="3.0 分及以上" value="3.0" />
        </el-select>
      </div>

      <div class="filter-row">
        <el-select
          v-model="selectedTagIds"
          class="tag-select"
          placeholder="标签筛选（任一匹配）"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          @change="searchFromFirstPage"
        >
          <el-option
            v-for="tag in tags"
            :key="tag.id"
            :label="tag.tagName"
            :value="tag.id"
          />
        </el-select>

        <el-select v-model="dimensionFilter" placeholder="维度评分筛选" clearable @change="applyDimensionFilter">
          <el-option label="给分 ≥ 4.0" value="grading:4" />
          <el-option label="授课 ≥ 4.0" value="teaching:4" />
          <el-option label="作业轻松 ≥ 4.0" value="workload:4" />
        </el-select>

        <el-select v-model="minReviewCount" placeholder="最低评价数" clearable @change="searchFromFirstPage">
          <el-option label="至少 1 条" :value="1" />
          <el-option label="至少 3 条" :value="3" />
          <el-option label="至少 5 条" :value="5" />
          <el-option label="至少 10 条" :value="10" />
        </el-select>

        <el-select v-model="sortKey" placeholder="排序方式" @change="searchFromFirstPage">
          <el-option label="评价数从高到低" value="reviewCount:desc" />
          <el-option label="综合评分从高到低" value="avgScore:desc" />
          <el-option label="给分从高到低" value="gradingScore:desc" />
          <el-option label="授课从高到低" value="teachingScore:desc" />
          <el-option label="作业轻松从高到低" value="workloadScore:desc" />
          <el-option label="课程代码 A-Z" value="courseCode:asc" />
        </el-select>

        <el-button @click="resetFilters">重置筛选</el-button>
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

const keyword = ref('')
const department = ref('')
const teacherName = ref('')
const semester = ref('')
const scorePreset = ref('')
const dimensionFilter = ref('')
const minReviewCount = ref(null)
const selectedTagIds = ref([])
const sortKey = ref('reviewCount:desc')

const tags = ref([])
const courses = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const minScore = ref(null)
const minGradingScore = ref(null)
const minTeachingScore = ref(null)
const minWorkloadScore = ref(null)

function searchFromFirstPage() {
  page.value = 1
  handleSearch()
}

function applyScorePreset() {
  minScore.value = scorePreset.value ? Number(scorePreset.value) : null
  searchFromFirstPage()
}

function applyDimensionFilter() {
  minGradingScore.value = null
  minTeachingScore.value = null
  minWorkloadScore.value = null

  if (dimensionFilter.value) {
    const [dimension, score] = dimensionFilter.value.split(':')
    const value = Number(score)
    if (dimension === 'grading') minGradingScore.value = value
    if (dimension === 'teaching') minTeachingScore.value = value
    if (dimension === 'workload') minWorkloadScore.value = value
  }

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
  sortKey.value = 'reviewCount:desc'
  minScore.value = null
  minGradingScore.value = null
  minTeachingScore.value = null
  minWorkloadScore.value = null
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
    minGradingScore: minGradingScore.value ?? undefined,
    minTeachingScore: minTeachingScore.value ?? undefined,
    minWorkloadScore: minWorkloadScore.value ?? undefined,
    minReviewCount: minReviewCount.value ?? undefined,
    tagIds: selectedTagIds.value.length ? selectedTagIds.value.join(',') : undefined,
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

.filter-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 12px;
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
    grid-template-columns: 1fr;
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
