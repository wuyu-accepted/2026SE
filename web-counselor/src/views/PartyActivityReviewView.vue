<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { approvePartyActivity, fetchPartyActivities, fetchPartyActivityDetail, rejectPartyActivity } from '../api/party'

const loading = ref(false)
const rows = ref([])
const statusFilter = ref(0)

const drawerOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const reviewForm = reactive({
  comment: '',
})
const reviewing = ref(false)

const statusLabel = computed(() => (s) => {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  if (s === 2) return '已驳回'
  return '未知'
})

async function loadData() {
  loading.value = true
  try {
    rows.value = await fetchPartyActivities({ status: statusFilter.value })
  } catch (error) {
    ElMessage.error(error.message || '加载党团活动申请失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(id) {
  drawerOpen.value = true
  detailLoading.value = true
  reviewForm.comment = ''
  try {
    detail.value = await fetchPartyActivityDetail(id)
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
    drawerOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

function closeDrawer() {
  drawerOpen.value = false
  detail.value = null
  reviewForm.comment = ''
}

async function doApprove() {
  if (!detail.value) {
    return
  }
  const comment = String(reviewForm.comment || '').trim()
  if (!comment) {
    ElMessage.error('请输入审批意见')
    return
  }
  reviewing.value = true
  try {
    await approvePartyActivity(detail.value.id, comment)
    ElMessage.success('已通过')
    closeDrawer()
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    reviewing.value = false
  }
}

async function doReject() {
  if (!detail.value) {
    return
  }
  const comment = String(reviewForm.comment || '').trim()
  if (!comment) {
    ElMessage.error('请输入驳回原因')
    return
  }
  reviewing.value = true
  try {
    await rejectPartyActivity(detail.value.id, comment)
    ElMessage.success('已驳回')
    closeDrawer()
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    reviewing.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <div class="header-left">
          <el-icon size="18" color="#1677ff"><Calendar /></el-icon>
          <span>党团活动审批</span>
        </div>
        <div class="header-right">
          <el-select v-model="statusFilter" style="width: 140px" @change="loadData">
            <el-option :value="0" label="待审核" />
            <el-option :value="1" label="已通过" />
            <el-option :value="2" label="已驳回" />
          </el-select>
          <el-button size="small" @click="loadData" :icon="'Refresh'">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" stripe empty-text="暂无党团活动申请">
      <el-table-column prop="id" label="编号" width="90" />
      <el-table-column label="学生" min-width="160">
        <template #default="{ row }">{{ row.realName }}（{{ row.studentNo }}）</template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column prop="eventDate" label="活动日期" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="row.status === 2" type="danger" size="small">已驳回</el-tag>
          <el-tag v-else size="small">未知</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row.id)">{{ statusFilter === 0 ? '审批' : '查看' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawerOpen" size="520px" :with-header="true" title="党团活动详情" @close="closeDrawer">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="学生">{{ detail.realName }}（{{ detail.studentNo }}）</el-descriptions-item>
            <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="事由">
              <div class="content">{{ detail.reason }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="活动日期">{{ detail.eventDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.status !== 0" label="审核意见">{{ detail.reviewComment || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="detail.status === 0" class="review">
            <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见/驳回原因" />
            <div class="actions">
              <el-button type="danger" :loading="reviewing" @click="doReject">驳回</el-button>
              <el-button type="primary" :loading="reviewing" @click="doApprove">通过</el-button>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
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

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review {
  margin-top: 16px;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.content {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
