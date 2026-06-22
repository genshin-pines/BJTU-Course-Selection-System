<template>
  <article class="review-card">
    <!-- 左侧评分竖条 -->
    <div class="review-rail" :style="{ background: railGradient }">
      <div class="rail-score">{{ overallScore }}</div>
      <div class="rail-lbl">评分</div>
    </div>

    <div class="review-main">
      <!-- 头部 -->
      <header class="review-head">
        <div class="reviewer">
          <span class="avatar">{{ avatarChar }}</span>
          <div class="reviewer-info">
            <span class="reviewer-name">{{ review.displayName || review.anonymousId || '交大学子' }}</span>
            <span class="review-time">{{ formatTime(review.createTime) }}</span>
          </div>
        </div>
        <div class="score-chips">
          <span class="chip" style="background:#eef2ff;color:#4f46e5">给分 {{ review.gradingScore }}</span>
          <span class="chip" style="background:#ecfeff;color:#0891b2">授课 {{ review.teachingScore }}</span>
          <span class="chip" style="background:#fef3c7;color:#b45309">作业 {{ review.workloadScore }}</span>
        </div>
      </header>

      <!-- 内容 -->
      <div class="review-content">{{ review.content }}</div>

      <!-- 标签 -->
      <div class="review-tags" v-if="review.tags?.length">
        <span v-for="(tag, i) in review.tags" :key="tag.id" class="rtag" :style="tagStyle(i)">
          # {{ tag.tagName }}
        </span>
      </div>

      <!-- 补充信息 -->
      <div class="review-extras" v-if="hasExtras">
        <div class="extra" v-if="review.examType">
          <span class="extra-key"><el-icon><Tickets /></el-icon>考核方式</span>
          <span class="extra-val">{{ review.examType }}</span>
        </div>
        <div class="extra" v-if="review.keyChapters">
          <span class="extra-key"><el-icon><Collection /></el-icon>重点章节</span>
          <span class="extra-val">{{ review.keyChapters }}</span>
        </div>
        <div class="extra" v-if="review.cheatSheetAllowed !== null && review.cheatSheetAllowed !== undefined">
          <span class="extra-key"><el-icon><DocumentChecked /></el-icon>可带资料</span>
          <span class="extra-val" :class="review.cheatSheetAllowed ? 'yes' : 'no'">
            {{ review.cheatSheetAllowed ? '可以' : '不可以' }}
          </span>
        </div>
        <div class="extra" v-if="review.studyTips">
          <span class="extra-key"><el-icon><MagicStick /></el-icon>复习建议</span>
          <span class="extra-val">{{ review.studyTips }}</span>
        </div>
      </div>

      <!-- 操作栏 -->
      <footer class="review-foot">
        <VoteButtons
          v-if="showVote"
          :review="review"
          :voting="voting"
          @like="$emit('like')"
          @downvote="$emit('downvote')"
        />
        <div class="foot-right">
          <button v-if="review.isOwner" class="edit-btn" @click="$emit('edit')">
            <el-icon><EditPen /></el-icon>修改
          </button>
          <el-button v-if="showReport" text type="danger" size="small" @click="$emit('report')">
            <el-icon><Warning /></el-icon>举报
          </el-button>
        </div>
      </footer>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { Warning, EditPen, Tickets, Collection, DocumentChecked, MagicStick } from '@element-plus/icons-vue'
import VoteButtons from './VoteButtons.vue'

const props = defineProps({
  review: { type: Object, required: true },
  voting: { type: Boolean, default: false },
  showVote: { type: Boolean, default: true },
  showReport: { type: Boolean, default: true }
})

defineEmits(['like', 'downvote', 'report', 'edit'])

const overallScore = computed(() => {
  const r = props.review
  const vals = [r.gradingScore, r.teachingScore, r.workloadScore].filter(v => v !== null && v !== undefined)
  if (!vals.length) return '-'
  return (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
})

const railGradient = computed(() => {
  const s = Number(overallScore.value)
  if (isNaN(s)) return 'linear-gradient(180deg, #cbd5e1, #94a3b8)'
  if (s >= 4) return 'linear-gradient(180deg, #34d399, #10b981)'
  if (s >= 3.5) return 'linear-gradient(180deg, #60a5fa, #3b82f6)'
  if (s >= 3) return 'linear-gradient(180deg, #fbbf24, #f59e0b)'
  if (s >= 2) return 'linear-gradient(180deg, #fb923c, #f97316)'
  return 'linear-gradient(180deg, #f87171, #ef4444)'
})

const avatarChar = computed(() => {
  const id = props.review.displayName || props.review.anonymousId || '?'
  const m = id.match(/(\d+)$/)
  return m ? m[1] : '?'
})

const hasExtras = computed(() =>
  props.review.examType || props.review.keyChapters ||
  props.review.studyTips ||
  (props.review.cheatSheetAllowed !== null && props.review.cheatSheetAllowed !== undefined)
)

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const tagPalette = [
  { bg: '#eef2ff', fg: '#4f46e5' },
  { bg: '#ecfeff', fg: '#0891b2' },
  { bg: '#fef3c7', fg: '#b45309' },
  { bg: '#f0fdf4', fg: '#16a34a' },
  { bg: '#fdf2f8', fg: '#be185d' },
  { bg: '#fff7ed', fg: '#c2410c' }
]
function tagStyle(i) {
  const c = tagPalette[i % tagPalette.length]
  return { background: c.bg, color: c.fg }
}
</script>

<style scoped>
.review-card {
  display: flex;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--shadow-xs);
  transition: box-shadow 0.25s, transform 0.25s;
}
.review-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

/* —— 左侧竖条 —— */
.review-rail {
  flex-shrink: 0;
  width: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  gap: 2px;
}
.rail-score {
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}
.rail-lbl {
  font-size: 10px;
  opacity: 0.9;
  letter-spacing: 1px;
}

/* —— 主体 —— */
.review-main {
  flex: 1;
  min-width: 0;
  padding: 18px 22px;
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.reviewer {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 15px;
  flex-shrink: 0;
}
.reviewer-name {
  font-weight: 600;
  color: var(--text-1);
  font-size: 14px;
}
.review-time {
  display: block;
  font-size: 12px;
  color: var(--text-3);
  margin-top: 1px;
}
.score-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.chip {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--r-pill);
}

.review-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-1);
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.review-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.rtag {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--r-pill);
}

.review-extras {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
  padding: 12px 14px;
  background: var(--bg-page);
  border-radius: var(--r-md);
  margin-bottom: 14px;
}
.extra {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  min-width: 0;
}
.extra-key {
  color: var(--text-3);
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.extra-val {
  color: var(--text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.extra-val.yes { color: var(--c-success); font-weight: 600; }
.extra-val.no { color: var(--c-danger); font-weight: 600; }

.review-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-soft);
}
.foot-right {
  display: flex;
  gap: 4px;
}

.edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--border-mid);
  background: #fff;
  color: var(--text-1);
  font-size: 13px;
  padding: 5px 14px;
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.edit-btn:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
  box-shadow: var(--shadow-xs);
}

@media (max-width: 640px) {
  .review-extras {
    grid-template-columns: 1fr;
  }
  .review-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
