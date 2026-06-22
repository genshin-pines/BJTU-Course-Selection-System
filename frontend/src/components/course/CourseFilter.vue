<template>
  <div class="course-filter">
    <section class="search-section">
      <el-input
        v-model="filters.keyword"
        size="large"
        placeholder="搜索课程名 / 课程代码 / 教师名..."
        clearable
        @keyup.enter="emitSearch"
        @clear="emitSearch"
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
      <el-button link type="danger" @click="emitReset">清除所有筛选</el-button>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <!-- 统一筛选下拉框 -->
        <el-select
          v-model="filters.filterCategory"
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
        <div v-if="filters.filterCategory === 'department'" class="filter-sub-select">
          <el-select v-model="filters.department" placeholder="选择学院" clearable @change="emitSearch">
            <el-option v-if="departments.length === 0" label="加载中..." value="" disabled />
            <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </div>

        <div v-if="filters.filterCategory === 'teacherName'" class="filter-sub-select">
          <el-select v-model="filters.teacherName" placeholder="选择教师" clearable @change="emitSearch">
            <el-option v-if="teachers.length === 0" label="加载中..." value="" disabled />
            <el-option v-for="teacher in teachers" :key="teacher" :label="teacher" :value="teacher" />
          </el-select>
        </div>

        <div v-if="filters.filterCategory === 'scorePreset'" class="filter-sub-select">
          <el-select v-model="filters.scorePreset" placeholder="综合评分" clearable @change="applyScorePreset">
            <el-option label="4.5 分及以上" value="4.5" />
            <el-option label="4.0 分及以上" value="4.0" />
            <el-option label="3.5 分及以上" value="3.5" />
            <el-option label="3.0 分及以上" value="3.0" />
          </el-select>
        </div>

        <div v-if="filters.filterCategory === 'dimensionFilter'" class="filter-sub-select">
          <el-select v-model="filters.dimensionFilter" placeholder="维度评分" clearable @change="applyDimensionFilter">
            <el-option label="给分 ≥ 4.0" value="grading:4" />
            <el-option label="授课 ≥ 4.0" value="teaching:4" />
            <el-option label="作业轻松 ≥ 4.0" value="workload:4" />
          </el-select>
        </div>

        <div v-if="filters.filterCategory === 'minReviewCount'" class="filter-sub-select">
          <el-select v-model="filters.minReviewCount" placeholder="最低评价数" clearable @change="emitSearch">
            <el-option label="至少 1 条" :value="1" />
            <el-option label="至少 3 条" :value="3" />
            <el-option label="至少 5 条" :value="5" />
            <el-option label="至少 10 条" :value="10" />
          </el-select>
        </div>

        <div v-if="filters.filterCategory === 'tagIds'" class="filter-sub-select">
          <el-select
            v-model="filters.selectedTagIds"
            class="tag-select"
            placeholder="选择标签"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            @change="emitSearch"
          >
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.tagName" :value="tag.id" />
          </el-select>
        </div>

        <!-- 排序下拉框 -->
        <el-select v-model="filters.sortKey" placeholder="排序" clearable @change="emitSearch" class="sort-select">
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

        <el-button @click="emitReset">重置</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  filters: { type: Object, required: true },
  departments: { type: Array, default: () => [] },
  teachers: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] }
})

const emit = defineEmits(['search', 'reset'])

const activeFilters = computed(() => {
  const f = props.filters
  const list = []
  if (f.department) {
    list.push({ name: f.department, key: 'department' })
  }
  if (f.teacherName) {
    list.push({ name: `教师: ${f.teacherName}`, key: 'teacherName' })
  }
  if (f.minScore !== null && f.minScore !== undefined) {
    const label = f.maxScore !== null ? `${f.minScore}-${f.maxScore}分` : `${f.minScore}+分`
    list.push({ name: label, key: 'minScore' })
  }
  if (f.minGradingScore !== null && f.minGradingScore !== undefined) {
    const label = f.maxGradingScore !== null ? `给分${f.minGradingScore}-${f.maxGradingScore}` : `给分≥${f.minGradingScore}`
    list.push({ name: label, key: 'minGradingScore' })
  }
  if (f.minTeachingScore !== null && f.minTeachingScore !== undefined) {
    const label = f.maxTeachingScore !== null ? `授课${f.minTeachingScore}-${f.maxTeachingScore}` : `授课≥${f.minTeachingScore}`
    list.push({ name: label, key: 'minTeachingScore' })
  }
  if (f.minWorkloadScore !== null && f.minWorkloadScore !== undefined) {
    const label = f.maxWorkloadScore !== null ? `作业${f.minWorkloadScore}-${f.maxWorkloadScore}` : `作业≥${f.minWorkloadScore}`
    list.push({ name: label, key: 'minWorkloadScore' })
  }
  if (f.minReviewCount !== null && f.minReviewCount !== undefined) {
    list.push({ name: `≥${f.minReviewCount}条评价`, key: 'minReviewCount' })
  }
  if (f.selectedTagIds?.length > 0) {
    const modeText = f.tagMatchMode === 'AND' ? '全部' : '任一'
    list.push({ name: `标签(${modeText}匹配)`, key: 'tags' })
  }
  return list
})

function emitSearch() {
  emit('search')
}

function emitReset() {
  emit('reset')
}

function onFilterCategoryChange() {
  const f = props.filters
  if (!f.filterCategory) {
    f.department = ''
    f.teacherName = ''
    f.scorePreset = ''
    f.dimensionFilter = ''
    f.minReviewCount = null
    f.selectedTagIds = []
    emit('search')
  }
}

function applyScorePreset() {
  const f = props.filters
  if (f.scorePreset) {
    f.minScore = Number(f.scorePreset)
    f.maxScore = 5.0
  } else {
    f.minScore = null
    f.maxScore = null
  }
  emit('search')
}

function applyDimensionFilter() {
  const f = props.filters
  if (f.dimensionFilter) {
    const [dimension, score] = f.dimensionFilter.split(':')
    const value = Number(score)
    if (dimension === 'grading') {
      f.minGradingScore = value
      f.maxGradingScore = 5.0
    }
    if (dimension === 'teaching') {
      f.minTeachingScore = value
      f.maxTeachingScore = 5.0
    }
    if (dimension === 'workload') {
      f.minWorkloadScore = value
      f.maxWorkloadScore = 5.0
    }
  } else {
    f.minGradingScore = null
    f.maxGradingScore = null
    f.minTeachingScore = null
    f.maxTeachingScore = null
    f.minWorkloadScore = null
    f.maxWorkloadScore = null
  }
  emit('search')
}

function removeFilter(key) {
  const f = props.filters
  switch (key) {
    case 'department':
      f.department = ''
      break
    case 'teacherName':
      f.teacherName = ''
      break
    case 'minScore':
      f.minScore = null
      f.maxScore = null
      f.scorePreset = ''
      break
    case 'minGradingScore':
      f.minGradingScore = null
      f.maxGradingScore = null
      f.dimensionFilter = ''
      break
    case 'minTeachingScore':
      f.minTeachingScore = null
      f.maxTeachingScore = null
      f.dimensionFilter = ''
      break
    case 'minWorkloadScore':
      f.minWorkloadScore = null
      f.maxWorkloadScore = null
      f.dimensionFilter = ''
      break
    case 'minReviewCount':
      f.minReviewCount = null
      break
    case 'tags':
      f.selectedTagIds = []
      f.tagMatchMode = 'OR'
      break
  }
  emit('search')
}
</script>

<style scoped>
.search-section {
  text-align: center;
  padding: 8px 0 24px;
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

@media (max-width: 900px) {
  .filter-panel,
  .active-filters {
    margin-left: 16px;
    margin-right: 16px;
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
}
</style>
