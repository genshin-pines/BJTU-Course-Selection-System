<template>
  <div class="review-toolbar">
    <div class="toolbar-left">
      <h2 class="toolbar-title">
        <span class="title-bar"></span>
        课程评价
        <span class="title-count" v-if="count">({{ count }})</span>
      </h2>
    </div>
    <div class="toolbar-right">
      <el-select
        :model-value="selectedTagIds"
        size="default"
        placeholder="按标签筛选"
        multiple
        collapse-tags
        collapse-tags-tooltip
        clearable
        class="tag-select"
        @update:model-value="(v) => $emit('update:selectedTagIds', v)"
        @change="$emit('change')"
      >
        <el-option v-for="tag in tags" :key="tag.id" :label="tag.tagName" :value="tag.id" />
      </el-select>
      <el-select
        :model-value="sortBy"
        size="default"
        class="sort-select"
        @update:model-value="(v) => $emit('update:sortBy', v)"
        @change="$emit('change')"
      >
        <template #prefix>
          <el-icon><Sort /></el-icon>
        </template>
        <el-option label="质量优先" value="quality" />
        <el-option label="最新优先" value="latest" />
        <el-option label="高分优先" value="highScore" />
        <el-option label="有用优先" value="useful" />
        <el-option label="争议优先" value="controversial" />
      </el-select>
    </div>
  </div>
</template>

<script setup>
import { Sort } from '@element-plus/icons-vue'

defineProps({
  sortBy: { type: String, default: 'quality' },
  selectedTagIds: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  count: { type: Number, default: 0 }
})

defineEmits(['update:sortBy', 'update:selectedTagIds', 'change'])
</script>

<style scoped>
.review-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-xs);
}

.toolbar-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}
.title-bar {
  width: 4px;
  height: 18px;
  border-radius: 3px;
  background: var(--brand-gradient);
}
.title-count {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-3);
}

.toolbar-right {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.tag-select {
  width: 200px;
}
.sort-select {
  width: 150px;
}

@media (max-width: 640px) {
  .review-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-right {
    width: 100%;
  }
  .tag-select,
  .sort-select {
    width: 100%;
  }
}
</style>
