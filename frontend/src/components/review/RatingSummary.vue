<template>
  <div class="rating-summary">
    <!-- 左侧大圆环综合评分 -->
    <div class="big-ring-wrap">
      <div class="big-ring" :style="ringStyle">
        <div class="ring-hole">
          <span class="ring-score" :style="{ color: scoreColor(course?.avgScore) }">{{ formatScore(course?.avgScore) }}</span>
          <span class="ring-max">/ 5.0</span>
        </div>
      </div>
      <div class="ring-caption">
        <span class="caption-label">综合评分</span>
        <span class="caption-count">{{ course?.reviewCount || 0 }} 条评价</span>
      </div>
    </div>

    <!-- 右侧三维度 -->
    <div class="dim-list">
      <div class="dim-row">
        <div class="dim-head">
          <span class="dim-icon" style="background:#dbeafe;color:#2563eb"><el-icon><Stamp /></el-icon></span>
          <span class="dim-name">给分情况</span>
          <span class="dim-score" :style="{ color: scoreColor(course?.gradingScore) }">{{ formatScore(course?.gradingScore) }}</span>
        </div>
        <div class="dim-track"><div class="dim-fill" :style="fillStyle(course?.gradingScore)"></div></div>
      </div>
      <div class="dim-row">
        <div class="dim-head">
          <span class="dim-icon" style="background:#e0f2fe;color:#0284c7"><el-icon><Reading /></el-icon></span>
          <span class="dim-name">授课质量</span>
          <span class="dim-score" :style="{ color: scoreColor(course?.avgTeachingScore) }">{{ formatScore(course?.avgTeachingScore) }}</span>
        </div>
        <div class="dim-track"><div class="dim-fill" :style="fillStyle(course?.avgTeachingScore)"></div></div>
      </div>
      <div class="dim-row">
        <div class="dim-head">
          <span class="dim-icon" style="background:#fef3c7;color:#d97706"><el-icon><Notebook /></el-icon></span>
          <span class="dim-name">作业轻松度</span>
          <span class="dim-score" :style="{ color: scoreColor(course?.avgWorkloadScore) }">{{ formatScore(course?.avgWorkloadScore) }}</span>
        </div>
        <div class="dim-track"><div class="dim-fill" :style="fillStyle(course?.avgWorkloadScore)"></div></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Stamp, Reading, Notebook } from '@element-plus/icons-vue'

const props = defineProps({
  course: { type: Object, default: null }
})

function formatScore(score) {
  return score === null || score === undefined ? '-' : Number(score).toFixed(1)
}

function scoreColor(score) {
  if (score === null || score === undefined) return 'var(--text-4)'
  if (score >= 4) return 'var(--score-5)'
  if (score >= 3.5) return 'var(--score-4)'
  if (score >= 3) return 'var(--score-3)'
  if (score >= 2) return 'var(--score-2)'
  return 'var(--score-1)'
}

const ringStyle = computed(() => {
  const score = props.course?.avgScore
  const color = scoreColor(score)
  const pct = score ? (score / 5) * 100 : 0
  return { background: `conic-gradient(${color} ${pct * 3.6}deg, #eef0f6 0deg)` }
})

function fillStyle(score) {
  const pct = score ? (score / 5) * 100 : 0
  return { width: pct + '%', background: scoreColor(score) }
}
</script>

<style scoped>
.rating-summary {
  display: flex;
  align-items: center;
  gap: 36px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--r-lg);
  padding: 24px 28px;
  box-shadow: var(--shadow-sm);
}

/* —— 大圆环 —— */
.big-ring-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.big-ring {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s;
}
.rating-summary:hover .big-ring {
  transform: scale(1.04);
}
.ring-hole {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ring-score {
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
}
.ring-max {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 2px;
}
.ring-caption {
  text-align: center;
}
.caption-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
}
.caption-count {
  font-size: 11px;
  color: var(--text-3);
}

/* —— 维度列表 —— */
.dim-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.dim-row {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.dim-head {
  display: flex;
  align-items: center;
  gap: 9px;
}
.dim-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}
.dim-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-2);
}
.dim-score {
  margin-left: auto;
  font-size: 18px;
  font-weight: 800;
}
.dim-track {
  height: 8px;
  border-radius: 5px;
  background: #f0f1f6;
  overflow: hidden;
}
.dim-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

@media (max-width: 640px) {
  .rating-summary {
    flex-direction: column;
    gap: 24px;
  }
}
</style>
