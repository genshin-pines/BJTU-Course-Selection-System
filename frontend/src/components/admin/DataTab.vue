<template>
  <div>
    <el-tabs v-model="dataTab">
      <el-tab-pane label="课程" name="courses">
        <div class="section-toolbar search-bar">
          <el-input v-model="courseFilter.courseCode" placeholder="课程代码" style="width: 140px" clearable
            @keyup.enter="searchCourses" />
          <el-input v-model="courseFilter.courseName" placeholder="课程名称" style="width: 180px" clearable
            @keyup.enter="searchCourses" />
          <el-input v-model="courseFilter.department" placeholder="学院" style="width: 160px" clearable
            @keyup.enter="searchCourses" />
          <el-button type="primary" @click="searchCourses">搜索</el-button>
          <el-button @click="resetCourseFilter">重置</el-button>
        </div>
        <div class="section-toolbar">
          <el-input v-model="courseForm.courseCode" placeholder="课程代码" style="width: 140px" />
          <el-input v-model="courseForm.courseName" placeholder="课程名称" style="width: 220px" />
          <el-input-number v-model="courseForm.credit" :min="0" :max="20" controls-position="right" />
          <el-input v-model="courseForm.department" placeholder="开课学院" style="width: 180px" />
          <el-button type="primary" @click="handleSaveCourse">
            {{ courseForm.id ? '保存课程' : '新增课程' }}
          </el-button>
          <el-button v-if="courseForm.id" @click="resetCourseForm">取消编辑</el-button>
        </div>
        <el-table :data="adminCourses" v-loading="courseLoading" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="courseCode" label="课程代码" width="140" />
          <el-table-column prop="courseName" label="课程名称" min-width="220" />
          <el-table-column prop="credit" label="学分" width="90" />
          <el-table-column prop="department" label="学院" width="180" />
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editCourse(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDeleteCourse(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="coursePage.current"
            v-model:page-size="coursePage.size"
            :page-sizes="pageSizeOptions"
            :total="coursePage.total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleCourseSizeChange"
            @current-change="handleCoursePageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="教师" name="teachers">
        <div class="section-toolbar search-bar">
          <el-input v-model="teacherFilter.teacherName" placeholder="教师姓名" style="width: 180px" clearable
            @keyup.enter="searchTeachers" />
          <el-input v-model="teacherFilter.department" placeholder="学院" style="width: 180px" clearable
            @keyup.enter="searchTeachers" />
          <el-button type="primary" @click="searchTeachers">搜索</el-button>
          <el-button @click="resetTeacherFilter">重置</el-button>
        </div>
        <div class="section-toolbar">
          <el-input v-model="teacherForm.teacherName" placeholder="教师姓名" style="width: 220px" />
          <el-input v-model="teacherForm.department" placeholder="所属学院" style="width: 220px" />
          <el-button type="primary" @click="handleSaveTeacher">
            {{ teacherForm.id ? '保存教师' : '新增教师' }}
          </el-button>
          <el-button v-if="teacherForm.id" @click="resetTeacherForm">取消编辑</el-button>
        </div>
        <el-table :data="adminTeachers" v-loading="teacherLoading" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="teacherName" label="教师姓名" min-width="180" />
          <el-table-column prop="department" label="学院" width="180" />
          <el-table-column prop="avgScore" label="综合评分" width="110">
            <template #default="{ row }">{{ formatScore(row.avgScore) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editTeacher(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDeleteTeacher(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="teacherPage.current"
            v-model:page-size="teacherPage.size"
            :page-sizes="pageSizeOptions"
            :total="teacherPage.total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleTeacherSizeChange"
            @current-change="handleTeacherPageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="开课实例" name="instances">
        <div class="section-toolbar search-bar">
          <el-input v-model="instanceFilter.courseName" placeholder="课程名称" style="width: 180px" clearable
            @keyup.enter="searchInstances" />
          <el-input v-model="instanceFilter.teacherName" placeholder="教师姓名" style="width: 160px" clearable
            @keyup.enter="searchInstances" />
          <el-button type="primary" @click="searchInstances">搜索</el-button>
          <el-button @click="resetInstanceFilter">重置</el-button>
        </div>
        <div class="section-toolbar">
          <el-select v-model="instanceForm.courseBaseId" filterable placeholder="选择课程" style="width: 260px">
            <el-option
              v-for="course in adminCourseOptions"
              :key="course.id"
              :label="`${course.courseCode} ${course.courseName}`"
              :value="course.id"
            />
          </el-select>
          <el-select v-model="instanceForm.teacherId" filterable placeholder="选择教师" style="width: 220px">
            <el-option
              v-for="teacher in adminTeacherOptions"
              :key="teacher.id"
              :label="teacher.teacherName"
              :value="teacher.id"
            />
          </el-select>
          <el-button type="primary" @click="handleSaveInstance">
            {{ instanceForm.id ? '保存实例' : '新增实例' }}
          </el-button>
          <el-button v-if="instanceForm.id" @click="resetInstanceForm">取消编辑</el-button>
        </div>
        <el-table :data="adminInstances" v-loading="instanceLoading" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="课程" min-width="220">
            <template #default="{ row }">{{ formatInstanceCourse(row) }}</template>
          </el-table-column>
          <el-table-column label="教师" width="140">
            <template #default="{ row }">{{ row.teacherName || teacherNameById(row.teacherId) }}</template>
          </el-table-column>
          <el-table-column prop="reviewCount" label="评价数" width="90" />
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editInstance(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDeleteInstance(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="instancePage.current"
            v-model:page-size="instancePage.size"
            :page-sizes="pageSizeOptions"
            :total="instancePage.total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleInstanceSizeChange"
            @current-change="handleInstancePageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const pageSizeOptions = [10, 20, 50, 100]
const dataTab = ref('courses')

const adminCourses = ref([])
const adminCourseOptions = ref([])
const courseLoading = ref(false)
const courseForm = ref(blankCourseForm())
const coursePage = ref(blankPageState())
const courseFilter = ref({ courseCode: '', courseName: '', department: '' })

const adminTeachers = ref([])
const adminTeacherOptions = ref([])
const teacherLoading = ref(false)
const teacherForm = ref(blankTeacherForm())
const teacherPage = ref(blankPageState())
const teacherFilter = ref({ teacherName: '', department: '' })

const adminInstances = ref([])
const instanceLoading = ref(false)
const instanceForm = ref(blankInstanceForm())
const instancePage = ref(blankPageState())
const instanceFilter = ref({ courseName: '', teacherName: '' })

function blankPageState() {
  return { current: 1, size: 10, total: 0 }
}

function blankCourseForm() {
  return { id: null, courseCode: '', courseName: '', credit: 0, department: '' }
}

function blankTeacherForm() {
  return { id: null, teacherName: '', department: '' }
}

function blankInstanceForm() {
  return { id: null, courseBaseId: null, teacherId: null }
}

function applyPageResult(data, recordsRef, pageRef) {
  recordsRef.value = data?.records || []
  pageRef.value.total = Number(data?.total || 0)
  pageRef.value.current = Number(data?.page || pageRef.value.current || 1)
  pageRef.value.size = Number(data?.size || pageRef.value.size || 10)
}

// ===== 课程 =====
async function loadAdminCourses() {
  courseLoading.value = true
  try {
    const params = {
      page: coursePage.value.current,
      pageSize: coursePage.value.size
    }
    if (courseFilter.value.courseCode) params.courseCode = courseFilter.value.courseCode
    if (courseFilter.value.courseName) params.courseName = courseFilter.value.courseName
    if (courseFilter.value.department) params.department = courseFilter.value.department
    const res = await adminApi.getAdminCourses(params)
    applyPageResult(res.data, adminCourses, coursePage)
    if (adminCourses.value.length === 0 && coursePage.value.current > 1 && coursePage.value.total > 0) {
      coursePage.value.current -= 1
      await loadAdminCourses()
    }
  } finally {
    courseLoading.value = false
  }
}

async function loadAdminCourseOptions() {
  const res = await adminApi.getAdminCourses({ page: 1, pageSize: 100 })
  adminCourseOptions.value = res.data?.records || []
}

function searchCourses() {
  coursePage.value.current = 1
  loadAdminCourses()
}

function resetCourseFilter() {
  courseFilter.value = { courseCode: '', courseName: '', department: '' }
  coursePage.value.current = 1
  loadAdminCourses()
}

function handleCoursePageChange(page) {
  coursePage.value.current = page
  loadAdminCourses()
}

function handleCourseSizeChange(size) {
  coursePage.value.size = size
  coursePage.value.current = 1
  loadAdminCourses()
}

async function handleSaveCourse() {
  const payload = {
    courseCode: courseForm.value.courseCode.trim(),
    courseName: courseForm.value.courseName.trim(),
    credit: courseForm.value.credit || 0,
    department: courseForm.value.department.trim()
  }
  if (!payload.courseCode || !payload.courseName) {
    ElMessage.warning('请填写课程代码和课程名称')
    return
  }
  if (courseForm.value.id) {
    await adminApi.updateAdminCourse(courseForm.value.id, payload)
    ElMessage.success('课程已更新')
  } else {
    await adminApi.createAdminCourse(payload)
    ElMessage.success('课程已创建')
  }
  resetCourseForm()
  await Promise.all([loadAdminCourses(), loadAdminCourseOptions()])
}

function editCourse(row) {
  courseForm.value = {
    id: row.id,
    courseCode: row.courseCode || '',
    courseName: row.courseName || '',
    credit: row.credit || 0,
    department: row.department || ''
  }
}

async function handleDeleteCourse(row) {
  try {
    await ElMessageBox.confirm(`确认删除课程 ${row.courseName}？`, '删除课程', { type: 'warning' })
    await adminApi.deleteAdminCourse(row.id)
    ElMessage.success('课程已删除')
    await Promise.all([loadAdminCourses(), loadAdminCourseOptions(), loadAdminInstances()])
  } catch (e) {}
}

function resetCourseForm() {
  courseForm.value = blankCourseForm()
}

// ===== 教师 =====
async function loadAdminTeachers() {
  teacherLoading.value = true
  try {
    const params = {
      page: teacherPage.value.current,
      pageSize: teacherPage.value.size
    }
    if (teacherFilter.value.teacherName) params.teacherName = teacherFilter.value.teacherName
    if (teacherFilter.value.department) params.department = teacherFilter.value.department
    const res = await adminApi.getAdminTeachers(params)
    applyPageResult(res.data, adminTeachers, teacherPage)
    if (adminTeachers.value.length === 0 && teacherPage.value.current > 1 && teacherPage.value.total > 0) {
      teacherPage.value.current -= 1
      await loadAdminTeachers()
    }
  } finally {
    teacherLoading.value = false
  }
}

async function loadAdminTeacherOptions() {
  const res = await adminApi.getAdminTeachers({ page: 1, pageSize: 100 })
  adminTeacherOptions.value = res.data?.records || []
}

function searchTeachers() {
  teacherPage.value.current = 1
  loadAdminTeachers()
}

function resetTeacherFilter() {
  teacherFilter.value = { teacherName: '', department: '' }
  teacherPage.value.current = 1
  loadAdminTeachers()
}

function handleTeacherPageChange(page) {
  teacherPage.value.current = page
  loadAdminTeachers()
}

function handleTeacherSizeChange(size) {
  teacherPage.value.size = size
  teacherPage.value.current = 1
  loadAdminTeachers()
}

async function handleSaveTeacher() {
  const payload = {
    teacherName: teacherForm.value.teacherName.trim(),
    department: teacherForm.value.department.trim()
  }
  if (!payload.teacherName) {
    ElMessage.warning('请填写教师姓名')
    return
  }
  if (teacherForm.value.id) {
    await adminApi.updateAdminTeacher(teacherForm.value.id, payload)
    ElMessage.success('教师已更新')
  } else {
    await adminApi.createAdminTeacher(payload)
    ElMessage.success('教师已创建')
  }
  resetTeacherForm()
  await Promise.all([loadAdminTeachers(), loadAdminTeacherOptions()])
}

function editTeacher(row) {
  teacherForm.value = {
    id: row.id,
    teacherName: row.teacherName || '',
    department: row.department || ''
  }
}

async function handleDeleteTeacher(row) {
  try {
    await ElMessageBox.confirm(`确认删除教师 ${row.teacherName}？`, '删除教师', { type: 'warning' })
    await adminApi.deleteAdminTeacher(row.id)
    ElMessage.success('教师已删除')
    await Promise.all([loadAdminTeachers(), loadAdminTeacherOptions(), loadAdminInstances()])
  } catch (e) {}
}

function resetTeacherForm() {
  teacherForm.value = blankTeacherForm()
}

// ===== 开课实例 =====
async function loadAdminInstances() {
  instanceLoading.value = true
  try {
    const params = {
      page: instancePage.value.current,
      pageSize: instancePage.value.size
    }
    if (instanceFilter.value.courseName) params.courseName = instanceFilter.value.courseName
    if (instanceFilter.value.teacherName) params.teacherName = instanceFilter.value.teacherName
    const res = await adminApi.getAdminCourseInstances(params)
    applyPageResult(res.data, adminInstances, instancePage)
    if (adminInstances.value.length === 0 && instancePage.value.current > 1 && instancePage.value.total > 0) {
      instancePage.value.current -= 1
      await loadAdminInstances()
    }
  } finally {
    instanceLoading.value = false
  }
}

function searchInstances() {
  instancePage.value.current = 1
  loadAdminInstances()
}

function resetInstanceFilter() {
  instanceFilter.value = { courseName: '', teacherName: '' }
  instancePage.value.current = 1
  loadAdminInstances()
}

function handleInstancePageChange(page) {
  instancePage.value.current = page
  loadAdminInstances()
}

function handleInstanceSizeChange(size) {
  instancePage.value.size = size
  instancePage.value.current = 1
  loadAdminInstances()
}

async function handleSaveInstance() {
  const payload = {
    courseBaseId: instanceForm.value.courseBaseId,
    teacherId: instanceForm.value.teacherId
  }
  if (!payload.courseBaseId || !payload.teacherId) {
    ElMessage.warning('请选择课程和教师')
    return
  }
  if (instanceForm.value.id) {
    await adminApi.updateAdminCourseInstance(instanceForm.value.id, payload)
    ElMessage.success('开课实例已更新')
  } else {
    await adminApi.createAdminCourseInstance(payload)
    ElMessage.success('开课实例已创建')
  }
  resetInstanceForm()
  await loadAdminInstances()
}

function editInstance(row) {
  instanceForm.value = {
    id: row.id,
    courseBaseId: row.courseBaseId,
    teacherId: row.teacherId
  }
}

async function handleDeleteInstance(row) {
  try {
    await ElMessageBox.confirm(`确认删除开课实例 ${formatInstanceCourse(row)} / ${row.teacherName || teacherNameById(row.teacherId)}？`, '删除开课实例', {
      type: 'warning'
    })
    await adminApi.deleteAdminCourseInstance(row.id)
    ElMessage.success('开课实例已删除')
    await loadAdminInstances()
  } catch (e) {}
}

function resetInstanceForm() {
  instanceForm.value = blankInstanceForm()
}

function formatInstanceCourse(row) {
  if (row.courseName) {
    const code = row.courseCode ? `${row.courseCode} ` : ''
    return `${code}${row.courseName}`.trim()
  }
  return courseNameById(row.courseBaseId)
}

function courseNameById(courseBaseId) {
  const course = adminCourseOptions.value.find((item) => item.id === courseBaseId)
    || adminCourses.value.find((item) => item.id === courseBaseId)
  return course ? `${course.courseCode} ${course.courseName}` : `课程 #${courseBaseId || '-'}`
}

function teacherNameById(teacherId) {
  const teacher = adminTeacherOptions.value.find((item) => item.id === teacherId)
    || adminTeachers.value.find((item) => item.id === teacherId)
  return teacher?.teacherName || `教师 #${teacherId || '-'}`
}

function formatScore(score) {
  return Number(score || 0).toFixed(1)
}

onMounted(() => {
  loadAdminCourses()
  loadAdminTeachers()
  loadAdminInstances()
  loadAdminCourseOptions()
  loadAdminTeacherOptions()
})
</script>
