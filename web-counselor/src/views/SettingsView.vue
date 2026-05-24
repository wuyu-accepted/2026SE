<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  activateAiConfig,
  createAiConfig,
  deleteAiConfig,
  fetchAiConfigs,
  fetchAuditLogs,
  fetchCounselors,
  testAiConfig,
  updateAiConfig,
} from '../api/admin'

const activeTab = ref('counselors')

// 辅导员账户管理
const counselors = ref([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref(null)
const form = ref({ realName: '', studentNo: '', phone: '', password: '' })
const pendingCounselorCount = computed(() => counselors.value.filter((item) => item.status !== 1).length)

async function loadCounselors() {
  loading.value = true
  try {
    const res = await fetchCounselors()
    counselors.value = res || []
  } catch (e) {
    ElMessage.error('加载辅导员列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { realName: '', studentNo: '', phone: '', password: '' }
  dialog.value = true
}

function openEdit(c) {
  editing.value = c
  form.value = { realName: c.realName, studentNo: c.studentNo, phone: c.phone || '', password: '' }
  dialog.value = true
}

function normalizeJobNo(value) {
  form.value.studentNo = String(value || '').replace(/\D/g, '')
}

async function save() {
  if (!form.value.realName || !form.value.studentNo) {
    ElMessage.warning('请填写姓名和工号')
    return
  }
  if (!/^\d+$/.test(form.value.studentNo)) {
    ElMessage.warning('辅导员工号只能填写数字')
    return
  }
  try {
    const http = (await import('../api/http')).default
    if (editing.value) {
      await http.put(`/api/admin/counselors/${editing.value.id}`, {
        realName: form.value.realName,
        phone: form.value.phone,
        password: form.value.password || undefined,
      })
      ElMessage.success('更新成功')
    } else {
      if (!form.value.password) {
        ElMessage.warning('请设置密码')
        return
      }
      await http.post('/api/admin/counselors', {
        realName: form.value.realName,
        studentNo: form.value.studentNo,
        phone: form.value.phone,
        password: form.value.password,
      })
      ElMessage.success('新增成功')
    }
    dialog.value = false
    await loadCounselors()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该辅导员账号？', '确认删除', { type: 'warning' })
    const http = (await import('../api/http')).default
    await http.delete(`/api/admin/counselors/${id}`)
    ElMessage.success('已删除')
    await loadCounselors()
  } catch (_) {}
}

async function handleApprove(id) {
  try {
    await ElMessageBox.confirm('确认通过该辅导员注册申请？', '审核通过', { type: 'warning' })
    const http = (await import('../api/http')).default
    await http.post(`/api/admin/counselors/${id}/approve`)
    ElMessage.success('已审核通过')
    await loadCounselors()
  } catch (_) {}
}

// 审计日志
const auditLogs = ref([])
const auditTotal = ref(0)
const auditLoading = ref(false)
const auditQuery = ref({ module: '', action: '', pageNum: 1, pageSize: 20 })
const modules = ['user', 'knowledge', 'party', 'notice', 'admin']
const actions = ['create', 'update', 'delete', 'login', 'logout']

async function loadAuditLogs() {
  auditLoading.value = true
  try {
    const res = await fetchAuditLogs({
      module: auditQuery.value.module || undefined,
      action: auditQuery.value.action || undefined,
      pageNum: auditQuery.value.pageNum,
      pageSize: auditQuery.value.pageSize,
    })
    auditLogs.value = res.records
    auditTotal.value = res.total
  } catch (e) {
    ElMessage.error('加载审计日志失败')
  } finally {
    auditLoading.value = false
  }
}

function onPageChange(page) {
  auditQuery.value.pageNum = page
  loadAuditLogs()
}

// AI 助手配置
const aiConfigs = ref([])
const aiLoading = ref(false)
const aiDialog = ref(false)
const aiEditing = ref(null)
const aiTestDialog = ref(false)
const aiTesting = ref(false)
const aiTestTarget = ref(null)
const aiTestQuestion = ref('请用一句话回复：连接测试成功')
const aiTestResult = ref(null)
const aiProviderOptions = [
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'OpenAI 兼容接口', value: 'openai-compatible' },
  { label: 'OpenAI', value: 'openai' },
]
const aiModelOptions = computed(() => {
  if (aiForm.value.provider === 'deepseek') {
    return ['deepseek-chat', 'deepseek-reasoner']
  }
  if (aiForm.value.provider === 'openai') {
    return ['gpt-4o-mini', 'gpt-4o']
  }
  return []
})
const activeAiConfig = computed(() => aiConfigs.value.find((item) => item.active))
const aiForm = ref(defaultAiForm())

function defaultAiForm() {
  return {
    configName: 'DeepSeek 默认配置',
    provider: 'deepseek',
    baseUrl: 'https://api.deepseek.com',
    apiKey: '',
    model: 'deepseek-chat',
    temperature: 0.3,
    topP: 1,
    maxTokens: 1200,
    presencePenalty: 0,
    frequencyPenalty: 0,
    responseFormat: '',
    timeoutSeconds: 30,
    streamEnabled: false,
    retrievalTopK: 5,
    actionTopK: 3,
    systemPrompt: '你是学院学生服务平台 AI 助手，回答要简洁、准确，并优先引导用户到平台内可用功能入口。',
    enabled: true,
  }
}

async function loadAiConfigs() {
  aiLoading.value = true
  try {
    aiConfigs.value = await fetchAiConfigs()
  } catch (e) {
    ElMessage.error(e.message || '加载 AI 配置失败')
  } finally {
    aiLoading.value = false
  }
}

function openAiCreate() {
  aiEditing.value = null
  aiForm.value = defaultAiForm()
  aiDialog.value = true
}

function openAiEdit(row) {
  aiEditing.value = row
  aiForm.value = {
    configName: row.configName,
    provider: row.provider,
    baseUrl: row.baseUrl,
    apiKey: '',
    model: row.model,
    temperature: row.temperature ?? 0.3,
    topP: row.topP ?? 1,
    maxTokens: row.maxTokens ?? 1200,
    presencePenalty: row.presencePenalty ?? 0,
    frequencyPenalty: row.frequencyPenalty ?? 0,
    responseFormat: row.responseFormat || '',
    timeoutSeconds: row.timeoutSeconds ?? 30,
    streamEnabled: !!row.streamEnabled,
    retrievalTopK: row.retrievalTopK ?? 5,
    actionTopK: row.actionTopK ?? 3,
    systemPrompt: row.systemPrompt || '',
    enabled: row.enabled !== false,
  }
  aiDialog.value = true
}

function onAiProviderChange(provider) {
  if (provider === 'deepseek') {
    aiForm.value.baseUrl = 'https://api.deepseek.com'
    aiForm.value.model = 'deepseek-chat'
  } else if (provider === 'openai') {
    aiForm.value.baseUrl = 'https://api.openai.com/v1'
    aiForm.value.model = 'gpt-4o-mini'
  } else {
    aiForm.value.model = ''
  }
}

async function saveAiConfig() {
  if (!aiForm.value.configName || !aiForm.value.provider || !aiForm.value.baseUrl || !aiForm.value.model) {
    ElMessage.warning('请填写配置名称、供应商、Base URL 和模型')
    return
  }
  if (!aiEditing.value && !aiForm.value.apiKey) {
    ElMessage.warning('新增配置需要填写 API Key')
    return
  }
  try {
    const payload = { ...aiForm.value }
    if (aiEditing.value && !payload.apiKey) {
      delete payload.apiKey
    }
    if (!payload.responseFormat) {
      delete payload.responseFormat
    }
    if (aiEditing.value) {
      await updateAiConfig(aiEditing.value.id, payload)
      ElMessage.success('AI 配置已更新')
    } else {
      await createAiConfig(payload)
      ElMessage.success('AI 配置已新增')
    }
    aiDialog.value = false
    await loadAiConfigs()
  } catch (e) {
    ElMessage.error(e.message || '保存 AI 配置失败')
  }
}

async function handleActivateAi(row) {
  try {
    if (row.active) {
      await ElMessageBox.confirm(`确定取消“${row.configName}”作为当前 AI 配置？取消后学生端 AI 将进入降级模式。`, '取消当前配置', { type: 'warning' })
      await activateAiConfig(row.id)
      ElMessage.success('已取消当前配置')
    } else {
      await ElMessageBox.confirm(`确定将“${row.configName}”设为当前 AI 配置？`, '设为当前配置', { type: 'warning' })
      await activateAiConfig(row.id)
      ElMessage.success('已设为当前配置')
    }
    await loadAiConfigs()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message || 'AI 配置状态切换失败')
  }
}

function openAiTest(row) {
  aiTestTarget.value = row
  aiTestQuestion.value = '请用一句话回复：连接测试成功'
  aiTestResult.value = null
  aiTestDialog.value = true
}

async function runAiTest() {
  if (!aiTestTarget.value) return
  aiTesting.value = true
  try {
    aiTestResult.value = await testAiConfig(aiTestTarget.value.id, { message: aiTestQuestion.value })
  } catch (e) {
    aiTestResult.value = { success: false, errorMessage: e.message || '测试失败' }
  } finally {
    aiTesting.value = false
  }
}

async function handleDeleteAi(row) {
  try {
    await ElMessageBox.confirm(`确定删除“${row.configName}”？`, '删除 AI 配置', { type: 'warning' })
    await deleteAiConfig(row.id)
    ElMessage.success('已删除')
    await loadAiConfigs()
  } catch (_) {}
}

onMounted(() => {
  loadCounselors()
  loadAuditLogs()
  loadAiConfigs()
})
</script>

<template>
  <div class="settings-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <el-icon size="18" color="#1677ff"><Setting /></el-icon>
          <span>系统设置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane name="counselors">
          <template #label>
            <span>辅导员账户管理</span>
            <el-badge v-if="pendingCounselorCount" :value="pendingCounselorCount" class="tab-badge" />
          </template>
          <div class="toolbar">
            <el-button type="primary" size="small" @click="openCreate">
              <el-icon><Plus /></el-icon> 新增辅导员
            </el-button>
            <el-tag v-if="pendingCounselorCount" type="warning" effect="plain">
              {{ pendingCounselorCount }} 个注册申请待审核
            </el-tag>
          </div>
          <el-table :data="counselors" v-loading="loading" stripe empty-text="暂无辅导员账户">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="studentNo" label="工号" width="120" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                  {{ row.status === 1 ? '正常' : '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status !== 1" type="success" link @click="handleApprove(row.id)">通过</el-button>
                <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="审计日志" name="audit">
          <div class="toolbar">
            <el-select v-model="auditQuery.module" placeholder="全部模块" size="small" clearable style="width: 140px" @change="loadAuditLogs">
              <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
            </el-select>
            <el-select v-model="auditQuery.action" placeholder="全部操作" size="small" clearable style="width: 140px" @change="loadAuditLogs">
              <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
            </el-select>
            <el-button size="small" @click="loadAuditLogs">刷新</el-button>
          </div>
          <el-table :data="auditLogs" v-loading="auditLoading" stripe empty-text="暂无审计日志">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userName" label="操作人" width="100" />
            <el-table-column prop="module" label="模块" width="90" />
            <el-table-column prop="action" label="操作" width="80" />
            <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
            <el-table-column prop="ipAddress" label="IP" width="140" />
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
          <div class="pagination" v-if="auditTotal > auditQuery.pageSize">
            <el-pagination
              v-model:current-page="auditQuery.pageNum"
              :page-size="auditQuery.pageSize"
              :total="auditTotal"
              layout="prev, pager, next"
              @current-change="onPageChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="AI 助手配置" name="ai">
          <div class="toolbar">
            <el-button type="primary" size="small" @click="openAiCreate">
              <el-icon><Plus /></el-icon> 新增 AI 配置
            </el-button>
            <el-tag v-if="activeAiConfig" type="success" effect="plain">
              当前使用：{{ activeAiConfig.configName }} / {{ activeAiConfig.provider }} / {{ activeAiConfig.model }}
            </el-tag>
            <el-tag v-else type="warning" effect="plain">
              当前未启用 AI 配置
            </el-tag>
          </div>
          <el-table :data="aiConfigs" v-loading="aiLoading" stripe empty-text="暂无 AI 配置">
            <el-table-column prop="configName" label="名称" min-width="140" />
            <el-table-column prop="provider" label="供应商" width="130" />
            <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip />
            <el-table-column prop="model" label="模型" width="150" />
            <el-table-column label="API Key" width="120">
              <template #default="{ row }">
                <el-tag :type="row.hasApiKey ? 'success' : 'info'" size="small">{{ row.hasApiKey ? row.apiKeyMask : '未配置' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.active ? 'success' : (row.enabled ? 'warning' : 'info')" size="small">
                  {{ row.active ? '当前使用' : (row.enabled ? '启用' : '停用') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="180" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openAiEdit(row)">编辑</el-button>
                <el-button :type="row.active ? 'info' : 'success'" link @click="handleActivateAi(row)">
                  {{ row.active ? '取消' : '设为当前' }}
                </el-button>
                <el-button type="warning" link @click="openAiTest(row)">测试</el-button>
                <el-button type="danger" link :disabled="row.active" @click="handleDeleteAi(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="dialog"
      :title="editing ? '编辑辅导员' : '新增辅导员'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" placeholder="辅导员姓名" />
        </el-form-item>
        <el-form-item label="工号" required :disabled="!!editing">
          <el-input v-model="form.studentNo" placeholder="数字辅导员工号" @input="normalizeJobNo" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item :label="editing ? '新密码' : '密码'" :required="!editing">
          <el-input v-model="form.password" type="password" :placeholder="editing ? '留空则不修改' : '设置密码'" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="aiDialog"
      :title="aiEditing ? '编辑 AI 配置' : '新增 AI 配置'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form :model="aiForm" label-width="120px">
        <el-form-item label="配置名称" required>
          <el-input v-model="aiForm.configName" placeholder="例如：DeepSeek 默认配置" />
        </el-form-item>
        <el-form-item label="供应商" required>
          <el-select v-model="aiForm.provider" style="width: 100%" @change="onAiProviderChange">
            <el-option v-for="item in aiProviderOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL" required>
          <el-input v-model="aiForm.baseUrl" placeholder="https://api.deepseek.com" />
        </el-form-item>
        <el-form-item label="API Key" :required="!aiEditing">
          <el-input v-model="aiForm.apiKey" type="password" :placeholder="aiEditing ? '留空则保留原 Key' : '请输入 API Key'" show-password />
        </el-form-item>
        <el-form-item label="模型" required>
          <el-select v-if="aiModelOptions.length" v-model="aiForm.model" style="width: 100%">
            <el-option v-for="item in aiModelOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-input v-else v-model="aiForm.model" placeholder="输入模型名" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="temperature"><el-input-number v-model="aiForm.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="top_p"><el-input-number v-model="aiForm.topP" :min="0" :max="1" :step="0.1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="max_tokens"><el-input-number v-model="aiForm.maxTokens" :min="1" :max="8192" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="timeout 秒"><el-input-number v-model="aiForm.timeoutSeconds" :min="5" :max="120" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="入口数量"><el-input-number v-model="aiForm.actionTopK" :min="1" :max="10" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="知识数量"><el-input-number v-model="aiForm.retrievalTopK" :min="1" :max="10" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="系统提示词">
          <el-input v-model="aiForm.systemPrompt" type="textarea" :rows="4" placeholder="输入平台内置提示词之外的补充约束" />
        </el-form-item>
        <el-form-item label="流式输出">
          <el-switch v-model="aiForm.streamEnabled" />
        </el-form-item>
        <el-form-item label="启用配置">
          <el-switch v-model="aiForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAiConfig">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiTestDialog" title="测试 AI 配置" width="640px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="测试问题">
          <el-input v-model="aiTestQuestion" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-alert v-if="aiTestResult" :type="aiTestResult.success ? 'success' : 'error'" :title="aiTestResult.success ? '测试成功' : '测试失败'" :description="aiTestResult.success ? `${aiTestResult.provider} / ${aiTestResult.model}，耗时 ${aiTestResult.latencyMs} ms` : aiTestResult.errorMessage" show-icon />
      <template #footer>
        <el-button @click="aiTestDialog = false">关闭</el-button>
        <el-button type="primary" :loading="aiTesting" @click="runAiTest">开始测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 1200px;
}

.header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.tab-badge {
  margin-left: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
