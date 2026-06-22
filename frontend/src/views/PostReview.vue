<template>
  <div class="post-review-container">
    <el-card class="post-review-card">
      <template #header>
        <div class="card-header">
          <el-button text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <span>{{ isEditMode ? '修改课程评价' : '发布课程评价' }}</span>
        </div>
      </template>

      <div v-if="courseInfo" class="course-summary">
        <strong>{{ courseInfo.courseName }}</strong>
        <span class="course-meta">{{ courseInfo.courseCode }} · {{ courseInfo.teacherName || '未分配教师' }}</span>
      </div>

      <el-alert
        v-if="isEditMode"
        type="info"
        :closable="false"
        show-icon
        title="修改后将重新进入审核流程，审核通过前评价仍会对其他用户可见。"
        style="margin-bottom: 20px"
      />

      <ReviewForm
        :form="form"
        :tags="availableTags"
        :is-edit-mode="isEditMode"
        :submitting="submitting"
        :loading="loading"
        @submit="onSubmit"
        @cancel="$router.back()"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { reviewApi } from '@/api/review'
import { courseApi } from '@/api/course'
import { tagApi } from '@/api/tag'
import ReviewForm from '@/components/review/ReviewForm.vue'

const route = useRoute()
const router = useRouter()

const editingReviewId = route.params.reviewId ? Number(route.params.reviewId) : null
const instanceId = ref(route.params.instanceId ? Number(route.params.instanceId) : null)

const submitting = ref(false)
const loading = ref(false)
const availableTags = ref([])
const courseInfo = ref(null)

const isEditMode = computed(() => Boolean(editingReviewId))

const form = reactive({
  courseInstanceId: instanceId.value,
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

function applyReviewToForm(review) {
  form.courseInstanceId = review.courseInstanceId || instanceId.value
  form.gradingScore = review.gradingScore || 0
  form.teachingScore = review.teachingScore || 0
  form.workloadScore = review.workloadScore || 0
  form.content = review.content || ''
  form.studyTips = review.studyTips || ''
  form.examType = review.examType || ''
  form.keyChapters = review.keyChapters || ''
  form.cheatSheetAllowed = review.cheatSheetAllowed ?? null
  form.tags = (review.tags || []).map((tag) => tag.id)
}

async function loadData() {
  loading.value = true
  try {
    // 编辑模式：先加载评价数据，从中拿到 instanceId 再加载课程信息
    if (isEditMode.value) {
      const reviewRes = await reviewApi.getMineById(editingReviewId)
      const review = reviewRes.data
      if (review.courseInstanceId) {
        instanceId.value = review.courseInstanceId
        form.courseInstanceId = review.courseInstanceId
      }
      applyReviewToForm(review)
      if (instanceId.value) {
        const courseRes = await courseApi.getInstanceDetail(instanceId.value)
        courseInfo.value = courseRes.data
      }
    } else {
      // 发布模式：从路由拿 instanceId
      const [courseRes, tagRes] = await Promise.all([
        courseApi.getInstanceDetail(instanceId.value),
        tagApi.getAll()
      ])
      courseInfo.value = courseRes.data
      availableTags.value = tagRes.data || []
    }

    if (isEditMode.value) {
      const tagRes = await tagApi.getAll()
      availableTags.value = tagRes.data || []
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载评价信息失败')
    router.back()
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  submitting.value = true
  try {
    const payload = { ...form }
    const res = isEditMode.value
      ? await reviewApi.edit(editingReviewId, payload)
      : await reviewApi.publish(payload)
    if (res.data?.hidden) {
      ElMessage.warning(res.data.message || res.message || '评价包含违禁词，已自动隐藏')
    } else {
      ElMessage.success(res.data?.message || (isEditMode.value ? '评价已更新' : '评价已发布'))
    }
    router.push(`/course/instance/${instanceId.value}`)
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
