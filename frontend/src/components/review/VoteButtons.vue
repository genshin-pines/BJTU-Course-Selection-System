<template>
  <div class="vote-group">
    <button
      class="vote-btn like"
      :class="{ active: review.liked }"
      :disabled="voting"
      @click="$emit('like')"
    >
      <el-icon class="v-icon"><CaretTop /></el-icon>
      <span class="v-count">{{ review.likeCount || 0 }}</span>
      <span class="v-label">有用</span>
    </button>
    <button
      class="vote-btn down"
      :class="{ active: review.downvoted }"
      :disabled="voting"
      @click="$emit('downvote')"
    >
      <el-icon class="v-icon"><CaretBottom /></el-icon>
      <span class="v-count">{{ review.downvoteCount || 0 }}</span>
      <span class="v-label">没用</span>
    </button>
  </div>
</template>

<script setup>
import { CaretBottom, CaretTop } from '@element-plus/icons-vue'

defineProps({
  review: { type: Object, required: true },
  voting: { type: Boolean, default: false }
})
defineEmits(['like', 'downvote'])
</script>

<style scoped>
.vote-group {
  display: inline-flex;
  background: var(--bg-page);
  border-radius: var(--r-pill);
  padding: 3px;
  gap: 2px;
}

.vote-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  padding: 5px 14px;
  border-radius: var(--r-pill);
  cursor: pointer;
  font-size: 13px;
  color: var(--text-2);
  transition: all 0.2s ease;
  font-family: inherit;
}
.vote-btn:hover:not(:disabled) {
  background: #fff;
  box-shadow: var(--shadow-xs);
}
.vote-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.v-icon {
  font-size: 15px;
}

.v-count {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.v-label {
  color: var(--text-3);
  font-size: 12px;
}

/* 激活态 */
.vote-btn.like.active {
  background: var(--brand-primary);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
}
.vote-btn.like.active .v-label {
  color: rgba(255,255,255,0.85);
}

.vote-btn.down.active {
  background: var(--c-danger);
  color: #fff;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.35);
}
.vote-btn.down.active .v-label {
  color: rgba(255,255,255,0.85);
}
</style>
