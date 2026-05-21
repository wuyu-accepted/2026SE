<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchCounselorFeedbackDetail,
  fetchCounselorFeedbacks,
  replyCounselorFeedback,
} from '../api/noticeFeedback'

const loading = ref(false)
const replying = ref(false)
const rows = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const detail = ref(null)
const replyContent = ref('')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  feedbackType: '',
  status: '',
  noticeId: '',
})

function typeLabel(type) {
  return type === 'private' ? '私密问题' : '普通问题'
}

function statusLabel(status) {
  const map = {
    pending_cadre: '待骨干处理',
    pending_counselor: '待辅导员处理',
    resolved_by_cadre: '骨干已处理',
    resolved_by_counselor: '辅导员已处理',
    closed: '已关闭',
  }
  return map[status] || status || '-'
}

function actionLabel(action) {
  const map = {
    submit: '学生提交',
    cadre_reply: '骨干回复处理',
    escalate: '骨干上报辅导员',
    counselor_reply: '辅导员回复处理',
    resolve: '处理完成',
  }
  return map[action] || action || '-'
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function buildParams() {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    feedbackType: query.feedbackType || undefined,
    status: query.status || undefined,
    noticeId: query.noticeId || undefined,
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await fetchCounselorFeedbacks(buildParams())
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载反馈列表失败')
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
  query.feedbackType = ''
  query.status = ''
  query.noticeId = ''
  loadData()
}

async function openDetail(row) {
  try {
    detail.value = await fetchCounselorFeedbackDetail(row.id)
    replyContent.value = ''
    drawerVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载反馈详情失败')
  }
}

async function submitReply() {
  const content = String(replyContent.value || '').trim()
  if (!content || !detail.value) {
    ElMessage.error('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyCounselorFeedback(detail.value.id, content)
    ElMessage.success('已回复并处理')
    drawerVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  } finally {
    replying.value = false
  }
}

function handleSizeChange() {
  query.pageNum = 1
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="feedback-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <div class="title">反馈处理</div>
            <div class="subtitle">查看私密问题、骨干上报问题，并检查骨干处理日志</div>
          </div>
          <el-button @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form :model="query" inline class="filters">
        <el-form-item label="类型">
          <el-select v-model="query.feedbackType" clearable placeholder="全部" style="width: 130px">
            <el-option label="普通问题" value="ordinary" />
            <el-option label="私密问题" value="private" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 160px">
            <el-option label="待骨干处理" value="pending_cadre" />
            <el-option label="待辅导员处理" value="pending_counselor" />
            <el-option label="骨干已处理" value="resolved_by_cadre" />
            <el-option label="辅导员已处理" value="resolved_by_counselor" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知ID">
          <el-input v-model="query.noticeId" clearable placeholder="可选" style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无反馈">
        <el-table-column prop="noticeTitle" label="通知" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeLabel(row.feedbackType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
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

    <el-drawer v-model="drawerVisible" title="反馈详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="通知">{{ detail.noticeTitle }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeLabel(detail.feedbackType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="学生问题">{{ detail.content }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">处理日志</div>
        <el-timeline>
          <el-timeline-item
            v-for="item in detail.messages || []"
            :key="item.id"
            :timestamp="formatTime(item.createdAt)"
          >
            <div class="log-title">{{ actionLabel(item.actionType) }} · 用户 {{ item.senderUserId }}</div>
            <div class="log-content">{{ item.content || '-' }}</div>
          </el-timeline-item>
        </el-timeline>

        <div v-if="detail.status === 'pending_counselor' || detail.status === 'pending_cadre'" class="reply-box">
          <div class="section-title">辅导员回复</div>
          <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="请输入处理回复" />
          <el-button class="reply-btn" type="primary" :loading="replying" @click="submitReply">回复并关闭</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.feedback-page {
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

.filters {
  margin-bottom: 14px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.section-title {
  margin: 18px 0 10px;
  font-weight: 700;
  color: #1f2d3d;
}

.log-title {
  font-weight: 600;
  color: #334155;
}

.log-content {
  margin-top: 6px;
  white-space: pre-wrap;
  color: #475569;
}

.reply-box {
  margin-top: 18px;
}

.reply-btn {
  margin-top: 12px;
}
</style>
