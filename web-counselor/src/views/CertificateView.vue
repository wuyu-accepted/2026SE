<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/http'

const list = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const currentItem = ref(null)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')

const statusMap = { 0: '待审批', 1: '审批中', 2: '已通过', 3: '已驳回' }
const statusType = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger' }

async function fetchList(status) {
  loading.value = true
  try {
    const params = status !== undefined ? { status } : {}
    const res = await request.get('/api/admin/certificates', { params })
    list.value = res || []
  } catch (e) {
    ElMessage.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  currentItem.value = row
  detailVisible.value = true
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确定通过「${row.title}」？`, '提示')
    await request.post(`/api/admin/certificates/${row.id}/approve`)
    ElMessage.success('已通过')
    detailVisible.value = false
    await fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

function openReject(row) {
  currentItem.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await request.post(`/api/admin/certificates/${currentItem.value.id}/reject`, { rejectReason: rejectReason.value.trim() })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    detailVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => fetchList())
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2>电子证明审批</h2>
    </div>

    <el-table :data="list" v-loading="loading" stripe empty-text="暂无证明申请">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="证明标题" min-width="180" />
      <el-table-column prop="templateType" label="类型" width="120" />
      <el-table-column label="申请人" width="120">
        <template #default="{ row }">{{ row.userId }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status] || 'info'" size="small">{{ statusMap[row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDetail(row)">详情</el-button>
          <el-button v-if="row.status === 0" type="success" link @click="handleApprove(row)">通过</el-button>
          <el-button v-if="row.status === 0" type="danger" link @click="openReject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="证明详情" width="520px">
      <el-descriptions v-if="currentItem" :column="1" border>
        <el-descriptions-item label="标题">{{ currentItem.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ currentItem.templateType }}</el-descriptions-item>
        <el-descriptions-item label="理由">{{ currentItem.reason || '无' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusMap[currentItem.status] }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentItem.submitTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentItem.rejectReason" label="驳回原因">{{ currentItem.rejectReason }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentItem && currentItem.status === 0" type="success" @click="handleApprove(currentItem)">通过</el-button>
        <el-button v-if="currentItem && currentItem.status === 0" type="danger" @click="openReject(currentItem)">驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="驳回申请" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
</style>
