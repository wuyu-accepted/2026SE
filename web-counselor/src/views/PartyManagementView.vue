<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import {
  approvePartyActivity,
  approvePartyReport,
  batchImportPartyProgress,
  fetchPartyActivities,
  fetchPartyActivityDetail,
  fetchPartyReports,
  fetchPartyReportDetail,
  fetchPartyStageOptions,
  fetchPartyStepOptions,
  fetchPartyStudentProgressPage,
  rejectPartyActivity,
  rejectPartyReport,
  updatePartyStudentProgress,
} from '../api/party'

const activeTab = ref('progress')

const fallbackStageOptions = [
  { stageCode: 'applicant', stageName: '申请入党' },
  { stageCode: 'activist', stageName: '积极分子' },
  { stageCode: 'development_target', stageName: '发展对象' },
  { stageCode: 'probationary_member', stageName: '预备党员' },
  { stageCode: 'full_member', stageName: '正式党员' },
]

const stages = ref([])
const stageLoading = ref(false)

const displayStageOptions = computed(() => (stages.value.length ? stages.value : fallbackStageOptions))

async function loadStages() {
  stageLoading.value = true
  try {
    stages.value = await fetchPartyStageOptions()
  } catch (error) {
    stages.value = []
    ElMessage.error(error.message || '加载阶段配置失败')
  } finally {
    stageLoading.value = false
  }
}

const progressQuery = reactive({
  stageCode: '',
  keyword: '',
  pageNum: 1,
  pageSize: 20,
})
const progressList = ref([])
const progressTotal = ref(0)
const progressLoading = ref(false)

async function loadProgress() {
  progressLoading.value = true
  try {
    const result = await fetchPartyStudentProgressPage({
      stageCode: progressQuery.stageCode || undefined,
      keyword: progressQuery.keyword || undefined,
      pageNum: progressQuery.pageNum,
      pageSize: progressQuery.pageSize,
    })
    progressList.value = result?.records || []
    progressTotal.value = Number(result?.total || 0)
  } catch (error) {
    progressList.value = []
    progressTotal.value = 0
    ElMessage.error(error.message || '加载学生进度失败')
  } finally {
    progressLoading.value = false
  }
}

function onProgressSearch() {
  progressQuery.pageNum = 1
  loadProgress()
}

const progressDialog = ref(false)
const progressSaving = ref(false)
const editingProgress = ref(null)
const progressForm = reactive({
  stageCode: '',
  stepCode: '',
})
const progressStepOptions = ref([])

async function loadProgressStepOptions(stageCode) {
  progressStepOptions.value = []
  if (!stageCode) {
    return
  }
  try {
    progressStepOptions.value = await fetchPartyStepOptions(stageCode)
  } catch (_) {
    progressStepOptions.value = []
  }
}

async function openEditProgress(row) {
  editingProgress.value = row
  progressForm.stageCode = row.currentStageCode || ''
  progressForm.stepCode = row.currentStepCode || ''
  await loadProgressStepOptions(progressForm.stageCode)
  progressDialog.value = true
}

async function onEditStageChange(stageCode) {
  progressForm.stepCode = ''
  await loadProgressStepOptions(stageCode)
}

async function saveProgress() {
  if (!editingProgress.value) {
    return
  }
  if (!progressForm.stageCode) {
    ElMessage.error('请选择阶段')
    return
  }
  progressSaving.value = true
  try {
    await updatePartyStudentProgress(editingProgress.value.userId, {
      stageCode: progressForm.stageCode,
      stepCode: progressForm.stepCode || null,
    })
    ElMessage.success('学生进度已更新')
    progressDialog.value = false
    await loadProgress()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    progressSaving.value = false
  }
}

const importDialog = ref(false)
const importActiveTab = ref('single')
const singleFormRef = ref(null)
const singleSubmitting = ref(false)
const importSingleForm = reactive({
  studentNo: '',
  realName: '',
  stageCode: '',
  stepCode: '',
  startDate: '',
  endDate: '',
  remark: '',
})
const importSingleStepOptions = ref([])
const rawText = ref('')
const parsing = ref(false)
const submitting = ref(false)
const previewRows = ref([])
const importResult = ref(null)

const validRows = computed(() => previewRows.value.filter((item) => !item.error))

const singleRules = {
  stageCode: [{ required: true, message: '请选择阶段', trigger: 'change' }],
}

function toText(value) {
  return value == null ? '' : String(value).trim()
}

function parseDate(value) {
  const raw = toText(value)
  if (!raw) {
    return null
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return null
  }
  const date = new Date(`${raw}T00:00:00`)
  return Number.isNaN(date.getTime()) ? null : date.getTime()
}

function requiresStartDate(stageCode) {
  return ['applicant', 'activist', 'development_target', 'probationary_member', 'full_member'].includes(toText(stageCode))
}

function normalizeCsvLine(line) {
  return line.replace(/\uFEFF/g, '').trim()
}

function splitLine(line) {
  if (line.includes('\t')) {
    return line.split('\t').map((item) => item.trim())
  }
  return line.split(',').map((item) => item.trim())
}

function detectHeader(fields) {
  const lower = fields.map((item) => toText(item).toLowerCase())
  return lower.includes('studentno') || lower.includes('realname') || lower.includes('name') || lower.includes('stagecode') || lower.includes('stepcode')
}

function mapFieldsByHeader(headerFields, fields) {
  const mapped = {}
  for (let i = 0; i < headerFields.length; i += 1) {
    mapped[toText(headerFields[i]).toLowerCase()] = fields[i]
  }
  return {
    studentNo: toText(mapped.studentno || mapped['学号'] || mapped.student_no),
    realName: toText(mapped.realname || mapped.name || mapped['姓名']),
    stageCode: toText(mapped.stagecode || mapped['阶段'] || mapped.stage_code),
    stepCode: toText(mapped.stepcode || mapped['步骤'] || mapped.step_code),
    startDate: toText(mapped.startdate || mapped['开始日期'] || mapped['开始时间'] || mapped.start_time),
    endDate: toText(mapped.enddate || mapped['结束日期'] || mapped['结束时间'] || mapped.end_time),
    remark: toText(mapped.remark || mapped['备注']),
  }
}

function parseCsv(text) {
  const lines = text
    .split(/\r?\n/)
    .map(normalizeCsvLine)
    .filter(Boolean)

  if (!lines.length) {
    return []
  }

  const first = splitLine(lines[0])
  const hasHeader = detectHeader(first)
  const headerFields = hasHeader ? first : null
  const startIndex = hasHeader ? 1 : 0
  const parsed = []

  for (let i = startIndex; i < lines.length; i += 1) {
    const fields = splitLine(lines[i])
    const payload = headerFields
      ? mapFieldsByHeader(headerFields, fields)
      : fields.length <= 6
        ? {
            studentNo: toText(fields[0]),
            realName: '',
            stageCode: toText(fields[1]),
            stepCode: toText(fields[2]),
            startDate: toText(fields[3]),
            endDate: toText(fields[4]),
            remark: toText(fields[5]),
          }
        : {
            studentNo: toText(fields[0]),
            realName: toText(fields[1]),
            stageCode: toText(fields[2]),
            stepCode: toText(fields[3]),
            startDate: toText(fields[4]),
            endDate: toText(fields[5]),
            remark: toText(fields[6]),
          }

    const row = {
      rowNo: parsed.length + 1,
      ...payload,
      error: '',
    }

    if (!row.studentNo && !row.realName) {
      row.error = '学号或姓名至少填写一个'
    } else if (row.studentNo && !/^\d+$/.test(row.studentNo)) {
      row.error = '学号必须为数字'
    } else if (!row.stageCode) {
      row.error = 'stageCode不能为空'
    } else if (requiresStartDate(row.stageCode) && !row.startDate) {
      row.error = '该阶段必须填写开始日期'
    }

    const startTs = parseDate(row.startDate)
    const endTs = parseDate(row.endDate)
    if (!row.error && row.startDate && startTs == null) {
      row.error = '开始日期格式不正确（yyyy-MM-dd）'
    }
    if (!row.error && row.endDate && endTs == null) {
      row.error = '结束日期格式不正确（yyyy-MM-dd）'
    }
    if (!row.error && startTs != null && endTs != null && endTs < startTs) {
      row.error = '结束日期不能早于开始日期'
    }
    parsed.push(row)
  }

  return parsed
}

async function loadImportStepOptions(stageCode) {
  importSingleStepOptions.value = []
  if (!stageCode) {
    return
  }
  try {
    importSingleStepOptions.value = await fetchPartyStepOptions(stageCode)
  } catch (_) {
    importSingleStepOptions.value = []
  }
}

watch(
  () => importSingleForm.stageCode,
  (value) => {
    importSingleForm.stepCode = ''
    loadImportStepOptions(value)
  },
)

function resetSingleForm() {
  importSingleForm.studentNo = ''
  importSingleForm.realName = ''
  importSingleForm.stageCode = ''
  importSingleForm.stepCode = ''
  importSingleForm.startDate = ''
  importSingleForm.endDate = ''
  importSingleForm.remark = ''
}

function openImportDialog() {
  importDialog.value = true
  importActiveTab.value = 'single'
  importResult.value = null
}

function clearImportPreview() {
  previewRows.value = []
  importResult.value = null
  rawText.value = ''
}

async function onSingleImport() {
  const studentNo = toText(importSingleForm.studentNo)
  const realName = toText(importSingleForm.realName)
  if (!studentNo && !realName) {
    ElMessage.error('学号或姓名至少填写一个')
    return
  }
  if (studentNo && !/^\d+$/.test(studentNo)) {
    ElMessage.error('学号必须为数字')
    return
  }
  try {
    await singleFormRef.value.validate()
  } catch {
    return
  }
  if (requiresStartDate(importSingleForm.stageCode) && !toText(importSingleForm.startDate)) {
    ElMessage.error('该阶段必须填写开始日期')
    return
  }
  const startTs = parseDate(importSingleForm.startDate)
  const endTs = parseDate(importSingleForm.endDate)
  if (importSingleForm.startDate && startTs == null) {
    ElMessage.error('开始日期格式不正确（yyyy-MM-dd）')
    return
  }
  if (importSingleForm.endDate && endTs == null) {
    ElMessage.error('结束日期格式不正确（yyyy-MM-dd）')
    return
  }
  if (startTs != null && endTs != null && endTs < startTs) {
    ElMessage.error('结束日期不能早于开始日期')
    return
  }

  singleSubmitting.value = true
  try {
    importResult.value = await batchImportPartyProgress([
      {
        studentNo: studentNo || null,
        realName: realName || null,
        stageCode: importSingleForm.stageCode,
        stepCode: toText(importSingleForm.stepCode) || null,
        startTime: toText(importSingleForm.startDate) || null,
        endTime: toText(importSingleForm.endDate) || null,
        remark: toText(importSingleForm.remark) || null,
      },
    ])
    await loadProgress()
    ElMessage.success(`保存完成：成功 ${importResult.value.successCount}，失败 ${importResult.value.failCount}`)
    if (importResult.value.failCount === 0) {
      resetSingleForm()
    }
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    singleSubmitting.value = false
  }
}

async function onParse() {
  parsing.value = true
  importResult.value = null
  try {
    previewRows.value = parseCsv(rawText.value)
    if (!previewRows.value.length) {
      ElMessage.warning('没有可解析的数据')
      return
    }
    const invalidCount = previewRows.value.filter((item) => item.error).length
    if (invalidCount) {
      ElMessage.warning(`解析完成：${invalidCount} 行存在校验错误`)
    } else {
      ElMessage.success(`解析完成：共 ${previewRows.value.length} 行`)
    }
  } finally {
    parsing.value = false
  }
}

function onFileChange(event) {
  const file = event.target.files && event.target.files[0]
  if (!file) {
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    rawText.value = String(reader.result || '')
    ElMessage.success('文件已读取，可点击“解析预览”')
  }
  reader.onerror = () => {
    ElMessage.error('读取文件失败')
  }
  reader.readAsText(file, 'utf-8')
  event.target.value = ''
}

async function onBatchImport() {
  if (!previewRows.value.length) {
    ElMessage.warning('请先解析预览')
    return
  }
  if (!validRows.value.length) {
    ElMessage.error('没有可导入的有效数据')
    return
  }

  submitting.value = true
  try {
    importResult.value = await batchImportPartyProgress(
      validRows.value.map((item) => ({
        studentNo: item.studentNo || null,
        realName: item.realName || null,
        stageCode: item.stageCode,
        stepCode: item.stepCode || null,
        startTime: item.startDate || null,
        endTime: item.endDate || null,
        remark: item.remark || null,
      })),
    )
    await loadProgress()
    ElMessage.success(`导入完成：成功 ${importResult.value.successCount}，失败 ${importResult.value.failCount}`)
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    submitting.value = false
  }
}

const reportQuery = reactive({
  status: undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 20,
})
const reportList = ref([])
const reportTotal = ref(0)
const reportLoading = ref(false)
const reportDetailDialog = ref(false)
const reportDetail = ref(null)
const reportReviewComment = ref('')
const reportReviewing = ref(false)

async function loadReports() {
  reportLoading.value = true
  try {
    const result = await fetchPartyReports({
      status: reportQuery.status,
      keyword: reportQuery.keyword || undefined,
      pageNum: reportQuery.pageNum,
      pageSize: reportQuery.pageSize,
    })
    reportList.value = result?.records || []
    reportTotal.value = Number(result?.total || 0)
  } catch (error) {
    reportList.value = []
    reportTotal.value = 0
    ElMessage.error(error.message || '加载思想汇报失败')
  } finally {
    reportLoading.value = false
  }
}

function onReportSearch() {
  reportQuery.pageNum = 1
  loadReports()
}

async function openReportDetail(id) {
  try {
    reportDetail.value = await fetchPartyReportDetail(id)
    reportReviewComment.value = ''
    reportDetailDialog.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载思想汇报详情失败')
  }
}

async function downloadReportAttachment(fileId) {
  if (!fileId) {
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

async function doApproveReport() {
  const comment = toText(reportReviewComment.value)
  if (!comment) {
    ElMessage.warning('请填写审核意见')
    return
  }
  reportReviewing.value = true
  try {
    await approvePartyReport(reportDetail.value.id, comment)
    ElMessage.success('思想汇报已通过')
    reportDetailDialog.value = false
    await loadReports()
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    reportReviewing.value = false
  }
}

async function doRejectReport() {
  const comment = toText(reportReviewComment.value)
  if (!comment) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  reportReviewing.value = true
  try {
    await rejectPartyReport(reportDetail.value.id, comment)
    ElMessage.success('思想汇报已驳回')
    reportDetailDialog.value = false
    await loadReports()
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    reportReviewing.value = false
  }
}

const activityQuery = reactive({
  status: undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 20,
})
const activityList = ref([])
const activityTotal = ref(0)
const activityLoading = ref(false)
const activityDetailDialog = ref(false)
const activityDetail = ref(null)
const activityReviewComment = ref('')
const activityReviewing = ref(false)

async function loadActivities() {
  activityLoading.value = true
  try {
    const result = await fetchPartyActivities({
      status: activityQuery.status,
      keyword: activityQuery.keyword || undefined,
      pageNum: activityQuery.pageNum,
      pageSize: activityQuery.pageSize,
    })
    activityList.value = result?.records || []
    activityTotal.value = Number(result?.total || 0)
  } catch (error) {
    activityList.value = []
    activityTotal.value = 0
    ElMessage.error(error.message || '加载活动申请失败')
  } finally {
    activityLoading.value = false
  }
}

function onActivitySearch() {
  activityQuery.pageNum = 1
  loadActivities()
}

async function openActivityDetail(id) {
  try {
    activityDetail.value = await fetchPartyActivityDetail(id)
    activityReviewComment.value = ''
    activityDetailDialog.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载活动申请详情失败')
  }
}

async function doApproveActivity() {
  const comment = toText(activityReviewComment.value)
  if (!comment) {
    ElMessage.warning('请填写审核意见')
    return
  }
  activityReviewing.value = true
  try {
    await approvePartyActivity(activityDetail.value.id, comment)
    ElMessage.success('活动申请已通过')
    activityDetailDialog.value = false
    await loadActivities()
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    activityReviewing.value = false
  }
}

async function doRejectActivity() {
  const comment = toText(activityReviewComment.value)
  if (!comment) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  activityReviewing.value = true
  try {
    await rejectPartyActivity(activityDetail.value.id, comment)
    ElMessage.success('活动申请已驳回')
    activityDetailDialog.value = false
    await loadActivities()
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    activityReviewing.value = false
  }
}

onMounted(async () => {
  await loadStages()
  await Promise.all([loadProgress(), loadReports(), loadActivities()])
})
</script>

<template>
  <div class="party-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <div class="header-left">
            <el-icon size="18" color="#1677ff"><Flag /></el-icon>
            <span>党团事务</span>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="学生进度管理" name="progress">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="progressQuery.keyword"
                placeholder="搜索学号/姓名"
                clearable
                style="width: 220px"
                @keyup.enter="onProgressSearch"
                @clear="onProgressSearch"
              />
              <el-select
                v-model="progressQuery.stageCode"
                placeholder="按阶段筛选"
                clearable
                style="width: 180px"
                :loading="stageLoading"
                @change="onProgressSearch"
              >
                <el-option v-for="stage in displayStageOptions" :key="stage.stageCode" :label="stage.stageName" :value="stage.stageCode" />
              </el-select>
              <el-button @click="onProgressSearch">查询</el-button>
            </div>
            <div class="toolbar-right">
              <el-button type="primary" @click="openImportDialog">导入学生进度</el-button>
              <el-button @click="loadProgress">刷新</el-button>
            </div>
          </div>

          <el-table :data="progressList" v-loading="progressLoading" stripe empty-text="暂无学生进度数据">
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="currentStageName" label="当前阶段" width="150" />
            <el-table-column prop="currentStepName" label="当前步骤" min-width="220" show-overflow-tooltip />
            <el-table-column prop="updatedAt" label="更新时间" width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openEditProgress(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="progressTotal > progressQuery.pageSize">
            <el-pagination
              v-model:current-page="progressQuery.pageNum"
              :page-size="progressQuery.pageSize"
              :total="progressTotal"
              layout="prev, pager, next"
              @current-change="loadProgress"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="思想汇报审核" name="reports">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select v-model="reportQuery.status" placeholder="全部状态" clearable style="width: 160px" @change="onReportSearch">
                <el-option label="待审核" :value="0" />
                <el-option label="已通过" :value="1" />
                <el-option label="已驳回" :value="2" />
              </el-select>
              <el-input
                v-model="reportQuery.keyword"
                placeholder="搜索姓名/标题"
                clearable
                style="width: 220px"
                @keyup.enter="onReportSearch"
                @clear="onReportSearch"
              />
              <el-button @click="onReportSearch">查询</el-button>
            </div>
            <div class="toolbar-right">
              <el-button @click="loadReports">刷新</el-button>
            </div>
          </div>

          <el-table :data="reportList" v-loading="reportLoading" stripe empty-text="暂无思想汇报">
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="学生" width="120" />
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="stageCode" label="所属阶段" width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
                <el-tag v-else type="danger" size="small">已驳回</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openReportDetail(row.id)">{{ row.status === 0 ? '审核' : '查看' }}</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="reportTotal > reportQuery.pageSize">
            <el-pagination
              v-model:current-page="reportQuery.pageNum"
              :page-size="reportQuery.pageSize"
              :total="reportTotal"
              layout="prev, pager, next"
              @current-change="loadReports"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="党团活动审批" name="activities">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select v-model="activityQuery.status" placeholder="全部状态" clearable style="width: 160px" @change="onActivitySearch">
                <el-option label="待审核" :value="0" />
                <el-option label="已通过" :value="1" />
                <el-option label="已驳回" :value="2" />
              </el-select>
              <el-input
                v-model="activityQuery.keyword"
                placeholder="搜索申请人/标题"
                clearable
                style="width: 220px"
                @keyup.enter="onActivitySearch"
                @clear="onActivitySearch"
              />
              <el-button @click="onActivitySearch">查询</el-button>
            </div>
            <div class="toolbar-right">
              <el-button @click="loadActivities">刷新</el-button>
            </div>
          </div>

          <el-table :data="activityList" v-loading="activityLoading" stripe empty-text="暂无活动申请">
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="申请人" width="120" />
            <el-table-column prop="title" label="活动名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="eventDate" label="活动日期" width="130" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
                <el-tag v-else type="danger" size="small">已驳回</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openActivityDetail(row.id)">{{ row.status === 0 ? '审批' : '查看' }}</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="activityTotal > activityQuery.pageSize">
            <el-pagination
              v-model:current-page="activityQuery.pageNum"
              :page-size="activityQuery.pageSize"
              :total="activityTotal"
              layout="prev, pager, next"
              @current-change="loadActivities"
            />
          </div>
        </el-tab-pane>

      </el-tabs>
    </el-card>

    <el-dialog v-model="progressDialog" title="编辑学生进度" width="460px" :close-on-click-modal="false">
      <el-form :model="progressForm" label-width="100px">
        <el-form-item label="当前阶段" required>
          <el-select v-model="progressForm.stageCode" style="width: 100%" @change="onEditStageChange">
            <el-option v-for="stage in displayStageOptions" :key="stage.stageCode" :label="stage.stageName" :value="stage.stageCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前步骤">
          <el-select v-model="progressForm.stepCode" style="width: 100%" clearable placeholder="不填则由后端按阶段默认步骤处理">
            <el-option v-for="step in progressStepOptions" :key="step.stepCode" :label="step.stepName" :value="step.stepCode" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressDialog = false">取消</el-button>
        <el-button type="primary" :loading="progressSaving" @click="saveProgress">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialog" title="导入学生进度" width="920px" :close-on-click-modal="false">
      <el-tabs v-model="importActiveTab">
        <el-tab-pane label="单条录入" name="single">
          <el-form ref="singleFormRef" :model="importSingleForm" :rules="singleRules" label-width="110px">
            <el-form-item label="学号">
              <el-input v-model="importSingleForm.studentNo" placeholder="可选：纯数字，优先使用" />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="importSingleForm.realName" placeholder="可选：与学号二选一" />
            </el-form-item>
            <el-form-item label="阶段" prop="stageCode">
              <el-select v-model="importSingleForm.stageCode" style="width: 100%" :loading="stageLoading" placeholder="请选择阶段">
                <el-option v-for="stage in displayStageOptions" :key="stage.stageCode" :label="`${stage.stageName}（${stage.stageCode}）`" :value="stage.stageCode" />
              </el-select>
            </el-form-item>
            <el-form-item label="步骤">
              <el-select
                v-model="importSingleForm.stepCode"
                style="width: 100%"
                filterable
                allow-create
                clearable
                default-first-option
                placeholder="可选：不填则自动使用该阶段第一步"
              >
                <el-option v-for="step in importSingleStepOptions" :key="step.stepCode" :label="`${step.stepName}（${step.stepCode}）`" :value="step.stepCode" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="importSingleForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="yyyy-MM-dd" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="importSingleForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="yyyy-MM-dd" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="importSingleForm.remark" type="textarea" :rows="3" placeholder="可选" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="singleSubmitting" @click="onSingleImport">保存</el-button>
              <el-button @click="resetSingleForm">重置</el-button>
              <div class="hint">时间精确到日，格式：yyyy-MM-dd</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="文件/粘贴导入" name="batch">
          <el-form label-width="120px">
            <el-form-item label="导入文件">
              <input type="file" accept=".csv,.txt" @change="onFileChange" />
            </el-form-item>
            <el-form-item label="粘贴数据">
              <el-input
                v-model="rawText"
                type="textarea"
                :rows="10"
                placeholder="支持 CSV/Tab 分隔；表头建议：studentNo,realName,stageCode,stepCode,startDate,endDate,remark；时间格式：yyyy-MM-dd"
              />
            </el-form-item>
            <el-form-item>
              <el-button :loading="parsing" @click="onParse">解析预览</el-button>
              <el-button type="primary" :loading="submitting" @click="onBatchImport">提交导入</el-button>
              <el-button @click="clearImportPreview">清空</el-button>
              <div class="hint">解析后仅导入校验通过的数据</div>
            </el-form-item>
          </el-form>

          <el-table :data="previewRows" stripe empty-text="暂无预览数据">
            <el-table-column prop="rowNo" label="#" width="60" />
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="stageCode" label="阶段" width="160" />
            <el-table-column prop="stepCode" label="步骤" width="220" show-overflow-tooltip />
            <el-table-column prop="startDate" label="开始日期" width="130" />
            <el-table-column prop="endDate" label="结束日期" width="130" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="校验" width="150">
              <template #default="{ row }">
                <el-tag v-if="row.error" type="danger" size="small">{{ row.error }}</el-tag>
                <el-tag v-else type="success" size="small">可导入</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="previewRows.length" class="summary-tags">
            <el-tag type="info">总行数：{{ previewRows.length }}</el-tag>
            <el-tag type="success">可导入：{{ validRows.length }}</el-tag>
            <el-tag type="danger">异常：{{ previewRows.length - validRows.length }}</el-tag>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div v-if="importResult" class="result">
        <div class="result-head">
          <span>导入结果</span>
          <el-tag type="success">成功 {{ importResult.successCount }}</el-tag>
          <el-tag type="danger">失败 {{ importResult.failCount }}</el-tag>
        </div>
        <el-table :data="importResult.rows" stripe empty-text="无结果明细">
          <el-table-column prop="rowNo" label="#" width="60" />
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="stageCode" label="阶段" width="160" />
          <el-table-column prop="stepCode" label="步骤" width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.success" type="success" size="small">成功</el-tag>
              <el-tag v-else type="danger" size="small">失败</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="240" show-overflow-tooltip />
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="reportDetailDialog" title="思想汇报详情" width="680px" :close-on-click-modal="false">
      <template v-if="reportDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生">{{ reportDetail.studentNo }} {{ reportDetail.realName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="reportDetail.status === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="reportDetail.status === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else type="danger" size="small">已驳回</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属阶段">{{ reportDetail.stageCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ reportDetail.submitTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ reportDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div class="detail-content">{{ reportDetail.content || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="附件" :span="2">
            <el-button v-if="reportDetail.fileId" size="small" @click="downloadReportAttachment(reportDetail.fileId)">下载附件</el-button>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="reportDetail.reviewComment" label="审核意见" :span="2">{{ reportDetail.reviewComment }}</el-descriptions-item>
        </el-descriptions>
        <div class="review-box" v-if="reportDetail.status === 0">
          <el-input v-model="reportReviewComment" type="textarea" :rows="4" placeholder="审核意见或驳回原因" />
          <div class="ops">
            <el-button type="danger" :loading="reportReviewing" @click="doRejectReport">驳回</el-button>
            <el-button type="primary" :loading="reportReviewing" @click="doApproveReport">通过</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="activityDetailDialog" title="党团活动详情" width="680px" :close-on-click-modal="false">
      <template v-if="activityDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请人">{{ activityDetail.studentNo }} {{ activityDetail.realName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="activityDetail.status === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="activityDetail.status === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else type="danger" size="small">已驳回</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="活动名称" :span="2">{{ activityDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="活动日期">{{ activityDetail.eventDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ activityDetail.submitTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请事由" :span="2">
            <div class="detail-content">{{ activityDetail.reason || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="activityDetail.reviewComment" label="审核意见" :span="2">{{ activityDetail.reviewComment }}</el-descriptions-item>
        </el-descriptions>
        <div class="review-box" v-if="activityDetail.status === 0">
          <el-input v-model="activityReviewComment" type="textarea" :rows="4" placeholder="审核意见或驳回原因" />
          <div class="ops">
            <el-button type="danger" :loading="activityReviewing" @click="doRejectActivity">驳回</el-button>
            <el-button type="primary" :loading="activityReviewing" @click="doApproveActivity">通过</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.party-page {
  max-width: 1280px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.hint {
  margin-left: 12px;
  color: #64748b;
  font-size: 13px;
}

.summary-tags {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.result {
  margin-top: 16px;
}

.result-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  margin-bottom: 10px;
}

.review-box {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.ops {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.detail-content {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
