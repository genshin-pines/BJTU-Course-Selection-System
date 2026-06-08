import request from '@/utils/request'

export const tagApi = {
  getAll() {
    return request.get('/tag')
  }
}
