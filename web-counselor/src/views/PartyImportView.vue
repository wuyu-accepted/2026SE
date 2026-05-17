<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { batchImportPartyProgress, fetchPartyStageOptions, fetchPartyStepOptions } from '../api/party'

const activeTab = ref('single')

const stageOptions = ref([])
const defaultStageCode = ref('')
const stageLoading = ref(false)

const stepOptions = ref([])
const stepLoading = ref(false)

const fallbackStageOptions = [
  { stageCode: 'applicant', stageName: '入党申请人' },
  { stageCode: 'activist', stageName: '积极分子' },
  { stageCode: 'development_target', stageName: '发展对象' },
  { stageCode: 'probationary_member', stageName: '预备党员' },
  { stageCode: 'full_member', stageName: '正式党员' },
]

const displayStageOptions = computed(() => (stageOptions.value && stageOptions.value.length ? stageOptions.value : fallbackStageOptions))

const singleFormRef = ref(null)
const singleSubmitting = ref(false)
const singleForm = reactive({
  studentNo: '',
  realName: '',
  stageCode: '',
  stepCode: '',
  startDate: '',
  endDate: '',
  remark: '',
})

const rawText = ref('')
const parsing = ref(false)
const submitting = ref(false)
const rows = ref([])
const result = ref(null)

const validRows = computed(() => rows.value.filter((item) => !item.error))

const singleRules = {
  studentNo: [
    {
      validator: (_, value, callback) => {
        const v = toText(value)
        if (!v) {
          callback()
          return
        }
        if (!/^\d+$/.test(v)) {
          callback(new Error('学号必须为数字'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  stageCode: [{ required: true, message: '请选择阶段', trigger: 'change' }],
  endDate: [
    {
      validator: (_, value, callback) => {
        const end = toText(value)
        const start = toText(singleForm.startDate)
        if (!start || !end) {
          callback()
          return
        }
        const startTs = parseDate(start)
        const endTs = parseDate(end)
        if (startTs == null || endTs == null) {
          callback(new Error('时间格式不正确（yyyy-MM-dd）'))
          return
        }
        if (endTs < startTs) {
          callback(new Error('结束时间不能早于开始时间'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
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
  if (Number.isNaN(date.getTime())) {
    return null
  }
  return date.getTime()
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
  return (
    lower.includes('studentno') ||
    lower.includes('realname') ||
    lower.includes('name') ||
    lower.includes('stagecode') ||
    lower.includes('stepcode')
  )
}

function mapFieldsByHeader(headerFields, fields) {
  const map = {}
  for (let i = 0; i < headerFields.length; i += 1) {
    const key = toText(headerFields[i]).toLowerCase()
    map[key] = fields[i]
  }
  return {
    studentNo: toText(map.studentno || map['学号'] || map.student_no),
    realName: toText(map.realname || map.name || map['姓名']),
    stageCode: toText(map.stagecode || map['阶段'] || map.stage_code),
    stepCode: toText(map.stepcode || map['步骤'] || map.step_code),
    startDate: toText(map.startdate || map['开始日期'] || map['开始时间'] || map.start_time || map.startdate),
    endDate: toText(map.enddate || map['结束日期'] || map['结束时间'] || map.end_time || map.enddate),
    remark: toText(map.remark || map['备注']),
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

    if (!payload.stageCode && defaultStageCode.value) {
      payload.stageCode = defaultStageCode.value
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

async function onParse() {
  parsing.value = true
  result.value = null
  try {
    rows.value = parseCsv(rawText.value)
    if (!rows.value.length) {
      ElMessage.warning('没有可解析的数据')
      return
    }
    const invalidCount = rows.value.filter((item) => item.error).length
    if (invalidCount) {
      ElMessage.warning(`解析完成：${invalidCount} 行存在校验错误`)
    } else {
      ElMessage.success(`解析完成：共 ${rows.value.length} 行`)
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
}

async function onSubmit() {
  if (!rows.value.length) {
    ElMessage.warning('请先解析预览')
    return
  }
  if (!validRows.value.length) {
    ElMessage.error('没有可导入的有效数据')
    return
  }

  submitting.value = true
  try {
    const payload = validRows.value.map((item) => ({
      studentNo: item.studentNo || null,
      realName: item.realName || null,
      stageCode: item.stageCode,
      stepCode: item.stepCode || null,
      startTime: item.startDate || null,
      endTime: item.endDate || null,
      remark: item.remark || null,
    }))
    result.value = await batchImportPartyProgress(payload)
    ElMessage.success(`导入完成：成功 ${result.value.successCount}，失败 ${result.value.failCount}`)
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    submitting.value = false
  }
}

async function onSingleSubmit() {
  if (!singleFormRef.value) {
    return
  }

  const studentNo = toText(singleForm.studentNo)
  const realName = toText(singleForm.realName)
  if (!studentNo && !realName) {
    ElMessage.error('学号或姓名至少填写一个')
    return
  }

  try {
    await singleFormRef.value.validate()
  } catch {
    return
  }

  const startTs = parseDate(singleForm.startDate)
  const endTs = parseDate(singleForm.endDate)
  if (requiresStartDate(singleForm.stageCode) && !toText(singleForm.startDate)) {
    ElMessage.error('该阶段必须填写开始日期')
    return
  }
  if (singleForm.startDate && startTs == null) {
    ElMessage.error('开始日期格式不正确（yyyy-MM-dd）')
    return
  }
  if (singleForm.endDate && endTs == null) {
    ElMessage.error('结束日期格式不正确（yyyy-MM-dd）')
    return
  }
  if (startTs != null && endTs != null && endTs < startTs) {
    ElMessage.error('结束日期不能早于开始日期')
    return
  }

  singleSubmitting.value = true
  try {
    const payload = [
      {
        studentNo: studentNo || null,
        realName: realName || null,
        stageCode: singleForm.stageCode,
        stepCode: toText(singleForm.stepCode) || null,
        startTime: toText(singleForm.startDate) || null,
        endTime: toText(singleForm.endDate) || null,
        remark: toText(singleForm.remark) || null,
      },
    ]
    const res = await batchImportPartyProgress(payload)
    result.value = res
    ElMessage.success(`保存成功：成功 ${res.successCount}，失败 ${res.failCount}`)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    singleSubmitting.value = false
  }
}

function onSingleReset() {
  singleForm.studentNo = ''
  singleForm.realName = ''
  singleForm.stageCode = ''
  singleForm.stepCode = ''
  singleForm.startDate = ''
  singleForm.endDate = ''
  singleForm.remark = ''
}

async function loadStageOptions() {
  stageLoading.value = true
  try {
    stageOptions.value = await fetchPartyStageOptions()
    if (!stageOptions.value || !stageOptions.value.length) {
      ElMessage.warning('未加载到阶段配置，将使用默认阶段列表（请检查后端是否已执行数据库迁移/是否连接到正确的数据库）')
    }
  } catch (error) {
    ElMessage.error((error && error.message) || '加载阶段配置失败（请检查是否已登录、后端地址是否正确、数据库是否有 party_stage_def 数据）')
  }
  finally {
    stageLoading.value = false
  }
}

async function loadStepOptions(stageCode) {
  const sc = toText(stageCode)
  stepOptions.value = []
  if (!sc) {
    return
  }
  stepLoading.value = true
  try {
    stepOptions.value = await fetchPartyStepOptions(sc)
  } catch (error) {
    stepOptions.value = []
    ElMessage.error((error && error.message) || '加载步骤配置失败（可先手动填写 stepCode）')
  } finally {
    stepLoading.value = false
  }
}

watch(
  () => singleForm.stageCode,
  (next) => {
    singleForm.stepCode = ''
    loadStepOptions(next)
  },
)

onMounted(loadStageOptions)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <div class="header-left">
          <span>入党进度导入</span>
          <el-tag size="small" type="info">支持学号/姓名</el-tag>
        </div>
      </div>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane name="single" label="单条录入">
        <el-form ref="singleFormRef" :model="singleForm" :rules="singleRules" label-width="120px">
          <el-form-item label="学号">
            <el-input v-model="singleForm.studentNo" placeholder="可选：纯数字，优先使用" style="width: 360px" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="singleForm.realName" placeholder="可选：与学号二选一（姓名重名会提示用学号）" style="width: 360px" />
          </el-form-item>
          <el-form-item label="阶段" prop="stageCode">
            <el-select v-model="singleForm.stageCode" :loading="stageLoading" placeholder="请选择阶段" style="width: 360px">
              <el-option
                v-for="item in displayStageOptions"
                :key="item.stageCode"
                :label="`${item.stageName}（${item.stageCode}）`"
                :value="item.stageCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="步骤(stepCode)">
            <el-select
              v-model="singleForm.stepCode"
              :loading="stepLoading"
              filterable
              allow-create
              clearable
              default-first-option
              placeholder="可选：不填则自动取该阶段第一步"
              style="width: 360px"
            >
              <el-option
                v-for="item in stepOptions"
                :key="item.stepCode"
                :label="`${item.stepName}（${item.stepCode}）`"
                :value="item.stepCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker v-model="singleForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="yyyy-MM-dd" />
          </el-form-item>
          <el-form-item label="结束日期" prop="endDate">
            <el-date-picker v-model="singleForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="yyyy-MM-dd" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="singleForm.remark" type="textarea" :rows="3" placeholder="可选" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="singleSubmitting" @click="onSingleSubmit">保存</el-button>
            <el-button :disabled="singleSubmitting" @click="onSingleReset">重置</el-button>
            <div class="hint">时间精确到日，格式：yyyy-MM-dd</div>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane name="batch" label="文件/粘贴导入">
        <el-form label-width="120px">
          <el-form-item label="默认阶段">
            <el-select v-model="defaultStageCode" placeholder="可选：用于补全缺失的 stageCode" clearable style="width: 360px">
              <el-option
                v-for="item in displayStageOptions"
                :key="item.stageCode"
                :label="`${item.stageName}（${item.stageCode}）`"
                :value="item.stageCode"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="导入文件">
            <input type="file" accept=".csv,.txt" @change="onFileChange" />
          </el-form-item>

          <el-form-item label="粘贴数据">
            <el-input
              v-model="rawText"
              type="textarea"
              :rows="10"
              placeholder="支持 CSV/Tab 分隔。\n表头建议：studentNo,realName,stageCode,stepCode,startDate,endDate,remark\n时间格式：yyyy-MM-dd"
            />
          </el-form-item>

          <el-form-item>
            <el-button :loading="parsing" @click="onParse">解析预览</el-button>
            <el-button type="primary" :loading="submitting" @click="onSubmit">提交导入</el-button>
            <el-button @click="rows = []; result = null">清空预览</el-button>
            <div class="hint">时间精确到日，格式：yyyy-MM-dd</div>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <el-divider />

    <el-table :data="rows" stripe empty-text="暂无预览数据">
      <el-table-column prop="rowNo" label="#" width="60" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="stageCode" label="阶段(stageCode)" width="160" />
      <el-table-column prop="stepCode" label="步骤(stepCode)" width="220" show-overflow-tooltip />
      <el-table-column prop="startDate" label="开始日期" width="140" />
      <el-table-column prop="endDate" label="结束日期" width="140" />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="校验" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.error" type="danger" size="small">{{ row.error }}</el-tag>
          <el-tag v-else type="success" size="small">可导入</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="rows.length" class="summary">
      <el-tag type="info">总行数：{{ rows.length }}</el-tag>
      <el-tag type="success">可导入：{{ validRows.length }}</el-tag>
      <el-tag type="danger">异常：{{ rows.length - validRows.length }}</el-tag>
    </div>

    <el-divider v-if="result" />

    <div v-if="result" class="result">
      <div class="result-head">
        <span>导入结果</span>
        <el-tag type="success">成功 {{ result.successCount }}</el-tag>
        <el-tag type="danger">失败 {{ result.failCount }}</el-tag>
      </div>
      <el-table :data="result.rows" stripe empty-text="无结果明细">
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
  </el-card>
</template>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.hint {
  margin-left: 12px;
  color: #64748b;
  font-size: 13px;
}

.summary {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}

.result {
  margin-top: 8px;
}

.result-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}
</style>
