import http from './http'

export function fetchStudents(params) {
  return http.get('/api/admin/students', { params })
}
