import http from './http'

export function fetchRoles() {
  return http.get('/api/admin/roles')
}

export function fetchRoleDetail(id) {
  return http.get(`/api/admin/roles/${id}`)
}

export function createRole(data) {
  return http.post('/api/admin/roles', data)
}

export function updateRole(id, data) {
  return http.put(`/api/admin/roles/${id}`, data)
}

export function deleteRole(id) {
  return http.delete(`/api/admin/roles/${id}`)
}

export function fetchAuditLogs(params) {
  return http.get('/api/admin/audit-logs', { params })
}

export function fetchCounselors() {
  return http.get('/api/admin/counselors')
}

export function fetchAiConfigs() {
  return http.get('/api/admin/ai-config')
}

export function createAiConfig(data) {
  return http.post('/api/admin/ai-config', data)
}

export function updateAiConfig(id, data) {
  return http.put(`/api/admin/ai-config/${id}`, data)
}

export function activateAiConfig(id) {
  return http.post(`/api/admin/ai-config/${id}/activate`)
}

export function testAiConfig(id, data) {
  return http.post(`/api/admin/ai-config/${id}/test`, data)
}

export function deleteAiConfig(id) {
  return http.delete(`/api/admin/ai-config/${id}`)
}
