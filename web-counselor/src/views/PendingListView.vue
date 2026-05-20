<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchReviewList } from '../api/leave'

const loading = ref(false)
const rows = ref([])
const router = useRouter()

async function loadData() {
  loading.value = true
  try {
    rows.value = await fetchReviewList(0)
  } catch (error) {
    ElMessage.error(error.message || '加载待审批失败')
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/review/${id}`)
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <div class="header-left">
          <el-icon size="18" color="#1677ff"><Clock /></el-icon>
          <span>待审批申请</span>
          <el-tag v-if="rows.length" size="small" type="danger">{{ rows.length }} 条</el-tag>
        </div>
        <el-button size="small" @click="loadData" :icon="'Refresh'">刷新</el-button>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" stripe empty-text="暂无待审批申请">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column label="申请人" min-width="140">
        <template #default="{ row }">{{ row.applicantName }}（{{ row.applicantStudentNo }}）</template>
      </el-table-column>
      <el-table-column prop="title" label="申请标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="leaveStartDate" label="开始" width="110" />
      <el-table-column prop="leaveEndDate" label="结束" width="110" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="goDetail(row.id)">审批</el-button>
        </template>
      </el-table-column>
    </el-table>
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
</style>
