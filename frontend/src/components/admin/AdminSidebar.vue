<template>
  <el-aside width="220px" class="admin-sidebar">
    <!-- 顶部品牌 -->
    <div class="side-brand">
      <span class="brand-mark"><el-icon><Platform /></el-icon></span>
      <div class="brand-text">
        <span class="brand-name">管理后台</span>
        <span class="brand-sub">Admin Console</span>
      </div>
    </div>

    <!-- 角色徽章 -->
    <div class="role-badge" :class="roleClass">
      <el-icon><UserFilled /></el-icon>
      <div class="role-info">
        <span class="role-label">当前角色</span>
        <span class="role-name">{{ adminRoleText }}</span>
      </div>
    </div>

    <!-- 菜单 -->
    <nav class="side-nav">
      <div class="nav-group-label">内容管理</div>
      <button class="nav-item" v-if="canGovernContent" :class="{ active: activeTab === 'reviews' }" @click="onSelect('reviews')">
        <el-icon><Checked /></el-icon><span>评价审核</span>
        <span class="nav-dot" v-if="activeTab === 'reviews'"></span>
      </button>
      <button class="nav-item" v-if="canViewAllReviews" :class="{ active: activeTab === 'allReviews' }" @click="onSelect('allReviews')">
        <el-icon><Document /></el-icon><span>全部评价</span>
        <span class="nav-dot" v-if="activeTab === 'allReviews'"></span>
      </button>
      <button class="nav-item" v-if="canGovernContent" :class="{ active: activeTab === 'reports' }" @click="onSelect('reports')">
        <el-icon><Warning /></el-icon><span>举报管理</span>
        <span class="nav-dot" v-if="activeTab === 'reports'"></span>
      </button>
      <button class="nav-item" v-if="canGovernContent" :class="{ active: activeTab === 'courseApps' }" @click="onSelect('courseApps')">
        <el-icon><Files /></el-icon><span>课程申请</span>
        <span class="nav-dot" v-if="activeTab === 'courseApps'"></span>
      </button>

      <div class="nav-group-label" v-if="canMaintainData">数据维护</div>
      <button class="nav-item" v-if="canMaintainData" :class="{ active: activeTab === 'tags' }" @click="onSelect('tags')">
        <el-icon><PriceTag /></el-icon><span>标签管理</span>
        <span class="nav-dot" v-if="activeTab === 'tags'"></span>
      </button>
      <button class="nav-item" v-if="canMaintainData" :class="{ active: activeTab === 'dataMaintenance' }" @click="onSelect('dataMaintenance')">
        <el-icon><DataBoard /></el-icon><span>基础数据</span>
        <span class="nav-dot" v-if="activeTab === 'dataMaintenance'"></span>
      </button>
      <button class="nav-item" v-if="canMaintainData" :class="{ active: activeTab === 'import' }" @click="onSelect('import')">
        <el-icon><Upload /></el-icon><span>数据导入</span>
        <span class="nav-dot" v-if="activeTab === 'import'"></span>
      </button>

      <div class="nav-group-label" v-if="canManageAdmins || canViewAuditLogs">系统</div>
      <button class="nav-item" v-if="canManageAdmins" :class="{ active: activeTab === 'accounts' }" @click="onSelect('accounts')">
        <el-icon><UserFilled /></el-icon><span>管理员管理</span>
        <span class="nav-dot" v-if="activeTab === 'accounts'"></span>
      </button>
      <button class="nav-item" v-if="canViewAuditLogs" :class="{ active: activeTab === 'auditLogs' }" @click="onSelect('auditLogs')">
        <el-icon><Clock /></el-icon><span>审核日志</span>
        <span class="nav-dot" v-if="activeTab === 'auditLogs'"></span>
      </button>
    </nav>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { Checked, Clock, DataBoard, Document, Files, PriceTag, Upload, UserFilled, Warning, Platform } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  activeTab: { type: String, default: '' }
})
const emit = defineEmits(['update:activeTab'])

const authStore = useAuthStore()

const adminRole = computed(() => authStore.adminRole || 'SUPER_ADMIN')
const canGovernContent = computed(() => ['SUPER_ADMIN', 'AUDITOR'].includes(adminRole.value))
const canViewAllReviews = computed(() => ['SUPER_ADMIN', 'DEPT_OP'].includes(adminRole.value))
const canMaintainData = computed(() => ['SUPER_ADMIN', 'DEPT_OP'].includes(adminRole.value))
const canManageAdmins = computed(() => adminRole.value === 'SUPER_ADMIN')
const canViewAuditLogs = computed(() => ['SUPER_ADMIN', 'DEPT_OP', 'AUDITOR'].includes(adminRole.value))

const adminRoleText = computed(() => roleText(adminRole.value))
const roleClass = computed(() => ({
  SUPER_ADMIN: 'role-super',
  DEPT_OP: 'role-dept',
  AUDITOR: 'role-auditor'
}[adminRole.value] || ''))

function roleText(role) {
  return { SUPER_ADMIN: '超级管理员', DEPT_OP: '院系维护员', AUDITOR: '内容审核员' }[role] || role || '-'
}

function onSelect(index) {
  emit('update:activeTab', index)
}
</script>

<style scoped>
.admin-sidebar {
  background: linear-gradient(180deg, #0f172a 0%, #0f172a 30%, #1e3a8a 100%);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow-y: auto;
}

.side-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 18px;
}
.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: var(--brand-gradient-vibrant);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}
.brand-name {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}
.brand-sub {
  color: rgba(255, 255, 255, 0.45);
  font-size: 10px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.role-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 4px 16px 18px;
  padding: 10px 12px;
  border-radius: var(--r-md);
  font-size: 16px;
  color: #fff;
}
.role-badge.role-super {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.2), rgba(245, 158, 11, 0.1));
  border: 1px solid rgba(251, 191, 36, 0.35);
}
.role-badge.role-dept {
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.18), rgba(14, 165, 233, 0.1));
  border: 1px solid rgba(56, 189, 248, 0.3);
}
.role-badge.role-auditor {
  background: linear-gradient(135deg, rgba(167, 139, 250, 0.18), rgba(139, 92, 246, 0.1));
  border: 1px solid rgba(167, 139, 250, 0.3);
}
.role-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.role-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.5px;
}
.role-name {
  font-size: 13px;
  font-weight: 600;
}

.side-nav {
  flex: 1;
  padding: 0 12px 24px;
}

.nav-group-label {
  color: rgba(255, 255, 255, 0.35);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  padding: 16px 12px 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
  padding: 10px 12px;
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  font-family: inherit;
  position: relative;
}
.nav-item .el-icon {
  font-size: 17px;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.nav-item.active {
  background: var(--brand-gradient-vibrant);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.4);
}
.nav-dot {
  margin-left: auto;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
}

/* 滚动条 */
.admin-sidebar::-webkit-scrollbar {
  width: 5px;
}
.admin-sidebar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 5px;
}
</style>
