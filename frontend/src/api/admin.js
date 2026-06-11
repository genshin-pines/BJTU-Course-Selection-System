import request from '@/utils/request'

export const adminApi = {
  getPendingReviews() {
    return request.get('/admin/reviews/pending')
  },
  approveReview(id, reason) {
    return request.put(`/admin/reviews/${id}/approve`, { reason })
  },
  rejectReview(id, reason) {
    return request.put(`/admin/reviews/${id}/reject`, { reason })
  },
  deleteReview(id, reason) {
    return request.delete(`/admin/reviews/${id}`, { data: { reason } })
  },
  getPendingReports() {
    return request.get('/admin/reports/pending')
  },
  getAllReports() {
    return request.get('/admin/reports')
  },
  resolveReport(id, reason) {
    return request.put(`/admin/reports/${id}/resolve`, { reason })
  },
  dismissReport(id, reason) {
    return request.put(`/admin/reports/${id}/dismiss`, { reason })
  },
  getAuditLogs() {
    return request.get('/admin/audit-logs')
  },
  createTag(tagName) {
    return request.post('/admin/tags', null, { params: { tagName } })
  },
  deleteTag(id) {
    return request.delete(`/admin/tags/${id}`)
  }
}
