<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchReviewList } from '../api/leave'
import { fetchCurrentUser } from '../api/auth'
import { fetchCounselorFeedbackCount } from '../api/noticeFeedback'
import { fetchCounselors } from '../api/admin'

const router = useRouter()
const pendingCount = ref(0)
const processedCount = ref(0)
const feedbackCount = ref(0)
const counselorRegisterCount = ref(0)
const loading = ref(false)
const userName = ref('')
const currentUser = ref(null)

const userRoles = computed(() => {
  if (currentUser.value) {
    return currentUser.value.roles || []
  }
  try {
    const u = localStorage.getItem('currentUser')
    return u ? JSON.parse(u).roles || [] : []
  } catch (_) {
    return []
  }
})
const isAdmin = computed(() => userRoles.value.includes('admin'))

const stats = computed(() => {
  const items = [
    { label: '待审批', value: pendingCount, icon: 'Clock', color: '#1677ff', bg: '#e6f4ff', path: '/review/pending' },
    { label: '已处理', value: processedCount, icon: 'Select', color: '#52c41a', bg: '#f6ffed', path: '/review/processed' },
    { label: '党团待办', value: ref('--'), icon: 'Flag', color: '#faad14', bg: '#fffbe6', path: '/party' },
    { label: '待处理反馈', value: feedbackCount, icon: 'ChatDotRound', color: '#722ed1', bg: '#f9f0ff', path: '/notice-feedback' },
  ]
  return items
})

const quickActions = [
  { label: '待审批列表', desc: '查看并处理学生申请', path: '/review/pending', color: '#1677ff' },
  { label: '党团管理', desc: '管理入党积极分子材料', path: '/party', color: '#faad14' },
  { label: '知识库', desc: '编辑学生知识库内容', path: '/knowledge', color: '#52c41a' },
  { label: '通知发布', desc: '发布学院公告通知', path: '/notices', color: '#ff4d4f' },
  { label: '反馈处理', desc: '处理学生疑问并检查骨干日志', path: '/notice-feedback', color: '#722ed1' },
]

async function loadData() {
  loading.value = true
  try {
    const user = await fetchCurrentUser()
    currentUser.value = user
    userName.value = user.realName
    localStorage.setItem('currentUser', JSON.stringify(user))
  } catch (_) {}
  try {
    const pending = await fetchReviewList(0)
    pendingCount.value = pending ? pending.length : 0
  } catch (_) {}
  try {
    const processed = await fetchReviewList(2)
    processedCount.value = processed ? processed.length : 0
  } catch (_) {}
  try {
    feedbackCount.value = await fetchCounselorFeedbackCount()
  } catch (_) {}
  if (isAdmin.value) {
    try {
      const counselors = await fetchCounselors()
      counselorRegisterCount.value = (counselors || []).filter((item) => item.status !== 1).length
    } catch (_) {}
  }
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <div class="dashboard">
    <div class="welcome">
      <h3>欢迎回来，{{ userName || '用户' }} 👋</h3>
      <p>以下是学院服务平台的运行概览</p>
    </div>

    <el-alert
      v-if="isAdmin && counselorRegisterCount > 0"
      class="admin-alert"
      type="warning"
      show-icon
      :closable="false"
    >
      <template #title>
        <div class="alert-title">
          <span>{{ counselorRegisterCount }} 个辅导员注册申请待审核</span>
          <el-button type="warning" size="small" @click="router.push('/settings')">去审核</el-button>
        </div>
      </template>
    </el-alert>

    <el-row :gutter="16" class="stat-row">
      <el-col v-for="s in stats" :key="s.label" :span="6">
        <el-card
          shadow="never"
          class="stat-card clickable"
          :style="{ background: s.bg }"
          role="button"
          tabindex="0"
          @click="router.push(s.path)"
          @keyup.enter="router.push(s.path)"
        >
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="section-header">
              <span>快捷入口</span>
            </div>
          </template>
          <div class="action-grid">
            <div
              v-for="action in quickActions"
              :key="action.label"
              class="action-item"
              @click="router.push(action.path)"
            >
              <div class="action-dot" :style="{ background: action.color }"></div>
              <div class="action-info">
                <div class="action-title">{{ action.label }}</div>
                <div class="action-desc">{{ action.desc }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="section-header">
              <span>系统信息</span>
            </div>
          </template>
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">运行模式</span>
              <el-tag size="small" type="warning">开发模式</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">当前角色</span>
              <el-tag v-if="isAdmin" type="danger" size="small">管理员</el-tag>
              <el-tag v-else type="warning" size="small">辅导员</el-tag>
            </div>
            <div v-if="isAdmin" class="info-item">
              <span class="info-label">辅导员注册申请</span>
              <el-tag :type="counselorRegisterCount > 0 ? 'warning' : 'success'" size="small">
                {{ counselorRegisterCount > 0 ? `${counselorRegisterCount} 个待审核` : '暂无待审核' }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">后端地址</span>
              <span class="info-value">127.0.0.1:18080</span>
            </div>
            <div class="info-item">
              <span class="info-label">审批模块</span>
              <span class="info-value">已就绪</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.welcome h3 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #1a1a1a;
}

.welcome p {
  margin: 0 0 20px;
  color: #666;
  font-size: 14px;
}

.stat-row {
  margin-bottom: 16px;
}

.admin-alert {
  margin-bottom: 16px;
}

.alert-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.stat-card {
  border-radius: 10px;
  border: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover,
.stat-card.clickable:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

.section-row {
  margin-bottom: 16px;
}

.section-header {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.action-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.action-item:hover {
  background: #f5f5f5;
}

.action-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.action-info {
  flex: 1;
}

.action-title {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.action-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 14px;
  color: #666;
}

.info-value {
  font-size: 14px;
  color: #1a1a1a;
}
</style>
