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
              <el-input v-model="instanceFilter.semester" placeholder="学期" style="width: 150px" clearable
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
        <div class="section-toolbar search-bar audit-search-bar">
          <el-select v-model="auditFilter.operateType" placeholder="操作类型" style="width: 190px" clearable>
            <el-option
              v-for="item in auditOperateOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-input v-model="auditFilter.operatorId" placeholder="管理员 ID" style="width: 130px" clearable
            @keyup.enter="searchAuditLogs" />
          <el-input v-model="auditFilter.reviewId" placeholder="评价 ID" style="width: 130px" clearable
            @keyup.enter="searchAuditLogs" />
          <el-input v-model="auditFilter.courseName" placeholder="课程名称" style="width: 180px" clearable
            @keyup.enter="searchAuditLogs" />
          <el-date-picker
            v-model="auditFilter.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 360px"
          />
          <el-button type="primary" @click="searchAuditLogs">搜索</el-button>
          <el-button @click="resetAuditFilter">重置</el-button>
        </div>
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

      <el-tab-pane v-if="canMaintainData" label="数据导入" name="import">
        <div class="import-section">
          <el-alert type="info" :closable="false" class="import-alert">
            <template #title>
              <ul class="import-hints">
                <li>支持 CSV 格式文件，编码建议 <b>UTF-8</b></li>
                <li>第一行为表头，数据从第二行开始</li>
                <li>课程代码和课程名称为必填字段</li>
                <li>重复的课程/教师/开课实例会自动跳过，<b>不会覆盖</b>已有数据</li>
              </ul>
            </template>
          </el-alert>

          <div class="import-toolbar">
            <el-button type="primary" @click="downloadTemplate">
              <el-icon style="margin-right:4px"><Download /></el-icon>下载导入模板
            </el-button>
          </div>

          <el-upload
            class="import-upload"
            drag
            accept=".csv"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽 CSV 文件到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .csv 格式文件</div>
            </template>
          </el-upload>

          <div class="import-action">
            <el-button
              type="success"
              :disabled="!uploadFile || importLoading"
              :loading="importLoading"
              @click="handleImport"
            >
              开始导入
            </el-button>
          </div>

          <div v-if="importResult" class="import-result">
            <el-divider>导入结果</el-divider>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-card shadow="hover" class="result-card success">
                  <div class="result-num">{{ importResult.successCount }}</div>
                  <div class="result-label">成功</div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" class="result-card skip">
                  <div class="result-num">{{ importResult.skipCount }}</div>
                  <div class="result-label">跳过</div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" class="result-card fail">
                  <div class="result-num">{{ importResult.failCount }}</div>
                  <div class="result-label">失败</div>
                </el-card>
              </el-col>
            </el-row>

            <div v-if="importResult.failures && importResult.failures.length > 0" class="failure-section">
              <div class="failure-header">
                <h4>失败详情</h4>
                <el-button size="small" @click="copyErrors">复制错误信息</el-button>
              </div>
              <el-table :data="importResult.failures" border size="small">
                <el-table-column prop="row" label="行号" width="80" />
                <el-table-column prop="courseCode" label="课程代码" width="150" />
                <el-table-column prop="courseName" label="课程名称" min-width="200" />
                <el-table-column prop="reason" label="失败原因" min-width="250" />
              </el-table>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, UploadFilled } from '@element-plus/icons-vue'
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
const auditFilter = ref(blankAuditFilter())
const pageSizeOptions = [10, 20, 50, 100]

const adminAccounts = ref([])
const accountLoading = ref(false)
const accountForm = ref({ username: '', password: '', role: 'AUDITOR', department: '' })

const adminCourses = ref([])
const adminCourseOptions = ref([])
const courseLoading = ref(false)
const courseForm = ref(blankCourseForm())
const coursePage = ref(blankPageState())
const adminTeachers = ref([])
const adminTeacherOptions = ref([])
const teacherLoading = ref(false)
const teacherForm = ref(blankTeacherForm())
const teacherPage = ref(blankPageState())
const adminInstances = ref([])
const instanceLoading = ref(false)
const instanceForm = ref(blankInstanceForm())
const instancePage = ref(blankPageState())

// 筛选条件
const courseFilter = ref({ courseCode: '', courseName: '', department: '' })
const teacherFilter = ref({ teacherName: '', department: '' })
const instanceFilter = ref({ courseName: '', teacherName: '', semester: '' })

// 数据导入
const uploadFile = ref(null)
const fileList = ref([])
const importLoading = ref(false)
const importResult = ref(null)

const adminRoleOptions = [
  { value: 'SUPER_ADMIN', label: '超级管理员' },
  { value: 'DEPT_OP', label: '院系维护员' },
  { value: 'AUDITOR', label: '内容审核员' }
]

const auditOperateOptions = [
  'APPROVE_REVIEW',
  'HIDE_REVIEW',
  'DELETE_REVIEW',
  'RESOLVE_REPORT',
  'DISMISS_REPORT',
  'CREATE_TAG',
  'DELETE_TAG',
  'CREATE_COURSE',
  'UPDATE_COURSE',
  'DELETE_COURSE',
  'CREATE_TEACHER',
  'UPDATE_TEACHER',
  'DELETE_TEACHER',
  'CREATE_COURSE_INSTANCE',
  'UPDATE_COURSE_INSTANCE',
  'DELETE_COURSE_INSTANCE',
  'IMPORT_COURSES',
  'CREATE_ADMIN',
  'UPDATE_ADMIN_ROLE',
  'RESET_ADMIN_PASSWORD',
  'DELETE_ADMIN'
].map(value => ({ value, label: operateTypeText(value) }))

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
    import: canMaintainData.value,
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
  if (!ensureAdmin() || !canMaintainData.value) return
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
  if (!ensureAdmin() || !canMaintainData.value) return
  try {
    await ElMessageBox.confirm(`确认删除课程 ${row.courseName}？`, '删除课程', { type: 'warning' })
    await adminApi.deleteAdminCourse(row.id)
    ElMessage.success('课程已删除')
    await Promise.all([loadAdminCourses(), loadAdminCourseOptions(), loadAdminInstances()])
  } catch (e) {}
}

async function loadAdminTeachers() {
  if (!ensureAdmin() || !canMaintainData.value) return
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
  if (!ensureAdmin() || !canMaintainData.value) return
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
  if (!ensureAdmin() || !canMaintainData.value) return
  try {
    await ElMessageBox.confirm(`确认删除教师 ${row.teacherName}？`, '删除教师', { type: 'warning' })
    await adminApi.deleteAdminTeacher(row.id)
    ElMessage.success('教师已删除')
    await Promise.all([loadAdminTeachers(), loadAdminTeacherOptions(), loadAdminInstances()])
  } catch (e) {}
}

async function loadAdminInstances() {
  if (!ensureAdmin() || !canMaintainData.value) return
  instanceLoading.value = true
  try {
    const params = {
      page: instancePage.value.current,
      pageSize: instancePage.value.size
    }
    if (instanceFilter.value.courseName) params.courseName = instanceFilter.value.courseName
    if (instanceFilter.value.teacherName) params.teacherName = instanceFilter.value.teacherName
    if (instanceFilter.value.semester) params.semester = instanceFilter.value.semester
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
  instanceFilter.value = { courseName: '', teacherName: '', semester: '' }
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
    const res = await adminApi.getAuditLogs(buildAuditParams())
    auditLogs.value = res.data || []
  } finally {
    auditLogLoading.value = false
  }
}

function buildAuditParams() {
  const params = {}
  if (auditFilter.value.operateType) params.operateType = auditFilter.value.operateType
  if (auditFilter.value.operatorId) params.operatorId = auditFilter.value.operatorId
  if (auditFilter.value.reviewId) params.reviewId = auditFilter.value.reviewId
  if (auditFilter.value.courseName) params.courseName = auditFilter.value.courseName
  if (auditFilter.value.timeRange?.length === 2) {
    params.startTime = auditFilter.value.timeRange[0]
    params.endTime = auditFilter.value.timeRange[1]
  }
  return params
}

function searchAuditLogs() {
  loadAuditLogs()
}

function resetAuditFilter() {
  auditFilter.value = blankAuditFilter()
  loadAuditLogs()
}

function loadDataMaintenance() {
  return Promise.all([
    loadAdminCourses(),
    loadAdminTeachers(),
    loadAdminInstances(),
    loadAdminCourseOptions(),
    loadAdminTeacherOptions()
  ])
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

function blankPageState() {
  return { current: 1, size: 10, total: 0 }
}

function blankAuditFilter() {
  return {
    operateType: '',
    operatorId: '',
    reviewId: '',
    courseName: '',
    timeRange: []
  }
}

function applyPageResult(data, recordsRef, pageRef) {
  recordsRef.value = data?.records || []
  pageRef.value.total = Number(data?.total || 0)
  pageRef.value.current = Number(data?.page || pageRef.value.current || 1)
  pageRef.value.size = Number(data?.size || pageRef.value.size || 10)
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
  const course = adminCourseOptions.value.find((item) => item.id === courseBaseId)
    || adminCourses.value.find((item) => item.id === courseBaseId)
  return course ? `${course.courseCode} ${course.courseName}` : `课程 #${courseBaseId || '-'}`
}

function teacherName(teacherId) {
  const teacher = adminTeacherOptions.value.find((item) => item.id === teacherId)
    || adminTeachers.value.find((item) => item.id === teacherId)
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
    IMPORT_COURSES: '导入课程数据',
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

// ========== 数据导入 ==========

function handleFileChange(file) {
  if (!isCsvFile(file)) {
    ElMessage.warning('仅支持选择 .csv 文件')
    uploadFile.value = null
    fileList.value = []
    importResult.value = null
    return
  }
  uploadFile.value = file.raw
  fileList.value = [file]
  importResult.value = null
}

function handleFileRemove() {
  uploadFile.value = null
  fileList.value = []
  importResult.value = null
}

function isCsvFile(file) {
  const name = file?.name || file?.raw?.name || ''
  return name.toLowerCase().endsWith('.csv')
}

async function handleImport() {
  if (!ensureAdmin() || !canMaintainData.value) return
  if (!uploadFile.value) {
    ElMessage.warning('请先选择 CSV 文件')
    return
  }
  try {
    await ElMessageBox.confirm('确认导入课程数据？重复数据将自动跳过，不会覆盖已有课程。', '确认导入', {
      type: 'warning',
      confirmButtonText: '确认导入',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  importLoading.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    const res = await adminApi.importCourses(formData)
    importResult.value = res.data
    if (importResult.value.failCount === 0 && importResult.value.successCount > 0) {
      ElMessage.success(`导入完成：成功 ${importResult.value.successCount} 门，跳过 ${importResult.value.skipCount} 门`)
    } else if (importResult.value.failCount > 0) {
      ElMessage.warning(`导入完成：成功 ${importResult.value.successCount} 门，跳过 ${importResult.value.skipCount} 门，失败 ${importResult.value.failCount} 门`)
    } else if (importResult.value.successCount === 0) {
      ElMessage.info('导入完成：没有新增课程，全部已存在')
    }
    await Promise.all([
      loadAdminCourses(),
      loadAdminTeachers(),
      loadAdminInstances(),
      loadAdminCourseOptions(),
      loadAdminTeacherOptions()
    ])
  } catch (e) {
    ElMessage.error('导入失败：' + (e.response?.data?.message || e.message || '未知错误'))
  } finally {
    importLoading.value = false
  }
}

function downloadTemplate() {
  if (!ensureAdmin() || !canMaintainData.value) return
  adminApi.downloadImportTemplate().then(res => {
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'course_import_template.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }).catch(() => {
    ElMessage.error('模板下载失败')
  })
}

function copyErrors() {
  if (!importResult.value?.failures?.length) return
  const text = importResult.value.failures.map(
    f => `行${f.row}: [${f.courseCode}] ${f.courseName} — ${f.reason}`
  ).join('\n')
  navigator.clipboard.writeText(text).then(
    () => ElMessage.success('错误信息已复制到剪贴板'),
    () => ElMessage.warning('复制失败，请手动复制')
  )
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

/* 数据导入 */
.import-section {
  max-width: 800px;
}

.import-alert {
  margin-bottom: 16px;
}

.import-hints {
  margin: 0;
  padding-left: 18px;
}

.import-hints li {
  margin-bottom: 4px;
}

.import-toolbar {
  margin-bottom: 16px;
}

.import-upload {
  margin-bottom: 16px;
}

.import-action {
  margin-bottom: 24px;
}

.import-result .result-card {
  text-align: center;
  border-radius: 8px;
}

.import-result .result-card.success {
  border-color: #67c23a;
  background: #f0f9eb;
}

.import-result .result-card.skip {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.import-result .result-card.fail {
  border-color: #f56c6c;
  background: #fef0f0;
}

.import-result .result-num {
  font-size: 36px;
  font-weight: bold;
  line-height: 1.2;
}

.import-result .result-card.success .result-num {
  color: #67c23a;
}

.import-result .result-card.skip .result-num {
  color: #e6a23c;
}

.import-result .result-card.fail .result-num {
  color: #f56c6c;
}

.import-result .result-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.failure-section {
  margin-top: 20px;
}

.failure-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.failure-header h4 {
  margin: 0;
  font-size: 15px;
}
</style>
