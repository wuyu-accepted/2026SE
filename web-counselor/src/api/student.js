import axios from 'axios'
import http from './http'

export function fetchStudents(params) {
  return http.get('/api/admin/students', { params })
}

export function importStudent(data) {
  return http.post('/api/admin/students', data)
}

export function importStudentsCsv(file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/api/admin/students/batch-import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export async function downloadStudentImportTemplate() {
  const baseURL = import.meta.env.VITE_API_BASE_URL || ''
  const token = localStorage.getItem('accessToken')
  const response = await axios.get(`${baseURL}/api/admin/students/import-template`, {
    responseType: 'blob',
    headers: token ? { Authorization: token } : {},
  })
  return response.data
}
