import http from './http'

export function fetchKnowledgeArticles(params) {
  return http.get('/api/admin/knowledge/articles', { params })
}

export function fetchKnowledgeArticleDetail(id) {
  return http.get(`/api/admin/knowledge/articles/${id}`)
}

export function createKnowledgeArticle(data) {
  return http.post('/api/admin/knowledge/articles', data)
}

export function updateKnowledgeArticle(id, data) {
  return http.put(`/api/admin/knowledge/articles/${id}`, data)
}

export function updateKnowledgeArticleStatus(id, status) {
  return http.put(`/api/admin/knowledge/articles/${id}/status`, { status })
}

export function previewKnowledgeArticle(data) {
  return http.post('/api/admin/knowledge/articles/preview', data)
}

export function deleteKnowledgeArticle(id) {
  return http.delete(`/api/admin/knowledge/articles/${id}`)
}

export function sourceDownloadUrl(id) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || ''
  return `${baseURL}/api/admin/knowledge/articles/${id}/source`
}

export function fetchKnowledgeTemplates(params) {
  return http.get('/api/admin/knowledge/templates', { params })
}

export function createKnowledgeTemplate(data) {
  return http.post('/api/admin/knowledge/templates', data)
}

export function updateKnowledgeTemplate(id, data) {
  return http.put(`/api/admin/knowledge/templates/${id}`, data)
}

export function updateKnowledgeTemplateStatus(id, status) {
  return http.put(`/api/admin/knowledge/templates/${id}/status`, { status })
}

export function deleteKnowledgeTemplate(id) {
  return http.delete(`/api/admin/knowledge/templates/${id}`)
}

export function fetchKnowledgeCategories() {
  return http.get('/api/admin/knowledge/categories')
}

export function createKnowledgeCategory(data) {
  return http.post('/api/admin/knowledge/categories', data)
}

export function updateKnowledgeCategory(id, data) {
  return http.put(`/api/admin/knowledge/categories/${id}`, data)
}

export function fetchKnowledgeStats() {
  return http.get('/api/admin/knowledge/stats')
}

export function uploadKnowledgeTemplate(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', 'knowledge-template')
  return http.post('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function uploadKnowledgeFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', 'knowledge-file')
  return http.post('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}


export function uploadKnowledgeImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', 'knowledge-image')
  return http.post('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function rebuildKnowledgeIndex() {
  return http.post('/api/admin/knowledge/index/rebuild')
}

export function fetchKnowledgeIndexTasks(params) {
  return http.get('/api/admin/knowledge/index/tasks', { params })
}

export function retryKnowledgeIndexTask(taskId) {
  return http.post(`/api/admin/knowledge/index/tasks/${taskId}/retry`)
}

export function correctKnowledgeOcrText(id, correctedText) {
  return http.put(`/api/admin/knowledge/articles/${id}/ocr-correction`, { correctedText })
}

export function fetchKnowledgeSynonyms() {
  return http.get('/api/admin/knowledge/synonyms')
}

export function createKnowledgeSynonym(data) {
  return http.post('/api/admin/knowledge/synonyms', data)
}

export function fetchRecommendWeights() {
  return http.get('/api/admin/knowledge/recommend-weights')
}

export function createRecommendWeight(data) {
  return http.post('/api/admin/knowledge/recommend-weights', data)
}

export function fetchKnowledgeGovernanceStats() {
  return http.get('/api/admin/knowledge/governance/stats')
}
