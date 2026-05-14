<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchReviewList } from '../api/leave'

const loading = ref(false)
const activeStatus = ref(2)
const rows = ref([])
const router = useRouter()

async function loadData() {
  loading.value = true
  try {
    rows.value = await fetchReviewList(activeStatus.value)
  } catch (error) {
    ElMessage.error(error.message || '加载已处理列表失败')
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
          <el-icon size="18" color="#52c41a"><Select /></el-icon>
          <span>已处理申请</span>
        </div>
        <div class="actions">
          <el-select v-model="activeStatus" size="small" style="width: 120px" @change="loadData">
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
          </el-select>
          <el-button size="small" @click="loadData" :icon="'Refresh'">刷新</el-button>
        </div>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" stripe empty-text="暂无已处理申请">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column label="申请人" min-width="140">
        <template #default="{ row }">{{ row.applicantName }}（{{ row.applicantStudentNo }}）</template>
      </el-table-column>
      <el-table-column prop="title" label="申请标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 2" type="success" size="small">已通过</el-tag>
          <el-tag v-else type="danger" size="small">已驳回</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" min-width="170" />
      <el-table-column label="详情" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="goDetail(row.id)">查看</el-button>
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

.actions {
  display: flex;
  gap: 8px;
}
</style>
