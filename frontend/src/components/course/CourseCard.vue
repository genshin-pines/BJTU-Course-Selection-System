<template>
  <article class="course-card" @click="$emit('click')">
    <!-- 左侧评分圆环 -->
    <div class="score-ring" :style="ringStyle">
      <div class="ring-inner">
        <span class="ring-num">{{ formatScore(course.avgScore) }}</span>
        <span class="ring-lbl">综合</span>
      </div>
    </div>

    <!-- 中间课程信息 -->
    <div class="card-body">
      <div class="body-head">
        <h3 class="course-name">{{ course.courseName }}</h3>
        <span class="course-code">{{ course.courseCode }}</span>
      </div>
      <div class="body-meta">
        <span class="meta-item">
          <el-icon><Avatar /></el-icon>{{ course.teacherName || '待定教师' }}
        </span>
        <span class="meta-sep">·</span>
        <span class="meta-item">{{ course.department }}</span>
        <span class="meta-sep">·</span>
        <span class="meta-item">{{ course.credit }} 学分</span>
      </div>

      <!-- 维度评分条 -->
      <div class="dim-scores">
        <div class="dim">
          <span class="dim-label">给分</span>
          <div class="dim-bar"><div class="dim-fill" :style="fillStyle(course.gradingScore)"></div></div>
          <span class="dim-val" :style="{ color: scoreColor(course.gradingScore) }">{{ formatScore(course.gradingScore) }}</span>
        </div>
        <div class="dim">
          <span class="dim-label">授课</span>
          <div class="dim-bar"><div class="dim-fill" :style="fillStyle(course.avgTeachingScore)"></div></div>
          <span class="dim-val" :style="{ color: scoreColor(course.avgTeachingScore) }">{{ formatScore(course.avgTeachingScore) }}</span>
        </div>
        <div class="dim">
          <span class="dim-label">作业</span>
          <div class="dim-bar"><div class="dim-fill" :style="fillStyle(course.avgWorkloadScore)"></div></div>
          <span class="dim-val" :style="{ color: scoreColor(course.avgWorkloadScore) }">{{ formatScore(course.avgWorkloadScore) }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧辅助信息 -->
    <div class="card-side">
      <div class="review-count">
        <el-icon><ChatDotRound /></el-icon>
        <span class="rc-num">{{ course.reviewCount || 0 }}</span>
        <span class="rc-lbl">条评价</span>
      </div>
      <div class="tag-cloud" v-if="course.topTags?.length">
        <span v-for="(tag, i) in course.topTags.slice(0, 3)" :key="tag" class="mini-tag" :style="tagStyle(i)">
          {{ tag }}
        </span>
      </div>
      <div class="go-arrow">
        <el-icon><ArrowRightBold /></el-icon>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { Avatar, ChatDotRound, ArrowRightBold } from '@element-plus/icons-vue'

const props = defineProps({
  course: { type: Object, required: true }
})
defineEmits(['click'])

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
  const score = props.course.avgScore
  const color = scoreColor(score)
  const pct = score ? (score / 5) * 100 : 0
  return {
    background: `conic-gradient(${color} ${pct * 3.6}deg, #eef0f6 0deg)`
  }
})

function fillStyle(score) {
  const pct = score ? (score / 5) * 100 : 0
  return { width: pct + '%', background: scoreColor(score) }
}

const tagPalette = [
  { bg: '#eef2ff', fg: '#4f46e5' },
  { bg: '#ecfeff', fg: '#0891b2' },
  { bg: '#fef3c7', fg: '#b45309' },
  { bg: '#f0fdf4', fg: '#16a34a' },
  { bg: '#fdf2f8', fg: '#be185d' }
]
function tagStyle(i) {
  const c = tagPalette[i % tagPalette.length]
  return { background: c.bg, color: c.fg }
}
</script>

<style scoped>
.course-card {
  display: flex;
  align-items: stretch;
  gap: 20px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--r-lg);
  padding: 20px 22px;
  cursor: pointer;
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-xs);
}
.course-card:hover {
  border-color: var(--brand-primary-light-5);
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
}

/* —— 评分圆环 —— */
.score-ring {
  flex-shrink: 0;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  align-self: center;
  transition: transform 0.3s;
}
.course-card:hover .score-ring {
  transform: scale(1.05);
}
.ring-inner {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ring-num {
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}
.ring-lbl {
  font-size: 10px;
  color: var(--text-3);
  margin-top: 2px;
}

/* —— 主体 —— */
.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}
.body-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.course-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-1);
  margin: 0;
}
.course-code {
  font-size: 12px;
  font-weight: 600;
  color: var(--brand-primary);
  background: var(--brand-primary-light-9);
  padding: 2px 10px;
  border-radius: var(--r-pill);
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.body-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-2);
  flex-wrap: wrap;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.meta-item .el-icon {
  font-size: 14px;
}
.meta-sep {
  color: var(--text-4);
}

/* —— 维度评分条 —— */
.dim-scores {
  display: flex;
  gap: 18px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.dim {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
}
.dim-label {
  color: var(--text-3);
  flex-shrink: 0;
}
.dim-bar {
  width: 64px;
  height: 5px;
  border-radius: 3px;
  background: #eef0f6;
  overflow: hidden;
}
.dim-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}
.dim-val {
  font-weight: 700;
  font-size: 12px;
}

/* —— 右侧 —— */
.card-side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  padding-left: 16px;
  border-left: 1px dashed var(--border-mid);
}
.review-count {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-2);
  font-size: 13px;
}
.review-count .el-icon {
  color: var(--brand-primary);
  font-size: 15px;
}
.rc-num {
  font-weight: 700;
  color: var(--brand-primary);
}
.rc-lbl {
  color: var(--text-3);
  font-size: 12px;
}
.tag-cloud {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 140px;
}
.mini-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--r-pill);
}
.go-arrow {
  color: var(--text-4);
  font-size: 14px;
  transition: all 0.25s;
}
.course-card:hover .go-arrow {
  color: var(--brand-primary);
  transform: translateX(3px);
}

@media (max-width: 768px) {
  .course-card {
    flex-direction: column;
    gap: 14px;
  }
  .card-side {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding-left: 0;
    border-left: none;
    border-top: 1px dashed var(--border-mid);
    padding-top: 12px;
  }
  .dim-scores {
    gap: 14px;
  }
}
</style>
