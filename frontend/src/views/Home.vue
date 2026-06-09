<template>
  <div class="home-container">
    <!-- 搜索栏 -->
    <div class="search-section">
      <h1>BJTU 课程评价系统</h1>
      <p class="subtitle">查看课程评价，找到最适合你的课程</p>
      <el-input
        v-model="keyword"
        size="large"
        placeholder="搜索课程名 / 课程代码 / 教师名..."
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-bar">
      <el-select v-model="department" placeholder="学院筛选" clearable @change="handleSearch" style="width: 200px">
        <el-option label="计算机与信息技术学院" value="计算机与信息技术学院" />
        <el-option label="软件学院" value="软件学院" />
        <el-option label="电子信息工程学院" value="电子信息工程学院" />
      </el-select>
    </div>

    <!-- 课程列表 -->
    <div class="course-list" v-loading="loading">
      <el-empty v-if="!loading && courses.length === 0" description="暂无课程数据" />

      <el-card
        v-for="course in courses"
        :key="course.id"
        class="course-card"
        shadow="hover"
        @click="$router.push(`/course/${course.id}`)"
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
            </p>
          </div>
          <div class="course-scores">
            <div class="score-item">
              <span class="score-label">给分</span>
              <span class="score-value">{{ course.gradingScore?.toFixed(1) || '-' }}</span>
            </div>
            <div class="score-item">
              <span class="score-label">授课</span>
              <span class="score-value">{{ course.avgTeachingScore?.toFixed(1) || '-' }}</span>
            </div>
            <div class="score-item">
              <span class="score-label">作业</span>
              <span class="score-value">{{ course.avgWorkloadScore?.toFixed(1) || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="course-footer">
          <span class="review-count">{{ course.reviewCount || 0 }} 条评价</span>
        </div>
      </el-card>

      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { courseApi } from '@/api/course'
import { Search } from '@element-plus/icons-vue'

const keyword = ref('')
const department = ref('')
const courses = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function handleSearch() {
  loading.value = true
  try {
    const res = await courseApi.search({
      keyword: keyword.value,
      department: department.value,
      page: page.value,
      size: pageSize.value
    })
    courses.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
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
  max-width: 600px;
}

.filter-bar {
  margin-bottom: 24px;
  padding: 0 40px;
}

.course-list {
  padding: 0 40px 40px;
}

.course-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.course-card:hover {
  transform: translateY(-2px);
}

.course-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course-info {
  flex: 1;
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
}

.course-scores {
  display: flex;
  gap: 24px;
}

.score-item {
  text-align: center;
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
}

.review-count {
  color: #999;
  font-size: 13px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
