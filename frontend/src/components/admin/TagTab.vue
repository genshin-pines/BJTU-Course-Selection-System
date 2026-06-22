<template>
  <div>
    <div class="section-toolbar">
      <el-input
        v-model="newTagName"
        placeholder="输入新标签名"
        style="width: 240px"
        @keyup.enter="handleCreateTag"
      />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { tagApi } from '@/api/tag'

const tags = ref([])
const newTagName = ref('')

async function loadTags() {
  const res = await tagApi.getAll()
  tags.value = res.data || []
}

async function handleCreateTag() {
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
  await adminApi.deleteTag(id)
  ElMessage.success('标签已删除')
  await loadTags()
}

onMounted(loadTags)
</script>

<style scoped>
.tag-list {
  margin-top: 16px;
}

.tag-list .el-tag {
  margin: 4px;
}
</style>
