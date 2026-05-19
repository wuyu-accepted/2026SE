import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 12000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data || {}
    if (payload.code === 0) {
      return payload.data
    }
    return Promise.reject(new Error(payload.message || '请求失败'))
  },
  (error) => {
    const resp = error && error.response
    const data = resp && resp.data
    if (data && (data.message || data.msg)) {
      return Promise.reject(new Error(data.message || data.msg))
    }
    if (resp && resp.status) {
      return Promise.reject(new Error(`请求失败（HTTP ${resp.status}）`))
    }
    return Promise.reject(error)
  },
)

export default http
