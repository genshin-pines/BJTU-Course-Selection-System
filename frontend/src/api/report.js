import request from '@/utils/request'

export const reportApi = {
  submit(data) {
    return request.post('/report', data)
  }
}
