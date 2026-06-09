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

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="给分情况" prop="gradingScore">
          <el-rate v-model="form.gradingScore" :max="5" show-score
            :texts="['很差', '较差', '一般', '较好', '很好']" />
        </el-form-item>
        <el-form-item label="授课质量" prop="teachingScore">
          <el-rate v-model="form.teachingScore" :max="5" show-score
            :texts="['很差', '较差', '一般', '较好', '很好']" />
        </el-form-item>
        <el-form-item label="作业轻松度" prop="workloadScore">
          <el-rate v-model="form.workloadScore" :max="5" show-score
            :texts="['很繁重', '较繁重', '适中', '较轻松', '很轻松']" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="分享你的课程体验..." />
        </el-form-item>
        <el-form-item label="考核方式">
          <el-input v-model="form.examType" placeholder="例如：闭卷考试 + 平时作业" />
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

const formRef = ref(null)
const submitting = ref(false)
const availableTags = ref([])

const form = reactive({
  courseId: Number(route.params.courseId),
  teacherId: null,
  gradingScore: 0,
  teachingScore: 0,
  workloadScore: 0,
  content: '',
  studyTips: '',
  examType: '',
  tags: []
})

const rules = {
  gradingScore: [{ required: true, message: '请评分', trigger: 'change', validator: (_, v, cb) => v > 0 ? cb() : cb(new Error('请评分')) }],
  teachingScore: [{ required: true, message: '请评分', trigger: 'change', validator: (_, v, cb) => v > 0 ? cb() : cb(new Error('请评分')) }],
  workloadScore: [{ required: true, message: '请评分', trigger: 'change', validator: (_, v, cb) => v > 0 ? cb() : cb(new Error('请评分')) }],
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
}

async function loadData() {
  try {
    const [courseRes, tagRes] = await Promise.all([
      courseApi.getDetail(route.params.courseId),
      tagApi.getAll()
    ])
    form.teacherId = courseRes.data.teacherId
    availableTags.value = tagRes.data || []
  } catch (e) {
    console.error(e)
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await reviewApi.publish(form)
    ElMessage.success('评价提交成功，等待审核')
    router.push(`/course/${route.params.courseId}`)
  } catch (e) {
    // handled globally
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.post-review-container {
  max-width: 700px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
