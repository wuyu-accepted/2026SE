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
