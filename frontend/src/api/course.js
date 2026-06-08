import request from '@/utils/request'

export const courseApi = {
  search(params) {
    return request.get('/course/search', { params })
  },
  getDetail(id) {
    return request.get(`/course/${id}`)
  }
}
