<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPartyStageOptions, fetchPartyStepOptions, fetchPartyStudentProgress, updatePartyStudentProgress, batchImportPartyProgress, fetchPartyReports, fetchPartyReportDetail, approvePartyReport, rejectPartyReport, fetchPartyActivities, fetchPartyActivityDetail, approvePartyActivity, rejectPartyActivity } from '../api/party'

const activeTab = ref('stages')

// 阶段管理
const stages = ref([])
const steps = ref([])
const selectedStage = ref('')

async function loadStages() {
  try {
    stages.value = await fetchPartyStageOptions()
  } catch (_) {}
}

async function loadSteps(stageCode) {
  try {
    steps.value = await fetchPartyStepOptions(stageCode)
  } catch (_) {}
}

function onStageSelect(code) {
  selectedStage.value = code
  if (code) loadSteps(code)
}

// 学生进度
const progressQuery = ref({ stageCode: '', keyword: '', pageNum: 1, pageSize: 20 })
const progressList = ref([])
const progressTotal = ref(0)
const progressLoading = ref(false)
const progressDialog = ref(false)
const editingProgress = ref(null)
const progressForm = ref({ stageCode: '', stepCode: '' })
const stageOptions = ref([])

async function loadProgress() {
  progressLoading.value = true
  try {
    const res = await fetchPartyStudentProgress({
      stageCode: progressQuery.value.stageCode || undefined,
      keyword: progressQuery.value.keyword || undefined,
      pageNum: progressQuery.value.pageNum,
      pageSize: progressQuery.value.pageSize,
    })
    progressList.value = res.records
    progressTotal.value = res.total
  } catch (e) {
    ElMessage.error('加载学生进度失败')
  } finally {
    progressLoading.value = false
  }
}

async function openEditProgress(row) {
  editingProgress.value = row
  progressForm.value = { stageCode: row.currentStageCode, stepCode: row.currentStepCode || '' }
  stageOptions.value = await fetchPartyStageOptions()
  if (row.currentStageCode) await loadStepOptions(row.currentStageCode)
  progressDialog.value = true
}

async function loadStepOptions(stageCode) {
  try {
    stepOptions.value = await fetchPartyStepOptions(stageCode)
  } catch (_) {
    stepOptions.value = []
  }
}

const stepOptions = ref([])

async function onStageChange(code) {
  progressForm.value.stepCode = ''
  await loadStepOptions(code)
}

async function saveProgress() {
  try {
    await updatePartyStudentProgress(editingProgress.value.userId, {
      stageCode: progressForm.value.stageCode,
      stepCode: progressForm.value.stepCode,
    })
    ElMessage.success('更新成功')
    progressDialog.value = false
    await loadProgress()
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  }
}

// 思想汇报
const reportQuery = ref({ status: undefined, keyword: '', pageNum: 1, pageSize: 20 })
const reportList = ref([])
const reportTotal = ref(0)
const reportLoading = ref(false)
const reportDetailDialog = ref(false)
const reportDetail = ref(null)
const reportReviewComment = ref('')

async function loadReports() {
  reportLoading.value = true
  try {
    const res = await fetchPartyReports({
      status: reportQuery.value.status,
      keyword: reportQuery.value.keyword || undefined,
      pageNum: reportQuery.value.pageNum,
      pageSize: reportQuery.value.pageSize,
    })
    reportList.value = res.records
    reportTotal.value = res.total
  } catch (e) {
    ElMessage.error('加载思想汇报失败')
  } finally {
    reportLoading.value = false
  }
}

async function openReportDetail(id) {
  try {
    reportDetail.value = await fetchPartyReportDetail(id)
    reportReviewComment.value = ''
    reportDetailDialog.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

async function doApproveReport() {
  if (!reportReviewComment.value.trim()) {
    ElMessage.warning('请填写审核意见')
    return
  }
  try {
    await approvePartyReport(reportDetail.value.id, reportReviewComment.value.trim())
    ElMessage.success('已通过')
    reportDetailDialog.value = false
    await loadReports()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function doRejectReport() {
  if (!reportReviewComment.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await rejectPartyReport(reportDetail.value.id, reportReviewComment.value.trim())
    ElMessage.success('已驳回')
    reportDetailDialog.value = false
    await loadReports()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// 活动审核
const activityQuery = ref({ status: undefined, keyword: '', pageNum: 1, pageSize: 20 })
const activityList = ref([])
const activityTotal = ref(0)
const activityLoading = ref(false)
const activityDetailDialog = ref(false)
const activityDetail = ref(null)
const activityReviewComment = ref('')

async function loadActivities() {
  activityLoading.value = true
  try {
    const res = await fetchPartyActivities({
      status: activityQuery.value.status,
      keyword: activityQuery.value.keyword || undefined,
      pageNum: activityQuery.value.pageNum,
      pageSize: activityQuery.value.pageSize,
    })
    activityList.value = res.records
    activityTotal.value = res.total
  } catch (e) {
    ElMessage.error('加载活动申请失败')
  } finally {
    activityLoading.value = false
  }
}

async function openActivityDetail(id) {
  try {
    activityDetail.value = await fetchPartyActivityDetail(id)
    activityReviewComment.value = ''
    activityDetailDialog.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

async function doApproveActivity() {
  if (!activityReviewComment.value.trim()) {
    ElMessage.warning('请填写审核意见')
    return
  }
  try {
    await approvePartyActivity(activityDetail.value.id, activityReviewComment.value.trim())
    ElMessage.success('已通过')
    activityDetailDialog.value = false
    await loadActivities()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function doRejectActivity() {
  if (!activityReviewComment.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await rejectPartyActivity(activityDetail.value.id, activityReviewComment.value.trim())
    ElMessage.success('已驳回')
    activityDetailDialog.value = false
    await loadActivities()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  loadStages()
  loadProgress()
  loadReports()
  loadActivities()
})
</script>

<template>
  <div class="party-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <el-icon size="18" color="#1677ff"><Flag /></el-icon>
          <span>党团管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="阶段与步骤" name="stages">
          <el-row :gutter="16">
            <el-col :span="12">
              <h4 class="sub-title">阶段定义</h4>
              <el-table :data="stages" stripe empty-text="暂无阶段数据">
                <el-table-column prop="stageCode" label="编码" width="160" />
                <el-table-column prop="stageName" label="名称" width="140" />
                <el-table-column prop="sortOrder" label="排序" width="60" />
                <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
              </el-table>
            </el-col>
            <el-col :span="12">
              <h4 class="sub-title">
                步骤
                <el-select v-model="selectedStage" placeholder="选择阶段" size="small" style="width: 200px; margin-left: 12px" @change="onStageSelect" clearable>
                  <el-option v-for="s in stages" :key="s.stageCode" :label="s.stageName" :value="s.stageCode" />
                </el-select>
              </h4>
              <el-table :data="steps" stripe empty-text="请先选择阶段">
                <el-table-column prop="stepCode" label="编码" width="200" />
                <el-table-column prop="stepName" label="名称" min-width="200" />
                <el-table-column prop="sortOrder" label="排序" width="60" />
              </el-table>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="学生进度" name="progress">
          <div class="toolbar">
            <el-input v-model="progressQuery.keyword" placeholder="搜索姓名/学号" size="small" clearable style="width: 200px" @clear="loadProgress" />
            <el-select v-model="progressQuery.stageCode" placeholder="按阶段筛选" size="small" clearable style="width: 160px" @change="loadProgress">
              <el-option v-for="s in stages" :key="s.stageCode" :label="s.stageName" :value="s.stageCode" />
            </el-select>
            <el-button size="small" @click="loadProgress">查询</el-button>
          </div>
          <el-table :data="progressList" v-loading="progressLoading" stripe empty-text="暂无学生进度数据">
            <el-table-column prop="studentNo" label="学号" width="110" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="currentStageName" label="当前阶段" width="130" />
            <el-table-column prop="currentStepName" label="当前步骤" min-width="200" show-overflow-tooltip />
            <el-table-column prop="updatedAt" label="更新时间" width="180" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openEditProgress(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination" v-if="progressTotal > progressQuery.pageSize">
            <el-pagination v-model:current-page="progressQuery.pageNum" :page-size="progressQuery.pageSize" :total="progressTotal" layout="prev, pager, next" @current-change="loadProgress" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="思想汇报审核" name="reports">
          <div class="toolbar">
            <el-select v-model="reportQuery.status" placeholder="全部状态" size="small" clearable style="width: 140px" @change="loadReports">
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
            <el-input v-model="reportQuery.keyword" placeholder="搜索姓名/标题" size="small" clearable style="width: 200px" @clear="loadReports" />
            <el-button size="small" @click="loadReports">查询</el-button>
          </div>
          <el-table :data="reportList" v-loading="reportLoading" stripe empty-text="暂无思想汇报">
            <el-table-column prop="studentNo" label="学号" width="110" />
            <el-table-column prop="realName" label="学生" width="90" />
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
                <el-tag v-else type="danger" size="small">已驳回</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openReportDetail(row.id)">审核</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination" v-if="reportTotal > reportQuery.pageSize">
            <el-pagination v-model:current-page="reportQuery.pageNum" :page-size="reportQuery.pageSize" :total="reportTotal" layout="prev, pager, next" @current-change="loadReports" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="活动申请审核" name="activities">
          <div class="toolbar">
            <el-select v-model="activityQuery.status" placeholder="全部状态" size="small" clearable style="width: 140px" @change="loadActivities">
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
            <el-input v-model="activityQuery.keyword" placeholder="搜索姓名/标题" size="small" clearable style="width: 200px" @clear="loadActivities" />
            <el-button size="small" @click="loadActivities">查询</el-button>
          </div>
          <el-table :data="activityList" v-loading="activityLoading" stripe empty-text="暂无活动申请">
            <el-table-column prop="studentNo" label="学号" width="110" />
            <el-table-column prop="realName" label="申请人" width="90" />
            <el-table-column prop="title" label="活动名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="eventDate" label="活动日期" width="120" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="warning" size="small">待审核</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
                <el-tag v-else type="danger" size="small">已驳回</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openActivityDetail(row.id)">审核</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination" v-if="activityTotal > activityQuery.pageSize">
            <el-pagination v-model:current-page="activityQuery.pageNum" :page-size="activityQuery.pageSize" :total="activityTotal" layout="prev, pager, next" @current-change="loadActivities" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 编辑进度对话框 -->
    <el-dialog v-model="progressDialog" title="编辑学生进度" width="420px" :close-on-click-modal="false">
      <el-form :model="progressForm" label-width="100px">
        <el-form-item label="当前阶段" required>
          <el-select v-model="progressForm.stageCode" style="width: 100%" @change="onStageChange">
            <el-option v-for="s in stageOptions" :key="s.stageCode" :label="s.stageName" :value="s.stageCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前步骤">
          <el-select v-model="progressForm.stepCode" style="width: 100%" clearable>
            <el-option v-for="s in stepOptions" :key="s.stepCode" :label="s.stepName" :value="s.stepCode" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProgress">保存</el-button>
      </template>
    </el-dialog>

    <!-- 思想汇报审核对话框 -->
    <el-dialog v-model="reportDetailDialog" title="思想汇报详情" width="600px" :close-on-click-modal="false">
      <template v-if="reportDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生">{{ reportDetail.studentNo }} {{ reportDetail.realName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="reportDetail.status === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="reportDetail.status === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else type="danger" size="small">已驳回</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ reportDetail.title }}</el-descriptions-item>
          <el-descriptions-item v-if="reportDetail.content" label="内容" :span="2">{{ reportDetail.content }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ reportDetail.submitTime }}</el-descriptions-item>
          <el-descriptions-item v-if="reportDetail.reviewComment" label="审核意见" :span="2">{{ reportDetail.reviewComment }}</el-descriptions-item>
        </el-descriptions>
        <div class="review-box" v-if="reportDetail.status === 0">
          <el-input v-model="reportReviewComment" type="textarea" :rows="3" placeholder="审核意见或驳回原因" />
          <div class="ops">
            <el-button type="success" @click="doApproveReport">通过</el-button>
            <el-button type="danger" @click="doRejectReport">驳回</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 活动审核对话框 -->
    <el-dialog v-model="activityDetailDialog" title="活动申请详情" width="600px" :close-on-click-modal="false">
      <template v-if="activityDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请人">{{ activityDetail.studentNo }} {{ activityDetail.realName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="activityDetail.status === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="activityDetail.status === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else type="danger" size="small">已驳回</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="活动名称" :span="2">{{ activityDetail.title }}</el-descriptions-item>
          <el-descriptions-item v-if="activityDetail.reason" label="活动说明" :span="2">{{ activityDetail.reason }}</el-descriptions-item>
          <el-descriptions-item label="活动日期">{{ activityDetail.eventDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ activityDetail.submitTime }}</el-descriptions-item>
          <el-descriptions-item v-if="activityDetail.reviewComment" label="审核意见" :span="2">{{ activityDetail.reviewComment }}</el-descriptions-item>
        </el-descriptions>
        <div class="review-box" v-if="activityDetail.status === 0">
          <el-input v-model="activityReviewComment" type="textarea" :rows="3" placeholder="审核意见或驳回原因" />
          <div class="ops">
            <el-button type="success" @click="doApproveActivity">通过</el-button>
            <el-button type="danger" @click="doRejectActivity">驳回</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.party-page {
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

.sub-title {
  font-size: 14px;
  color: #333;
  margin: 0 0 12px;
  display: flex;
  align-items: center;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
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
  gap: 12px;
}
</style>
