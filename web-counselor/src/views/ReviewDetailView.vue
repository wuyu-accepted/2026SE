<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { approveLeave, fetchReviewDetail, rejectLeave } from '../api/leave'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const detail = ref(null)
const comment = ref('')

const canReview = computed(() => detail.value && detail.value.status === 0)

const statusMap = { 0: { label: '待审批', type: 'warning' }, 1: { label: '审批中', type: 'primary' }, 2: { label: '已通过', type: 'success' }, 3: { label: '已驳回', type: 'danger' } }
const statusInfo = computed(() => statusMap[detail.value?.status] ?? {})

async function loadData() {
  loading.value = true
  try {
    detail.value = await fetchReviewDetail(route.params.id)
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
  } finally {
    loading.value = false
  }
}

async function doApprove() {
  if (!comment.value.trim()) {
    ElMessage.warning('请先填写审批意见')
    return
  }
  submitting.value = true
  try {
    await approveLeave(route.params.id, comment.value.trim())
    ElMessage.success('已审批通过')
    await loadData()
    comment.value = ''
  } catch (error) {
    ElMessage.error(error.message || '审批失败')
  } finally {
    submitting.value = false
  }
}

async function doReject() {
  if (!comment.value.trim()) {
    ElMessage.warning('请先填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await rejectLeave(route.params.id, comment.value.trim())
    ElMessage.success('已驳回申请')
    await loadData()
    comment.value = ''
  } catch (error) {
    ElMessage.error(error.message || '驳回失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card v-loading="loading" shadow="never">
    <template #header>
      <div class="header">
        <div class="header-left">
          <el-icon size="18" color="#1677ff"><Document /></el-icon>
          <span>申请详情</span>
          <el-tag v-if="statusInfo.label" :type="statusInfo.type" size="small" effect="dark">{{ statusInfo.label }}</el-tag>
        </div>
        <el-button @click="router.back()" :icon="'ArrowLeft'" size="small">返回</el-button>
      </div>
    </template>

    <template v-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请人">{{ detail.applicantName }}（{{ detail.applicantStudentNo }}）</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="请假日期" :span="2">
          {{ detail.leaveStartDate }} ~ {{ detail.leaveEndDate }}
        </el-descriptions-item>
        <el-descriptions-item label="申请标题" :span="2">
          {{ detail.title }}
        </el-descriptions-item>
        <el-descriptions-item label="请假事由" :span="2">
          {{ detail.reason }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.submitTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">
          <span style="color: #ff4d4f">{{ detail.rejectReason }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="timeline" v-if="detail.timelines && detail.timelines.length">
        <h4 class="section-title">
          <el-icon><List /></el-icon>
          审批时间线
        </h4>
        <el-timeline>
          <el-timeline-item
            v-for="item in detail.timelines"
            :key="`${item.action}-${item.createdAt}`"
            :timestamp="item.createdAt"
            :type="item.action === 'approve' ? 'success' : item.action === 'reject' ? 'danger' : 'primary'"
          >
            <span :style="{ color: item.action === 'approve' ? '#52c41a' : item.action === 'reject' ? '#ff4d4f' : '#1677ff' }">
              {{ item.actionText }}
            </span>
            · {{ item.operatorName }}
            <template v-if="item.comment">· {{ item.comment }}</template>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div class="review-box" v-if="canReview">
        <h4 class="section-title">
          <el-icon><Edit /></el-icon>
          审批操作
        </h4>
        <el-input
          v-model="comment"
          type="textarea"
          :rows="4"
          placeholder="请输入审批意见或驳回原因…"
        />
        <div class="ops">
          <el-button type="success" :loading="submitting" @click="doApprove" size="large">
            <el-icon><CircleCheck /></el-icon> 通过
          </el-button>
          <el-button type="danger" :loading="submitting" @click="doReject" size="large">
            <el-icon><CircleClose /></el-icon> 驳回
          </el-button>
        </div>
      </div>
    </template>
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
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #1a1a1a;
  margin: 0 0 12px;
}

.timeline {
  margin-top: 24px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.review-box {
  margin-top: 24px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.ops {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}
</style>
