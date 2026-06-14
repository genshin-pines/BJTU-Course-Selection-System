<template>
  <div class="admin-container">
    <div class="admin-role-bar">当前角色：{{ adminRoleText }}</div>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-if="canGovernContent" label="评价审核" name="reviews">
        <el-table :data="pendingReviews" v-loading="reviewLoading" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="courseName" label="课程" width="180" />
          <el-table-column prop="teacherName" label="教师" width="120" />
          <el-table-column label="开课实例" width="190">
            <template #default="{ row }">{{ formatInstance(row) }}</template>
          </el-table-column>
          <el-table-column label="评分" width="170">
            <template #default="{ row }">
              给分: {{ row.gradingScore }} | 授课: {{ row.teachingScore }} | 作业: {{ row.workloadScore }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="220" show-overflow-tooltip />
          <el-table-column prop="anonymousId" label="用户" width="140" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="approveReview(row.id)">通过</el-button>
              <el-button type="danger" size="small" @click="rejectReview(row.id)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!reviewLoading && pendingReviews.length === 0" description="暂无待审核评价" />
      </el-tab-pane>

      <el-tab-pane v-if="canGovernContent" label="举报管理" name="reports">
        <el-table :data="reports" v-loading="reportLoading" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="courseName" label="课程" width="180" />
          <el-table-column prop="teacherName" label="教师" width="120" />
          <el-table-column label="开课实例" width="190">
            <template #default="{ row }">{{ formatInstance(row) }}</template>
          </el-table-column>
          <el-table-column prop="anonymousId" label="评价用户" width="140" />
          <el-table-column prop="reporterAnonymousId" label="举报用户" width="140" />
          <el-table-column prop="reviewContent" label="被举报内容" min-width="220" show-overflow-tooltip />
          <el-table-column prop="reason" label="举报原因" width="220" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="reportStatusType(row.status)">{{ reportStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button type="primary" size="small" @click="resolveReport(row.id)">采纳</el-button>
                <el-button type="warning" size="small" @click="dismissReport(row.id)">驳回</el-button>
              </template>
              <span v-else class="muted">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!reportLoading && reports.length === 0" description="暂无举报" />
      </el-tab-pane>

      <el-tab-pane v-if="canMaintainData" label="标签管理" name="tags">
        <div class="section-toolbar">
          <el-input v-model="newTagName" placeholder="输入新标签名" style="width: 240px" />
          <el-button type="primary" @click="handleCreateTag">添加标签</el-button>
        </div>
        <div class="tag-list">
          <el-tag
            v-for="tag in tags"
            :key="tag.id"
            closable
            size="large"
            @close="handleDeleteTag(tag.id)"
          >
            {{ tag.tagName }}
          </el-tag>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canMaintainData" label="基础数据" name="dataMaintenance">
        <el-tabs v-model="dataTab">
          <el-tab-pane label="课程" name="courses">
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
          </el-tab-pane>

          <el-tab-pane label="教师" name="teachers">
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
          </el-tab-pane>

          <el-tab-pane label="开课实例" name="instances">
            <div class="section-toolbar">
              <el-select v-model="instanceForm.courseBaseId" filterable placeholder="选择课程" style="width: 260px">
                <el-option
                  v-for="course in adminCourses"
                  :key="course.id"
                  :label="`${course.courseCode} ${course.courseName}`"
                  :value="course.id"
                />
              </el-select>
              <el-select v-model="instanceForm.teacherId" filterable placeholder="选择教师" style="width: 220px">
                <el-option
                  v-for="teacher in adminTeachers"
                  :key="teacher.id"
                  :label="teacher.teacherName"
                  :value="teacher.id"
                />
              </el-select>
              <el-input v-model="instanceForm.semester" placeholder="学期，如 2026春" style="width: 150px" />
              <el-input v-model="instanceForm.className" placeholder="班级" style="width: 160px" />
              <el-button type="primary" @click="handleSaveInstance">
                {{ instanceForm.id ? '保存实例' : '新增实例' }}
              </el-button>
              <el-button v-if="instanceForm.id" @click="resetInstanceForm">取消编辑</el-button>
            </div>
            <el-table :data="adminInstances" v-loading="instanceLoading" border>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column label="课程" min-width="220">
                <template #default="{ row }">{{ courseName(row.courseBaseId) }}</template>
              </el-table-column>
              <el-table-column label="教师" width="140">
                <template #default="{ row }">{{ teacherName(row.teacherId) }}</template>
              </el-table-column>
              <el-table-column prop="semester" label="学期" width="130" />
              <el-table-column prop="className" label="班级" width="150" />
              <el-table-column prop="reviewCount" label="评价数" width="90" />
              <el-table-column label="操作" width="170" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="editInstance(row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="handleDeleteInstance(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>

      <el-tab-pane v-if="canManageAdmins" label="管理员账号" name="accounts">
        <div class="section-toolbar">
          <el-input v-model="accountForm.username" placeholder="账号，如 dept_op2" style="width: 180px" />
          <el-input v-model="accountForm.password" type="password" show-password placeholder="至少 6 位" style="width: 180px" />
          <el-select v-model="accountForm.role" style="width: 160px">
            <el-option v-for="role in adminRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
          <el-input v-model="accountForm.department" placeholder="所属院系（DEPT_OP）" style="width: 220px" />
          <el-button type="primary" @click="handleCreateAccount">新增管理员</el-button>
        </div>
        <el-table :data="adminAccounts" v-loading="accountLoading" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="账号" min-width="160" />
          <el-table-column label="角色" width="180">
            <template #default="{ row }">
              <el-select :model-value="row.role" size="small" @change="(role) => handleUpdateAccountRole(row, role)">
                <el-option v-for="role in adminRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="department" label="所属院系" min-width="180">
            <template #default="{ row }">{{ row.department || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleUpdateAccountDepartment(row)">设置院系</el-button>
              <el-button size="small" @click="handleResetAccountPassword(row)">重置密码</el-button>
              <el-button
                type="danger"
                size="small"
                :disabled="row.username === authStore.userInfo?.username"
                @click="handleDeleteAccount(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="canViewAuditLogs" label="审计日志" name="auditLogs">
        <el-table :data="auditLogs" v-loading="auditLogLoading" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="adminId" label="管理员 ID" width="110" />
          <el-table-column prop="operateType" label="操作类型" width="150">
            <template #default="{ row }">{{ operateTypeText(row.operateType) }}</template>
          </el-table-column>
          <el-table-column prop="reviewId" label="评价 ID" width="100" />
          <el-table-column prop="reportId" label="举报 ID" width="100" />
          <el-table-column prop="courseName" label="课程" width="180" />
          <el-table-column prop="teacherName" label="教师" width="120" />
          <el-table-column label="开课实例" width="190">
            <template #default="{ row }">{{ formatInstance(row) }}</template>
          </el-table-column>
          <el-table-column prop="anonymousId" label="评价用户" width="140" />
          <el-table-column prop="reviewContent" label="评价摘要" min-width="180" show-overflow-tooltip />
          <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作时间" width="190">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { tagApi } from '@/api/tag'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const activeTab = ref('reviews')
const dataTab = ref('courses')

const pendingReviews = ref([])
const reviewLoading = ref(false)
const reports = ref([])
const reportLoading = ref(false)
const tags = ref([])
const newTagName = ref('')
const auditLogs = ref([])
const auditLogLoading = ref(false)

const adminAccounts = ref([])
const accountLoading = ref(false)
const accountForm = ref({ username: '', password: '', role: 'AUDITOR', department: '' })

const adminCourses = ref([])
const courseLoading = ref(false)
const courseForm = ref(blankCourseForm())
const adminTeachers = ref([])
const teacherLoading = ref(false)
const teacherForm = ref(blankTeacherForm())
const adminInstances = ref([])
const instanceLoading = ref(false)
const instanceForm = ref(blankInstanceForm())

const adminRoleOptions = [
  { value: 'SUPER_ADMIN', label: '超级管理员' },
  { value: 'DEPT_OP', label: '院系维护员' },
  { value: 'AUDITOR', label: '内容审核员' }
]

const adminRole = computed(() => authStore.adminRole || 'SUPER_ADMIN')
const canGovernContent = computed(() => ['SUPER_ADMIN', 'AUDITOR'].includes(adminRole.value))
const canMaintainData = computed(() => ['SUPER_ADMIN', 'DEPT_OP'].includes(adminRole.value))
const canManageAdmins = computed(() => adminRole.value === 'SUPER_ADMIN')
const canViewAuditLogs = computed(() => ['SUPER_ADMIN', 'DEPT_OP', 'AUDITOR'].includes(adminRole.value))
const adminRoleText = computed(() => roleText(adminRole.value))

function ensureAdmin() {
  authStore.syncFromStorage()
  if (!authStore.isAdmin) {
    ElMessage.warning('请先登录管理员账号')
    return false
  }
  return true
}

function firstAccessibleTab() {
  if (canGovernContent.value) return 'reviews'
  if (canMaintainData.value) return 'tags'
  if (canManageAdmins.value) return 'accounts'
  if (canViewAuditLogs.value) return 'auditLogs'
  return ''
}

function ensureAccessibleTab() {
  const allowed = {
    reviews: canGovernContent.value,
    reports: canGovernContent.value,
    tags: canMaintainData.value,
    dataMaintenance: canMaintainData.value,
    accounts: canManageAdmins.value,
    auditLogs: canViewAuditLogs.value
  }
  if (!allowed[activeTab.value]) {
    activeTab.value = firstAccessibleTab()
  }
}

async function askReason(title, placeholder) {
  const { value } = await ElMessageBox.prompt(placeholder, title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPattern: /\S+/,
    inputErrorMessage: '请填写操作原因'
  })
  return value.trim()
}

async function loadPendingReviews() {
  if (!ensureAdmin() || !canGovernContent.value) return
  reviewLoading.value = true
  try {
    const res = await adminApi.getPendingReviews()
    pendingReviews.value = res.data || []
  } finally {
    reviewLoading.value = false
  }
}

async function approveReview(id) {
  if (!ensureAdmin() || !canGovernContent.value) return
  try {
    const reason = await askReason('审核通过', '请填写通过该评价的原因')
    await adminApi.approveReview(id, reason)
    ElMessage.success('已通过')
    await Promise.all([loadPendingReviews(), loadAuditLogsIfActive()])
  } catch (e) {}
}

async function rejectReview(id) {
  if (!ensureAdmin() || !canGovernContent.value) return
  try {
    const reason = await askReason('审核拒绝', '请填写拒绝该评价的原因')
    await adminApi.rejectReview(id, reason)
    ElMessage.success('已拒绝')
    await Promise.all([loadPendingReviews(), loadAuditLogsIfActive()])
  } catch (e) {}
}

async function loadReports() {
  if (!ensureAdmin() || !canGovernContent.value) return
  reportLoading.value = true
  try {
    const res = await adminApi.getAllReports()
    reports.value = res.data || []
  } finally {
    reportLoading.value = false
  }
}

async function resolveReport(id) {
  if (!ensureAdmin() || !canGovernContent.value) return
  try {
    const reason = await askReason('采纳举报', '请填写采纳原因，采纳后将隐藏/删除对应评价')
    await adminApi.resolveReport(id, reason)
    ElMessage.success('举报已采纳')
    await Promise.all([loadReports(), loadAuditLogsIfActive()])
  } catch (e) {}
}

async function dismissReport(id) {
  if (!ensureAdmin() || !canGovernContent.value) return
  try {
    const reason = await askReason('驳回举报', '请填写驳回该举报的原因')
    await adminApi.dismissReport(id, reason)
    ElMessage.success('举报已驳回')
    await Promise.all([loadReports(), loadAuditLogsIfActive()])
  } catch (e) {}
}

async function loadTags() {
  if (!ensureAdmin() || !canMaintainData.value) return
  const res = await tagApi.getAll()
  tags.value = res.data || []
}

async function handleCreateTag() {
  if (!ensureAdmin() || !canMaintainData.value) return
  if (!newTagName.value.trim()) {
    ElMessage.warning('请输入标签名')
    return
  }
  await adminApi.createTag(newTagName.value.trim())
  ElMessage.success('标签添加成功')
  newTagName.value = ''
  await loadTags()
}

async function handleDeleteTag(id) {
  if (!ensureAdmin() || !canMaintainData.value) return
  await adminApi.deleteTag(id)
  ElMessage.success('标签已删除')
  await loadTags()
}

async function loadAdminAccounts() {
  if (!ensureAdmin() || !canManageAdmins.value) return
  accountLoading.value = true
  try {
    const res = await adminApi.getAdminAccounts()
    adminAccounts.value = res.data || []
  } finally {
    accountLoading.value = false
  }
}

async function handleCreateAccount() {
  if (!ensureAdmin() || !canManageAdmins.value) return
  if (!accountForm.value.username.trim() || !accountForm.value.password.trim()) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  await adminApi.createAdminAccount({
    username: accountForm.value.username.trim(),
    password: accountForm.value.password.trim(),
    role: accountForm.value.role,
    department: accountForm.value.department.trim()
  })
  ElMessage.success('管理员账号已创建')
  accountForm.value = { username: '', password: '', role: 'AUDITOR', department: '' }
  await loadAdminAccounts()
}

async function handleUpdateAccountRole(row, role) {
  if (!ensureAdmin() || !canManageAdmins.value || row.role === role) return
  try {
    await adminApi.updateAdminRole(row.id, role, row.department || '')
    ElMessage.success('角色已更新')
  } finally {
    await loadAdminAccounts()
  }
}

async function handleUpdateAccountDepartment(row) {
  if (!ensureAdmin() || !canManageAdmins.value) return
  try {
    const { value } = await ElMessageBox.prompt(`请输入 ${row.username} 的所属院系`, '设置院系范围', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValue: row.department || ''
    })
    await adminApi.updateAdminRole(row.id, row.role, value.trim())
    ElMessage.success('院系范围已更新')
    await loadAdminAccounts()
  } catch (e) {}
}

async function handleResetAccountPassword(row) {
  if (!ensureAdmin() || !canManageAdmins.value) return
  try {
    const { value } = await ElMessageBox.prompt(`请输入 ${row.username} 的新密码`, '重置密码', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /\S{6,}/,
      inputErrorMessage: '密码至少 6 位'
    })
    await adminApi.resetAdminPassword(row.id, value.trim())
    ElMessage.success('密码已重置')
  } catch (e) {}
}

async function handleDeleteAccount(row) {
  if (!ensureAdmin() || !canManageAdmins.value) return
  try {
    await ElMessageBox.confirm(`确认删除管理员账号 ${row.username}？`, '删除管理员', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminApi.deleteAdminAccount(row.id)
    ElMessage.success('管理员账号已删除')
    await loadAdminAccounts()
  } catch (e) {}
}

async function loadAdminCourses() {
  if (!ensureAdmin() || !canMaintainData.value) return
  courseLoading.value = true
  try {
    const res = await adminApi.getAdminCourses()
    adminCourses.value = res.data || []
  } finally {
    courseLoading.value = false
  }
}

async function handleSaveCourse() {
  if (!ensureAdmin() || !canMaintainData.value) return
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
  await loadAdminCourses()
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
  if (!ensureAdmin() || !canMaintainData.value) return
  try {
    await ElMessageBox.confirm(`确认删除课程 ${row.courseName}？`, '删除课程', { type: 'warning' })
    await adminApi.deleteAdminCourse(row.id)
    ElMessage.success('课程已删除')
    await Promise.all([loadAdminCourses(), loadAdminInstances()])
  } catch (e) {}
}

async function loadAdminTeachers() {
  if (!ensureAdmin() || !canMaintainData.value) return
  teacherLoading.value = true
  try {
    const res = await adminApi.getAdminTeachers()
    adminTeachers.value = res.data || []
  } finally {
    teacherLoading.value = false
  }
}

async function handleSaveTeacher() {
  if (!ensureAdmin() || !canMaintainData.value) return
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
  await loadAdminTeachers()
}

function editTeacher(row) {
  teacherForm.value = {
    id: row.id,
    teacherName: row.teacherName || '',
    department: row.department || ''
  }
}

async function handleDeleteTeacher(row) {
  if (!ensureAdmin() || !canMaintainData.value) return
  try {
    await ElMessageBox.confirm(`确认删除教师 ${row.teacherName}？`, '删除教师', { type: 'warning' })
    await adminApi.deleteAdminTeacher(row.id)
    ElMessage.success('教师已删除')
    await Promise.all([loadAdminTeachers(), loadAdminInstances()])
  } catch (e) {}
}

async function loadAdminInstances() {
  if (!ensureAdmin() || !canMaintainData.value) return
  instanceLoading.value = true
  try {
    const res = await adminApi.getAdminCourseInstances()
    adminInstances.value = res.data || []
  } finally {
    instanceLoading.value = false
  }
}

async function handleSaveInstance() {
  if (!ensureAdmin() || !canMaintainData.value) return
  const payload = {
    courseBaseId: instanceForm.value.courseBaseId,
    teacherId: instanceForm.value.teacherId,
    semester: instanceForm.value.semester.trim(),
    className: instanceForm.value.className.trim()
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
    teacherId: row.teacherId,
    semester: row.semester || '',
    className: row.className || ''
  }
}

async function handleDeleteInstance(row) {
  if (!ensureAdmin() || !canMaintainData.value) return
  try {
    await ElMessageBox.confirm(`确认删除开课实例 ${courseName(row.courseBaseId)} / ${row.semester || '-'}？`, '删除开课实例', {
      type: 'warning'
    })
    await adminApi.deleteAdminCourseInstance(row.id)
    ElMessage.success('开课实例已删除')
    await loadAdminInstances()
  } catch (e) {}
}

async function loadAuditLogs() {
  if (!ensureAdmin() || !canViewAuditLogs.value) return
  auditLogLoading.value = true
  try {
    const res = await adminApi.getAuditLogs()
    auditLogs.value = res.data || []
  } finally {
    auditLogLoading.value = false
  }
}

function loadDataMaintenance() {
  return Promise.all([loadAdminCourses(), loadAdminTeachers(), loadAdminInstances()])
}

function loadAuditLogsIfActive() {
  return activeTab.value === 'auditLogs' ? loadAuditLogs() : Promise.resolve()
}

function loadActiveTab() {
  if (activeTab.value === 'reviews') loadPendingReviews()
  if (activeTab.value === 'reports') loadReports()
  if (activeTab.value === 'tags') loadTags()
  if (activeTab.value === 'dataMaintenance') loadDataMaintenance()
  if (activeTab.value === 'accounts') loadAdminAccounts()
  if (activeTab.value === 'auditLogs') loadAuditLogs()
}

function blankCourseForm() {
  return { id: null, courseCode: '', courseName: '', credit: 0, department: '' }
}

function blankTeacherForm() {
  return { id: null, teacherName: '', department: '' }
}

function blankInstanceForm() {
  return { id: null, courseBaseId: null, teacherId: null, semester: '', className: '' }
}

function resetCourseForm() {
  courseForm.value = blankCourseForm()
}

function resetTeacherForm() {
  teacherForm.value = blankTeacherForm()
}

function resetInstanceForm() {
  instanceForm.value = blankInstanceForm()
}

function courseName(courseBaseId) {
  const course = adminCourses.value.find((item) => item.id === courseBaseId)
  return course ? `${course.courseCode} ${course.courseName}` : `课程 #${courseBaseId || '-'}`
}

function teacherName(teacherId) {
  const teacher = adminTeachers.value.find((item) => item.id === teacherId)
  return teacher?.teacherName || `教师 #${teacherId || '-'}`
}

function reportStatusType(status) {
  if (status === 'PENDING') return 'warning'
  if (status === 'RESOLVED') return 'success'
  return 'info'
}

function reportStatusText(status) {
  if (status === 'PENDING') return '待处理'
  if (status === 'RESOLVED') return '已处理'
  if (status === 'DISMISSED') return '已驳回'
  return status || '-'
}

function operateTypeText(type) {
  const map = {
    APPROVE_REVIEW: '审核通过',
    REJECT_REVIEW: '审核拒绝',
    HIDE_REVIEW: '审核拒绝',
    DELETE_REVIEW: '删除评价',
    RESOLVE_REPORT: '采纳举报',
    DISMISS_REPORT: '驳回举报',
    CREATE_TAG: '新增标签',
    DELETE_TAG: '删除标签',
    CREATE_COURSE: '新增课程',
    UPDATE_COURSE: '更新课程',
    DELETE_COURSE: '删除课程',
    CREATE_TEACHER: '新增教师',
    UPDATE_TEACHER: '更新教师',
    DELETE_TEACHER: '删除教师',
    CREATE_COURSE_INSTANCE: '新增开课实例',
    UPDATE_COURSE_INSTANCE: '更新开课实例',
    DELETE_COURSE_INSTANCE: '删除开课实例',
    CREATE_ADMIN: '新增管理员',
    UPDATE_ADMIN_ROLE: '更新管理员角色',
    RESET_ADMIN_PASSWORD: '重置管理员密码',
    DELETE_ADMIN: '删除管理员'
  }
  return map[type] || type || '-'
}

function roleText(role) {
  const map = {
    SUPER_ADMIN: '超级管理员',
    DEPT_OP: '院系维护员',
    AUDITOR: '内容审核员'
  }
  return map[role] || role || '-'
}

function formatInstance(row) {
  const parts = []
  if (row.courseInstanceId) parts.push(`#${row.courseInstanceId}`)
  if (row.semester) parts.push(row.semester)
  if (row.className) parts.push(row.className)
  return parts.length ? parts.join(' / ') : '-'
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function formatScore(score) {
  return Number(score || 0).toFixed(1)
}

watch(activeTab, () => {
  ensureAccessibleTab()
  loadActiveTab()
})

onMounted(() => {
  authStore.syncFromStorage()
  ensureAccessibleTab()
  loadActiveTab()
})
</script>

<style scoped>
.admin-container {
  max-width: 1200px;
  margin: 0 auto;
}

.admin-role-bar {
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
  text-align: right;
}

.section-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.tag-list {
  margin-top: 16px;
}

.tag-list .el-tag {
  margin: 4px;
}

.muted {
  color: #999;
}
</style>
