import request from '@/utils/request'

export const adminApi = {
  getPendingReviews() {
    return request.get('/admin/reviews/pending')
  },
  approveReview(id) {
    return request.put(`/admin/reviews/${id}/approve`)
  },
  rejectReview(id) {
    return request.put(`/admin/reviews/${id}/reject`)
  },
  deleteReview(id) {
    return request.delete(`/admin/reviews/${id}`)
  },
  getPendingReports() {
    return request.get('/admin/reports/pending')
  },
  getAllReports() {
    return request.get('/admin/reports')
  },
  resolveReport(id) {
    return request.put(`/admin/reports/${id}/resolve`)
  },
  dismissReport(id) {
    return request.put(`/admin/reports/${id}/dismiss`)
  },
  createTag(tagName) {
    return request.post('/admin/tags', null, { params: { tagName } })
  },
  deleteTag(id) {
    return request.delete(`/admin/tags/${id}`)
  }
}
