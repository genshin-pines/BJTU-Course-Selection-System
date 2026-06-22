<template>
  <div class="apply-page">
    <el-card class="apply-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
          <span class="header-title">申请新课程</span>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 20px">
        没找到你想评价的课程？在这里填写课程和教师信息并附上你的评价，管理员审核通过后将自动创建课程并发布你的评价。
      </el-alert>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <!-- 课程信息 -->
        <div class="section-label">课程信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程代码" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="如 CS-101" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="如 数据结构" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学分">
              <el-input-number v-model="form.credit" :min="0" :max="10" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学院">
              <el-input v-model="form.department" placeholder="如 计算机学院" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 教师信息 -->
        <div class="section-label">教师信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="教师姓名" prop="teacherName">
              <el-input v-model="form.teacherName" placeholder="如 张三" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教师所属学院">
              <el-input v-model="form.teacherDepartment" placeholder="不填则默认与课程学院一致" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 评价信息 -->
        <div class="section-label">你的评价</div>
        <el-form-item label="给分情况" prop="gradingScore">
          <el-rate v-model="form.gradingScore" :max="5" />
        </el-form-item>
        <el-form-item label="授课质量" prop="teachingScore">
          <el-rate v-model="form.teachingScore" :max="5" />
        </el-form-item>
        <el-form-item label="作业轻松度" prop="workloadScore">
          <el-rate v-model="form.workloadScore" :max="5" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="分享你的上课体验..." />
        </el-form-item>
        <el-form-item label="考核方式">
          <el-input v-model="form.examType" placeholder="如：闭卷考试+平时作业" />
        </el-form-item>
        <el-form-item label="重点章节">
          <el-input v-model="form.keyChapters" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="复习建议">
          <el-input v-model="form.studyTips" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="标签">
          <el-checkbox-group v-model="form.tagIds">
            <el-checkbox v-for="tag in tags" :key="tag.id" :label="tag.id">{{ tag.tagName }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">提交申请</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 我的申请记录 -->
    <el-card class="apply-card" style="margin-top: 20px">
      <template #header>
        <span class="header-title">我的申请记录</span>
      </template>
      <el-empty v-if="myApplications.length === 0" description="暂无申请记录" />
      <div class="app-list" v-else>
        <div class="app-item" v-for="app in myApplications" :key="app.id">
          <div class="app-item-head">
            <span class="app-course">{{ app.courseName }} ({{ app.courseCode }})</span>
            <span class="app-teacher">{{ app.teacherName }}</span>
            <span class="app-status" :class="statusClass(app.status)">{{ statusText(app.status) }}</span>
          </div>
          <p class="app-content">{{ app.content }}</p>
          <div class="app-time">{{ formatTime(app.createTime) }}</div>
          <div class="app-reason" v-if="app.reviewReason">
            <span class="reason-label">审核备注：</span>{{ app.reviewReason }}
          </div>
          <div class="app-link" v-if="app.status === 'APPROVED' && app.createdInstanceId">
            <router-link :to="`/course/instance/${app.createdInstanceId}`">查看已创建的课程 →</router-link>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { courseAppApi } from '@/api/courseApplication'
import { tagApi } from '@/api/tag'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const tags = ref([])
const myApplications = ref([])

const form = reactive({
  courseCode: '', courseName: '', credit: 0, department: '',
  teacherName: '', teacherDepartment: '',
  gradingScore: 0, teachingScore: 0, workloadScore: 0,
  content: '', examType: '', keyChapters: '', studyTips: '',
  tagIds: []
})

const rules = {
  courseCode: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherName: [{ required: true, message: '请输入教师姓名', trigger: 'blur' }],
  gradingScore: [{ required: true, validator: (_, v, cb) => v > 0 ? cb() : cb(new Error('请评分')), trigger: 'change' }],
  teachingScore: [{ required: true, validator: (_, v, cb) => v > 0 ? cb() : cb(new Error('请评分')), trigger: 'change' }],
  workloadScore: [{ required: true, validator: (_, v, cb) => v > 0 ? cb() : cb(new Error('请评分')), trigger: 'change' }],
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await courseAppApi.submit({ ...form })
    ElMessage.success('申请已提交，等待管理员审核')
    Object.assign(form, { courseCode: '', courseName: '', credit: 0, department: '', teacherName: '', teacherDepartment: '', gradingScore: 0, teachingScore: 0, workloadScore: 0, content: '', examType: '', keyChapters: '', studyTips: '', tagIds: [] })
    await loadMyApplications()
  } catch (e) { /* interceptor handles */ } finally {
    submitting.value = false
  }
}

async function loadMyApplications() {
  try {
    const res = await courseAppApi.getMine()
    myApplications.value = res.data || []
  } catch (e) { console.error(e) }
}

async function loadTags() {
  try {
    const res = await tagApi.getAll()
    tags.value = res.data || []
  } catch (e) { console.error(e) }
}

function statusText(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s
}
function statusClass(s) {
  return { PENDING: 'st-pending', APPROVED: 'st-ok', REJECTED: 'st-reject' }[s] || ''
}
function formatTime(t) {
  return t ? new Date(t).toLocaleString('zh-CN') : ''
}

onMounted(() => { loadTags(); loadMyApplications() })
</script>

<style scoped>
.apply-page { max-width: 760px; margin: 0 auto; }
.card-header { display: flex; align-items: center; gap: 16px; }
.header-title { font-size: 18px; font-weight: 700; }
.section-label {
  font-size: 15px; font-weight: 700; color: var(--brand-primary);
  margin: 16px 0 12px; padding-left: 10px;
  border-left: 3px solid var(--brand-primary);
}
.form-actions { margin-top: 20px; display: flex; gap: 12px; }

.app-list { display: flex; flex-direction: column; gap: 12px; }
.app-item {
  padding: 14px 16px; border: 1px solid var(--border-soft);
  border-radius: var(--r-md); background: var(--bg-page);
}
.app-item-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.app-course { font-weight: 700; font-size: 15px; }
.app-teacher { color: var(--text-3); font-size: 13px; }
.app-status { font-size: 12px; font-weight: 600; padding: 2px 10px; border-radius: var(--r-pill); }
.app-status.st-pending { background: #fffbeb; color: var(--c-warning); }
.app-status.st-ok { background: #f0fdf4; color: var(--c-success); }
.app-status.st-reject { background: #fef2f2; color: var(--c-danger); }
.app-content { font-size: 14px; color: var(--text-2); line-height: 1.7; margin: 6px 0; }
.app-time { font-size: 12px; color: var(--text-3); }
.app-reason { font-size: 13px; color: var(--text-2); margin-top: 6px; }
.reason-label { font-weight: 600; }
.app-link { margin-top: 8px; }
.app-link a { color: var(--brand-primary); font-size: 13px; font-weight: 600; }
</style>
