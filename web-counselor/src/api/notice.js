import http from './http'

export function fetchNotices(params) {
  return http.get('/api/admin/notices', { params })
}

export function fetchNoticeDetail(id) {
  return http.get(`/api/admin/notices/${id}`)
}

export function createNotice(data) {
  return http.post('/api/admin/notices', data)
}

export function updateNotice(id, data) {
  return http.put(`/api/admin/notices/${id}`, data)
}

export function publishNotice(id) {
  return http.post(`/api/admin/notices/${id}/publish`)
}

export function estimateNoticeTarget(id) {
  return http.get(`/api/admin/notices/${id}/target-estimate`)
}

export function fetchNoticeStats(id) {
  return http.get(`/api/admin/notices/${id}/stats`)
}

export function offlineNotice(id) {
  return http.post(`/api/admin/notices/${id}/offline`)
}

export function deleteNotice(id) {
  return http.delete(`/api/admin/notices/${id}`)
}
