import request from '@/utils/request'

export const reviewApi = {
  getByCourse(courseId) {
    return request.get(`/review/course/${courseId}`)
  },
  publish(data) {
    return request.post('/review', data)
  },
  edit(id, data) {
    return request.put(`/review/${id}`, data)
  },
  delete(id) {
    return request.delete(`/review/${id}`)
  },
  like(id) {
    return request.post(`/review/like/${id}`)
  }
}
