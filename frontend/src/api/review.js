import request from '@/utils/request'

export const reviewApi = {
  getByCourse(courseId, courseInstanceId, sortBy, tagIds = []) {
    return request.get(`/review/course/${courseId}`, {
      params: {
        ...(courseInstanceId ? { courseInstanceId } : {}),
        ...(sortBy ? { sortBy } : {}),
        ...(tagIds.length ? { tagIds: tagIds.join(',') } : {})
      }
    })
  },
  getLikedByCourse(courseId, courseInstanceId) {
    return request.get(`/review/course/${courseId}/liked`, {
      params: courseInstanceId ? { courseInstanceId } : {}
    })
  },
  getDownvotedByCourse(courseId, courseInstanceId) {
    return request.get(`/review/course/${courseId}/downvoted`, {
      params: courseInstanceId ? { courseInstanceId } : {}
    })
  },
  getByInstance(instanceId, sortBy, tagIds = []) {
    return request.get(`/review/instance/${instanceId}`, {
      params: {
        ...(sortBy ? { sortBy } : {}),
        ...(tagIds.length ? { tagIds: tagIds.join(',') } : {})
      }
    })
  },
  getLikedByInstance(instanceId) {
    return request.get(`/review/instance/${instanceId}/liked`)
  },
  getDownvotedByInstance(instanceId) {
    return request.get(`/review/instance/${instanceId}/downvoted`)
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
    return request.post(`/review/like/${id}`, null, { silentError: true })
  },
  downvote(id) {
    return request.post(`/review/downvote/${id}`, null, { silentError: true })
  }
}
