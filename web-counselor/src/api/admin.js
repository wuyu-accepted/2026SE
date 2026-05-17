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
