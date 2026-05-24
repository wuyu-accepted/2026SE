<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import {
  createNotice,
  deleteNotice,
  estimateNoticeTarget,
  fetchNoticeDetail,
  fetchNoticeStats,
  fetchNotices,
  offlineNotice,
  publishNotice,
  updateNotice,
  uploadNoticeAttachment,
} from '../api/notice'
import { fetchStudents } from '../api/student'

const loading = ref(false)
const saving = ref(false)
const attachmentUploading = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const currentDetail = ref(null)
const currentStats = ref(null)
const editingId = ref(null)
const formRef = ref(null)
const cadreOptions = ref([])
const cadreLoading = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  noticeType: '',
  status: '',
})

const form = reactive({
  title: '',
  summary: '',
  content: '',
  noticeType: '',
  tag: '',
  priority: 0,
  isBanner: false,
  attachmentFileId: null,
  feedbackCadreIds: [],
  status: 0,
  target: {
    grades: [],
    majors: [],
    className: '',
    authType: '',
  },
})

const gradeOptions = [
  '2022本', '2022硕', '2022博',
  '2023本', '2023硕', '2023博',
  '2024本', '2024硕', '2024博',
  '2025本', '2025硕', '2025博',
  '2026本', '2026硕', '2026博',
]
const majorOptions = ['软件工程', '计算机科学与技术', '人工智能', '数据科学与大数据技术', '信息管理与信息系统']

const rules = {
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入通知正文', trigger: 'blur' }],
}

const dialogTitle = computed(() => (editingId.value ? '编辑通知' : '新建通知'))
const targetDisabled = computed(() => form.status === 1)

function statusLabel(status) {
  const map = { 0: '草稿', 1: '已发布', 2: '已下架' }
  return map[status] || '未知'
}

function statusType(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

function priorityLabel(priority) {
  const map = { 0: '普通', 1: '重要', 2: '紧急' }
  return map[priority] || '普通'
}

function priorityType(priority) {
  const map = { 0: 'info', 1: 'warning', 2: 'danger' }
  return map[priority] || 'info'
}

function authTypeLabel(authType) {
  const map = { student: '普通学生', cadre: '学生骨干' }
  return map[authType] || '不限'
}

function statusValue(row) {
  const status = Number(row.status)
  return Number.isFinite(status) ? status : 0
}

function deliveredCountValue(row) {
  return Number(row.deliveredCount || 0)
}

function hasAttachment(fileId) {
  return fileId !== null && fileId !== undefined && String(fileId).trim() !== ''
}

function canPublish(row) {
  return statusValue(row) !== 1 && deliveredCountValue(row) === 0
}

function canDelete(row) {
  return statusValue(row) !== 1 && deliveredCountValue(row) === 0
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function buildParams() {
  const status = query.status === '' || query.status === null || query.status === undefined ? undefined : query.status
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    keyword: query.keyword || undefined,
    noticeType: query.noticeType || undefined,
    status,
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await fetchNotices(buildParams())
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载通知列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadData()
}

function reset() {
  query.pageNum = 1
  query.keyword = ''
  query.noticeType = ''
  query.status = ''
  loadData()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    title: '',
    summary: '',
    content: '',
    noticeType: '',
    tag: '',
    priority: 0,
    isBanner: false,
    attachmentFileId: null,
    feedbackCadreIds: [],
    status: 0,
    target: {
      grades: [],
      majors: [],
      className: '',
      authType: '',
    },
  })
  formRef.value?.clearValidate?.()
}

function openCreate() {
  resetForm()
  loadCadreOptions()
  dialogVisible.value = true
}

async function openEdit(row) {
  try {
    const detail = await fetchNoticeDetail(row.id)
    editingId.value = detail.id
    Object.assign(form, {
      title: detail.title || '',
      summary: detail.summary || '',
      content: detail.content || '',
      noticeType: detail.noticeType || '',
      tag: detail.tag || '',
      priority: typeof detail.priority === 'number' ? detail.priority : 0,
      isBanner: !!detail.isBanner,
      attachmentFileId: detail.attachmentFileId || null,
      feedbackCadreIds: normalizeIdValues(detail.feedbackCadreIds),
      status: detail.status,
      target: {
        grades: normalizeSelectValues(detail.target?.grades, detail.target?.grade),
        majors: normalizeSelectValues(detail.target?.majors, detail.target?.major),
        className: detail.target?.className || '',
        authType: detail.target?.authType || '',
      },
    })
    loadCadreOptions()
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载通知详情失败')
  }
}

function buildPayload() {
  return {
    title: form.title,
    summary: form.summary,
    content: form.content,
    noticeType: form.noticeType,
    tag: form.tag,
    priority: form.priority,
    isBanner: form.isBanner,
    attachmentFileId: normalizeAttachmentFileId(form.attachmentFileId),
    feedbackCadreIds: normalizeLongList(form.feedbackCadreIds),
    target: {
      grade: form.target.grades[0] || '',
      grades: form.target.grades,
      major: form.target.majors[0] || '',
      majors: form.target.majors,
      className: form.target.className,
      authType: form.target.authType,
    },
  }
}

function normalizeOptionalLong(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : value
}

function normalizeLongList(values) {
  if (!Array.isArray(values)) {
    return []
  }
  const result = []
  values.forEach((value) => {
    const normalized = normalizeOptionalLong(value)
    if (normalized !== null && !result.includes(normalized)) {
      result.push(normalized)
    }
  })
  return result
}

function normalizeIdValues(values) {
  if (!Array.isArray(values)) {
    return []
  }
  return values.map((value) => String(value))
}

function normalizeAttachmentFileId(value) {
  if (value === null || value === undefined) {
    return null
  }
  const cleaned = String(value).trim()
  return cleaned || null
}

function normalizeSelectValues(values, legacyValue) {
  const result = []
  if (Array.isArray(values)) {
    values.forEach((value) => addSelectValue(result, value))
  }
  addSelectValue(result, legacyValue)
  return result
}

function addSelectValue(result, value) {
  const cleaned = value === null || value === undefined ? '' : String(value).trim()
  if (cleaned && !result.includes(cleaned)) {
    result.push(cleaned)
  }
}

function targetValuesText(values, legacyValue) {
  const normalized = normalizeSelectValues(values, legacyValue)
  return normalized.length ? normalized.join('、') : '不限'
}

function formatCadreOption(item) {
  const parts = [item.realName || `用户${item.userId}`, item.studentNo, item.className].filter(Boolean)
  return `${parts.join(' / ')}（${item.userId}）`
}

function feedbackCadreText(values) {
  const normalized = normalizeIdValues(values)
  if (!normalized.length) {
    return '未指定，普通问题直接由辅导员处理'
  }
  return normalized.map((id) => {
    const option = cadreOptions.value.find((item) => String(item.userId) === String(id))
    return option ? formatCadreOption(option) : id
  }).join('、')
}

async function loadCadreOptions() {
  cadreLoading.value = true
  try {
    const result = await fetchStudents({ authType: 'cadre', pageNum: 1, pageSize: 200 })
    cadreOptions.value = result.records || []
  } catch (error) {
    ElMessage.warning(error.message || '加载学生骨干列表失败，可手动输入用户ID')
  } finally {
    cadreLoading.value = false
  }
}

async function uploadAttachment(options) {
  attachmentUploading.value = true
  try {
    const result = await uploadNoticeAttachment(options.file)
    form.attachmentFileId = result.id
    ElMessage.success('附件上传成功')
    options.onSuccess?.(result)
  } catch (error) {
    ElMessage.error(error.message || '附件上传失败')
    options.onError?.(error)
  } finally {
    attachmentUploading.value = false
  }
}

function clearAttachment() {
  form.attachmentFileId = null
}

async function downloadAttachment(fileId) {
  if (!hasAttachment(fileId)) {
    return
  }
  const token = localStorage.getItem('accessToken')
  if (!token) {
    ElMessage.error('未登录')
    return
  }
  const baseURL = import.meta.env.VITE_API_BASE_URL || ''
  try {
    const response = await axios.get(`${baseURL}/api/files/${fileId}/download`, {
      responseType: 'blob',
      headers: { Authorization: token },
    })
    const blobUrl = window.URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = ''
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(blobUrl)
  } catch (error) {
    ElMessage.error(error.message || '附件下载失败')
  }
}

async function submitForm() {
  await formRef.value?.validate?.()
  saving.value = true
  try {
    if (editingId.value) {
      await updateNotice(editingId.value, buildPayload())
      ElMessage.success('通知已保存')
    } else {
      await createNotice(buildPayload())
      ElMessage.success('通知草稿已创建')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '保存通知失败')
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  try {
    const [detail, stats] = await Promise.all([
      fetchNoticeDetail(row.id),
      fetchNoticeStats(row.id),
    ])
    currentDetail.value = detail
    currentStats.value = stats
    drawerVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载通知详情失败')
  }
}

async function handleEstimate(row) {
  try {
    const result = await estimateNoticeTarget(row.id)
    ElMessage.info(`当前发布范围预计覆盖 ${result.targetCount || 0} 名学生`)
  } catch (error) {
    ElMessage.error(error.message || '预估发布人数失败')
  }
}

async function handlePublish(row) {
  try {
    await ElMessageBox.confirm('发布后将按筛选条件投递给学生，确认发布吗？', '发布通知', { type: 'warning' })
    const result = await publishNotice(row.id)
    ElMessage.success(`已发布，投递 ${result.deliveredCount || 0} 人`)
    if (drawerVisible.value && currentDetail.value?.id === row.id) {
      const [detail, stats] = await Promise.all([
        fetchNoticeDetail(row.id),
        fetchNoticeStats(row.id),
      ])
      currentDetail.value = detail
      currentStats.value = stats
    }
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '发布失败')
    }
  }
}

async function handleOffline(row) {
  try {
    await ElMessageBox.confirm('确认下架该通知吗？学生端已收到的消息第一阶段仍会保留。', '下架通知', { type: 'warning' })
    await offlineNotice(row.id)
    ElMessage.success('通知已下架')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '下架失败')
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该通知草稿吗？此操作不可恢复。', '删除通知', { type: 'warning' })
    await deleteNotice(row.id)
    ElMessage.success('通知已删除')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

function handleSizeChange() {
  query.pageNum = 1
  loadData()
}

onMounted(() => {
  loadData()
  loadCadreOptions()
})
</script>

<template>
  <div class="notices-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <div class="title">通知发布</div>
            <div class="subtitle">创建通知并按年级、专业、班级、身份筛选投递</div>
          </div>
          <div class="actions">
            <el-button :icon="'Refresh'" @click="loadData">刷新</el-button>
            <el-button type="primary" :icon="'Plus'" @click="openCreate">新建通知</el-button>
          </div>
        </div>
      </template>

      <el-form class="filters" :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="标题/摘要" style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.noticeType" clearable placeholder="全部" style="width: 140px">
            <el-option label="教学" value="教学" />
            <el-option label="党团" value="党团" />
            <el-option label="就业" value="就业" />
            <el-option label="生活" value="生活" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无通知">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="noticeType" label="类型" width="90">
          <template #default="{ row }">{{ row.noticeType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="tag" label="标签" width="90">
          <template #default="{ row }">{{ row.tag || '-' }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" effect="plain">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveredCount" label="投递人数" width="100" />
        <el-table-column label="附件" width="80">
          <template #default="{ row }">
            <el-tag v-if="hasAttachment(row.attachmentFileId)" type="success" effect="plain">有</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="150">
          <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canPublish(row)" type="info" link @click="handleEstimate(row)">预估</el-button>
            <el-button v-if="canPublish(row)" type="success" link @click="handlePublish(row)">发布</el-button>
            <el-button v-if="statusValue(row) === 1" type="warning" link @click="handleOffline(row)">下架</el-button>
            <el-button v-if="canDelete(row)" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
      <el-alert
        v-if="targetDisabled"
        title="已发布通知可编辑内容，但不会改变已投递人群。"
        type="info"
        show-icon
        :closable="false"
        class="dialog-tip"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="255" show-word-limit placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" maxlength="500" show-word-limit placeholder="用于学生端列表展示" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="7" placeholder="请输入通知正文" />
        </el-form-item>
        <el-form-item label="附件">
          <div class="attachment-editor">
            <el-input v-model="form.attachmentFileId" clearable placeholder="可填写已上传文件 ID，或点击右侧上传" />
            <el-upload :show-file-list="false" :http-request="uploadAttachment" :disabled="attachmentUploading">
              <el-button :loading="attachmentUploading">上传附件</el-button>
            </el-upload>
            <el-button v-if="hasAttachment(form.attachmentFileId)" type="primary" link @click="downloadAttachment(form.attachmentFileId)">下载</el-button>
            <el-button v-if="hasAttachment(form.attachmentFileId)" type="danger" link @click="clearAttachment">移除</el-button>
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="form.noticeType" clearable placeholder="请选择">
                <el-option label="教学" value="教学" />
                <el-option label="党团" value="党团" />
                <el-option label="就业" value="就业" />
                <el-option label="生活" value="生活" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标签">
              <el-input v-model="form.tag" maxlength="32" placeholder="如 重要" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-select v-model="form.priority">
                <el-option label="普通" :value="0" />
                <el-option label="重要" :value="1" />
                <el-option label="紧急" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="首页轮播">
              <el-switch v-model="form.isBanner" active-text="展示" inactive-text="关闭" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">疑问反馈处理</el-divider>
        <el-alert
          title="普通问题由指定学生骨干处理；未指定骨干时由发布通知的辅导员处理。私密问题由发布通知的辅导员处理。"
          type="info"
          show-icon
          :closable="false"
          class="dialog-tip"
        />
        <el-form-item label="处理骨干">
          <el-select
            v-model="form.feedbackCadreIds"
            multiple
            filterable
            allow-create
            default-first-option
            clearable
            :loading="cadreLoading"
            placeholder="选择学生骨干，或手动输入用户ID"
          >
            <el-option
              v-for="item in cadreOptions"
              :key="item.userId"
              :label="formatCadreOption(item)"
              :value="String(item.userId)"
            />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">发布范围</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="年级">
              <el-select
                v-model="form.target.grades"
                :disabled="targetDisabled"
                multiple
                filterable
                allow-create
                default-first-option
                clearable
                placeholder="如 2023本 / 2022硕 / 2023博"
              >
                <el-option v-for="item in gradeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-select
                v-model="form.target.majors"
                :disabled="targetDisabled"
                multiple
                filterable
                allow-create
                default-first-option
                clearable
                placeholder="可选择或输入专业"
              >
                <el-option v-for="item in majorOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级">
              <el-input v-model="form.target.className" :disabled="targetDisabled" placeholder="留空表示不限" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份">
              <el-select v-model="form.target.authType" :disabled="targetDisabled" clearable placeholder="不限">
                <el-option label="普通学生" value="student" />
                <el-option label="学生骨干" value="cadre" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" title="通知详情" size="520px">
      <template v-if="currentDetail">
        <div class="drawer-actions">
          <el-button v-if="canPublish(currentDetail)" type="info" @click="handleEstimate(currentDetail)">预估人数</el-button>
          <el-button v-if="canPublish(currentDetail)" type="success" @click="handlePublish(currentDetail)">发布通知</el-button>
          <el-button v-if="statusValue(currentDetail) === 1" type="warning" @click="handleOffline(currentDetail)">下架通知</el-button>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ currentDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="摘要">{{ currentDetail.summary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentDetail.noticeType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签">{{ currentDetail.tag || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ priorityLabel(currentDetail.priority) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(currentDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="投递人数">{{ currentDetail.deliveredCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="附件">
            <el-button v-if="hasAttachment(currentDetail.attachmentFileId)" type="primary" link @click="downloadAttachment(currentDetail.attachmentFileId)">下载附件</el-button>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="最终负责人">{{ currentDetail.feedbackCounselorId || (currentDetail.status === 1 ? '-' : '发布时确定') }}</el-descriptions-item>
          <el-descriptions-item label="普通问题骨干">{{ feedbackCadreText(currentDetail.feedbackCadreIds) }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(currentDetail.publishTime) }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-section">
          <div class="detail-title">阅读统计</div>
          <el-row :gutter="12" class="stats-row">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ currentStats?.deliveredCount || 0 }}</div>
                <div class="stat-label">投递人数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ currentStats?.readCount || 0 }}</div>
                <div class="stat-label">已读</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ currentStats?.unreadCount || 0 }}</div>
                <div class="stat-label">未读</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ currentStats?.readRate || 0 }}%</div>
                <div class="stat-label">已读率</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="detail-section">
          <div class="detail-title">发布范围</div>
          <el-tag>年级：{{ targetValuesText(currentDetail.target?.grades, currentDetail.target?.grade) }}</el-tag>
          <el-tag>专业：{{ targetValuesText(currentDetail.target?.majors, currentDetail.target?.major) }}</el-tag>
          <el-tag>班级：{{ currentDetail.target?.className || '不限' }}</el-tag>
          <el-tag>身份：{{ authTypeLabel(currentDetail.target?.authType) }}</el-tag>
        </div>
        <div class="detail-section">
          <div class="detail-title">正文</div>
          <div class="content-box">{{ currentDetail.content }}</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.notices-page {
  max-width: 1280px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.subtitle {
  margin-top: 4px;
  color: #7a8a9a;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 8px;
}

.filters {
  margin-bottom: 14px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.dialog-tip {
  margin-bottom: 16px;
}

.attachment-editor {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.attachment-editor .el-input {
  flex: 1;
}

.detail-section {
  margin-top: 18px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 14px;
}

.detail-title {
  margin-bottom: 10px;
  font-weight: 700;
  color: #1f2d3d;
}

.detail-section .el-tag {
  margin: 0 8px 8px 0;
}

.stats-row {
  margin-bottom: 4px;
}

.stat-card {
  padding: 12px 8px;
  text-align: center;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.content-box {
  padding: 14px;
  min-height: 120px;
  white-space: pre-wrap;
  line-height: 1.7;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
</style>
