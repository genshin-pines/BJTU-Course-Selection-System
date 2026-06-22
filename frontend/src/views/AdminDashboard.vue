<template>
  <el-container class="admin-layout">
    <AdminSidebar v-model:activeTab="activeTab" />
    <el-main class="admin-main">
      <div class="main-inner">
        <ReviewAuditTab v-if="activeTab === 'reviews' && canGovernContent" />
        <AllReviewsTab v-else-if="activeTab === 'allReviews' && canViewAllReviews" />
        <ReportTab v-else-if="activeTab === 'reports' && canGovernContent" />
        <CourseApplicationTab v-else-if="activeTab === 'courseApps' && canGovernContent" />
        <TagTab v-else-if="activeTab === 'tags' && canMaintainData" />
        <DataTab v-else-if="activeTab === 'dataMaintenance' && canMaintainData" />
        <ImportTab v-else-if="activeTab === 'import' && canMaintainData" />
        <AdminAccountTab v-else-if="activeTab === 'accounts' && canManageAdmins" />
        <AuditLogTab v-else-if="activeTab === 'auditLogs' && canViewAuditLogs" />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import ReviewAuditTab from '@/components/admin/ReviewAuditTab.vue'
import AllReviewsTab from '@/components/admin/AllReviewsTab.vue'
import ReportTab from '@/components/admin/ReportTab.vue'
import CourseApplicationTab from '@/components/admin/CourseApplicationTab.vue'
import TagTab from '@/components/admin/TagTab.vue'
import DataTab from '@/components/admin/DataTab.vue'
import ImportTab from '@/components/admin/ImportTab.vue'
import AdminAccountTab from '@/components/admin/AdminAccountTab.vue'
import AuditLogTab from '@/components/admin/AuditLogTab.vue'

const authStore = useAuthStore()
const activeTab = ref('reviews')

const adminRole = computed(() => authStore.adminRole || 'SUPER_ADMIN')
const canGovernContent = computed(() => ['SUPER_ADMIN', 'AUDITOR'].includes(adminRole.value))
const canViewAllReviews = computed(() => ['SUPER_ADMIN', 'DEPT_OP'].includes(adminRole.value))
const canMaintainData = computed(() => ['SUPER_ADMIN', 'DEPT_OP'].includes(adminRole.value))
const canManageAdmins = computed(() => adminRole.value === 'SUPER_ADMIN')
const canViewAuditLogs = computed(() => ['SUPER_ADMIN', 'DEPT_OP', 'AUDITOR'].includes(adminRole.value))

function firstAccessibleTab() {
  if (canGovernContent.value) return 'reviews'
  if (canViewAllReviews.value) return 'allReviews'
  if (canMaintainData.value) return 'tags'
  if (canManageAdmins.value) return 'accounts'
  if (canViewAuditLogs.value) return 'auditLogs'
  return ''
}

function ensureAccessibleTab() {
  const allowed = {
    reviews: canGovernContent.value,
    allReviews: canViewAllReviews.value,
    reports: canGovernContent.value,
    courseApps: canGovernContent.value,
    tags: canMaintainData.value,
    dataMaintenance: canMaintainData.value,
    import: canMaintainData.value,
    accounts: canManageAdmins.value,
    auditLogs: canViewAuditLogs.value
  }
  if (!allowed[activeTab.value]) {
    activeTab.value = firstAccessibleTab()
  }
}

watch(activeTab, () => { ensureAccessibleTab() })

onMounted(() => {
  authStore.syncFromStorage()
  if (!authStore.isAdmin) {
    ElMessage.warning('请先登录管理员账号')
    return
  }
  ensureAccessibleTab()
})
</script>

<style>
/* admin 通用样式（非 scoped，供各 Tab 子组件复用） */
.admin-layout {
  min-height: calc(100vh - 56px - 56px);
  background: var(--bg-page);
}

.admin-main {
  padding: 0;
  background: var(--bg-page);
}

.main-inner {
  padding: 24px 28px 40px;
  max-width: 100%;
}

.section-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-xs);
}

.search-bar {
  margin-bottom: 16px;
}

.table-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 管理后台表格增强 */
.admin-main .el-table {
  border-radius: var(--r-md) !important;
  overflow: hidden;
  border: 1px solid var(--border-soft) !important;
}
.admin-main .el-table th.el-table__cell {
  background: #f8fafc !important;
  color: var(--text-2);
  font-weight: 600;
}
</style>
