import request from '@/utils/request'

export const courseApi = {
  search(params) {
    return request.get('/course/search', { params })
  },
  getInstanceDetail(instanceId) {
    return request.get(`/course/instances/${instanceId}/detail`)
  }
}
