import http from './http'

export function login(data) {
  return http.post('/api/auth/login', data)
}

export function fetchCurrentUser() {
  return http.get('/api/auth/me')
}

export function logout() {
  return http.post('/api/auth/logout')
}
