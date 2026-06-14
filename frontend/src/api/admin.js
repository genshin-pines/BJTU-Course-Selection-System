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
  },
  getAdminAccounts() {
    return request.get('/admin/accounts')
  },
  createAdminAccount(data) {
    return request.post('/admin/accounts', data)
  },
  updateAdminRole(id, role, department = '') {
    return request.put(`/admin/accounts/${id}/role`, { role, department })
  },
  resetAdminPassword(id, password) {
    return request.put(`/admin/accounts/${id}/password`, { password })
  },
  deleteAdminAccount(id) {
    return request.delete(`/admin/accounts/${id}`)
  },
  getAdminCourses() {
    return request.get('/admin/courses')
  },
  createAdminCourse(data) {
    return request.post('/admin/courses', data)
  },
  updateAdminCourse(id, data) {
    return request.put(`/admin/courses/${id}`, data)
  },
  deleteAdminCourse(id) {
    return request.delete(`/admin/courses/${id}`)
  },
  getAdminTeachers() {
    return request.get('/admin/teachers')
  },
  createAdminTeacher(data) {
    return request.post('/admin/teachers', data)
  },
  updateAdminTeacher(id, data) {
    return request.put(`/admin/teachers/${id}`, data)
  },
  deleteAdminTeacher(id) {
    return request.delete(`/admin/teachers/${id}`)
  },
  getAdminCourseInstances(courseBaseId) {
    return request.get('/admin/course-instances', {
      params: courseBaseId ? { courseBaseId } : {}
    })
  },
  createAdminCourseInstance(data) {
    return request.post('/admin/course-instances', data)
  },
  updateAdminCourseInstance(id, data) {
    return request.put(`/admin/course-instances/${id}`, data)
  },
  deleteAdminCourseInstance(id) {
    return request.delete(`/admin/course-instances/${id}`)
  }
}
