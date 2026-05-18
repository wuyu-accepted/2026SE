import http from './http'

export function fetchPartyStageOptions() {
  return http.get('/api/admin/party/stages')
}

export function fetchPartyStepOptions(stageCode) {
  return http.get('/api/admin/party/steps', { params: { stageCode } })
}

export function batchImportPartyProgress(items) {
  return http.post('/api/admin/party/progress/batch-import', { items })
}

export function fetchStudentPartyProgress(params) {
  return http.get('/api/admin/party/progress/student', { params })
}

export function fetchPartyReports(params) {
  return http.get('/api/admin/party/reports', { params })
}

export function fetchPartyReportDetail(id) {
  return http.get(`/api/admin/party/reports/${id}`)
}

export function approvePartyReport(id, comment) {
  return http.post(`/api/admin/party/reports/${id}/approve`, { comment })
}

export function rejectPartyReport(id, comment) {
  return http.post(`/api/admin/party/reports/${id}/reject`, { comment })
}

export function fetchPartyActivities(params) {
  return http.get('/api/admin/party/activities', { params })
}

export function fetchPartyActivityDetail(id) {
  return http.get(`/api/admin/party/activities/${id}`)
}

export function approvePartyActivity(id, comment) {
  return http.post(`/api/admin/party/activities/${id}/approve`, { comment })
}

export function rejectPartyActivity(id, comment) {
  return http.post(`/api/admin/party/activities/${id}/reject`, { comment })
}
