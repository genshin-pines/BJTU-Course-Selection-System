<template>
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
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'

const uploadFile = ref(null)
const fileList = ref([])
const importLoading = ref(false)
const importResult = ref(null)

function isCsvFile(file) {
  const name = file?.name || file?.raw?.name || ''
  return name.toLowerCase().endsWith('.csv')
}

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

async function handleImport() {
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
  } catch (e) {
    ElMessage.error('导入失败：' + (e.response?.data?.message || e.message || '未知错误'))
  } finally {
    importLoading.value = false
  }
}

function downloadTemplate() {
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
</script>

<style scoped>
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
