<template>
  <div class="post-review-container">
    <el-card class="post-review-card">
      <template #header>
        <div class="card-header">
          <el-button text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <span>发布课程评价</span>
        </div>
      </template>

      <div v-if="courseInfo" class="course-summary">
        <strong>{{ courseInfo.courseName }}</strong>
        <span class="course-meta">{{ courseInfo.courseCode }} · {{ courseInfo.teacherName || '未分配教师' }}</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="给分情况" prop="gradingScore">
          <el-rate
            v-model="form.gradingScore"
            :max="5"
            show-score
            :texts="['很差', '较差', '一般', '较好', '很好']"
          />
        </el-form-item>

        <el-form-item label="授课质量" prop="teachingScore">
          <el-rate
            v-model="form.teachingScore"
            :max="5"
            show-score
            :texts="['很差', '较差', '一般', '较好', '很好']"
          />
        </el-form-item>

        <el-form-item label="作业轻松度" prop="workloadScore">
          <el-rate
            v-model="form.workloadScore"
            :max="5"
            show-score
            :texts="['很繁重', '较繁重', '适中', '较轻松', '很轻松']"
          />
        </el-form-item>

        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="分享你的课程体验、老师风格、作业强度等..."
          />
        </el-form-item>

        <el-form-item label="考核方式">
          <el-input v-model="form.examType" placeholder="例如：闭卷考试 + 平时作业" />
        </el-form-item>

        <el-form-item label="重点章节">
          <el-input
            v-model="form.keyChapters"
            type="textarea"
            :rows="2"
            placeholder="例如：第 3、5、8 章，老师上课反复强调的案例题"
          />
        </el-form-item>

        <el-form-item label="可带资料">
          <el-radio-group v-model="form.cheatSheetAllowed">
            <el-radio :label="true">可以</el-radio>
            <el-radio :label="false">不可以</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="复习建议">
          <el-input
            v-model="form.studyTips"
            type="textarea"
            :rows="3"
            placeholder="分享复习节奏、资料来源、作业和考试的关系等"
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-checkbox-group v-model="form.tags">
            <el-checkbox v-for="tag in availableTags" :key="tag.id" :label="tag.id">
              {{ tag.tagName }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交评价
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { reviewApi } from '@/api/review'
import { courseApi } from '@/api/course'
import { tagApi } from '@/api/tag'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const instanceId = Number(route.params.instanceId)
const formRef = ref(null)
const submitting = ref(false)
const availableTags = ref([])
const courseInfo = ref(null)

const form = reactive({
  courseInstanceId: instanceId,
  gradingScore: 0,
  teachingScore: 0,
  workloadScore: 0,
  content: '',
  studyTips: '',
  examType: '',
  keyChapters: '',
  cheatSheetAllowed: null,
  tags: []
})

const rules = {
  gradingScore: [{ required: true, trigger: 'change', validator: validateScore }],
  teachingScore: [{ required: true, trigger: 'change', validator: validateScore }],
  workloadScore: [{ required: true, trigger: 'change', validator: validateScore }],
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
}

function validateScore(_, value, callback) {
  if (value > 0) {
    callback()
    return
  }
  callback(new Error('请评分'))
}

async function loadData() {
  try {
    const [courseRes, tagRes] = await Promise.all([
      courseApi.getInstanceDetail(instanceId),
      tagApi.getAll()
    ])
    courseInfo.value = courseRes.data
    availableTags.value = tagRes.data || []
  } catch (error) {
    console.error(error)
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await reviewApi.publish({ ...form })
    ElMessage.success('评价提交成功，等待审核')
    router.push(`/course/instance/${instanceId}`)
  } catch (error) {
    // Global interceptor handles request errors.
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.post-review-container {
  max-width: 760px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 18px;
  font-weight: 600;
}

.course-summary {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #f5f8ff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-meta {
  color: #666;
  font-size: 14px;
}
</style>
