import request from '@/utils/request'

export const courseAppApi = {
  submit(data) {
    return request.post('/course-application', data)
  },
  getMine() {
    return request.get('/course-application/mine')
  },
  // 管理端
  getPending() {
    return request.get('/admin/course-applications/pending')
  },
  approve(id, reason) {
    return request.put(`/admin/course-applications/${id}/approve`, { reason })
  },
  reject(id, reason) {
    return request.put(`/admin/course-applications/${id}/reject`, { reason })
  }
}
