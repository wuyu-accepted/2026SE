import http from './http'

export function fetchCounselorFeedbacks(params) {
  return http.get('/api/notice-feedback/counselor/pending', { params })
}

export function fetchCounselorFeedbackDetail(id) {
  return http.get(`/api/notice-feedback/counselor/${id}`)
}

export function replyCounselorFeedback(id, content) {
  return http.post(`/api/notice-feedback/${id}/counselor-reply`, { content })
}

export function fetchCounselorFeedbackCount() {
  return http.get('/api/notice-feedback/counselor/pending-count')
}
