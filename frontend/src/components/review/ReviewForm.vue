<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
    <el-form-item label="给分情况" prop="gradingScore">
      <el-rate v-model="form.gradingScore" :max="5" show-score :texts="['很差', '较差', '一般', '较好', '很好']" />
    </el-form-item>

    <el-form-item label="授课质量" prop="teachingScore">
      <el-rate v-model="form.teachingScore" :max="5" show-score :texts="['很差', '较差', '一般', '较好', '很好']" />
    </el-form-item>

    <el-form-item label="作业轻松度" prop="workloadScore">
      <el-rate v-model="form.workloadScore" :max="5" show-score :texts="['很繁重', '较繁重', '适中', '较轻松', '很轻松']" />
    </el-form-item>

    <el-form-item label="评价内容" prop="content">
      <el-input v-model="form.content" type="textarea" :rows="6" placeholder="分享你的课程体验、老师风格、作业强度等..." />
    </el-form-item>

    <el-form-item label="考核方式">
      <el-input v-model="form.examType" placeholder="例如：闭卷考试 + 平时作业" />
    </el-form-item>

    <el-form-item label="重点章节">
      <el-input v-model="form.keyChapters" type="textarea" :rows="2" placeholder="例如：第 3、5、8 章，老师上课反复强调的案例题" />
    </el-form-item>

    <el-form-item label="可带资料">
      <el-radio-group v-model="form.cheatSheetAllowed">
        <el-radio :label="true">可以</el-radio>
        <el-radio :label="false">不可以</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="复习建议">
      <el-input v-model="form.studyTips" type="textarea" :rows="3" placeholder="分享复习节奏、资料来源、作业和考试的关系等" />
    </el-form-item>

    <el-form-item label="标签">
      <el-checkbox-group v-model="form.tags">
        <el-checkbox v-for="tag in tags" :key="tag.id" :label="tag.id">
          {{ tag.tagName }}
        </el-checkbox>
      </el-checkbox-group>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEditMode ? '保存修改' : '提交评价' }}
      </el-button>
      <el-button @click="$emit('cancel')">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  form: { type: Object, required: true },
  tags: { type: Array, default: () => [] },
  isEditMode: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['submit', 'cancel'])

const formRef = ref(null)

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

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  emit('submit')
}
</script>
