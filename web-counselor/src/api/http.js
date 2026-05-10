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
  (error) => Promise.reject(error),
)

export default http
