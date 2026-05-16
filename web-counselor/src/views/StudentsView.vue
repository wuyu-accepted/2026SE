<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchStudents } from '../api/student'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const selected = ref(null)
const drawerVisible = ref(false)

const query = reactive({
  keyword: '',
  grade: '',
  major: '',
  className: '',
  authType: '',
  pageNum: 1,
  pageSize: 10,
})

const summary = computed(() => {
  const cadreCount = rows.value.filter((item) => item.authType === 'cadre').length
  const activeCount = rows.value.filter((item) => item.status === 1).length
  const grades = new Set(rows.value.map((item) => item.grade).filter(Boolean))
  return [
    { label: '当前页学生', value: rows.value.length },
    { label: '账号正常', value: activeCount },
    { label: '学生骨干', value: cadreCount },
    { label: '涉及年级', value: grades.size },
  ]
})

function authTypeLabel(value) {
  if (value === 'cadre') {
    return '学生骨干'
  }
  return '普通学生'
}

function genderLabel(value) {
  if (value === 1) {
    return '男'
  }
  if (value === 2) {
    return '女'
  }
  return '未填写'
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchStudents({
      keyword: query.keyword || undefined,
      grade: query.grade || undefined,
      major: query.major || undefined,
      className: query.className || undefined,
      authType: query.authType || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    rows.value = result.records || []
    total.value = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '加载学生列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadData()
}

function reset() {
  query.keyword = ''
  query.grade = ''
  query.major = ''
  query.className = ''
  query.authType = ''
  query.pageNum = 1
  loadData()
}

function handleSizeChange(size) {
  query.pageSize = size
  query.pageNum = 1
  loadData()
}

function openDetail(row) {
  selected.value = row
  drawerVisible.value = true
}

onMounted(loadData)
</script>

<template>
  <div class="students-page">
    <el-row :gutter="12" class="summary-row">
      <el-col v-for="item in summary" :key="item.label" :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-value">{{ item.value }}</div>
          <div class="summary-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon color="#1677ff"><User /></el-icon>
            <span>学生信息列表</span>
            <el-tag size="small">{{ total }} 人</el-tag>
          </div>
          <el-button size="small" :icon="'Refresh'" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form class="filters" :model="query" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="姓名/学号/手机号"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="query.grade" clearable placeholder="如 2023" style="width: 120px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="query.major" clearable placeholder="专业" style="width: 160px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="query.className" clearable placeholder="班级" style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="query.authType" clearable placeholder="全部" style="width: 130px">
            <el-option label="普通学生" value="student" />
            <el-option label="学生骨干" value="cadre" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无学生信息">
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="身份" width="110">
          <template #default="{ row }">
            <el-tag :type="row.authType === 'cadre' ? 'warning' : 'info'" effect="plain">
              {{ authTypeLabel(row.authType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="年级" width="90" />
        <el-table-column prop="major" label="专业" min-width="150" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" min-width="130" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="politicalStatus" label="政治面貌" width="120" />
        <el-table-column label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
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

    <el-drawer v-model="drawerVisible" title="学生详情" size="420px">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item label="姓名">{{ selected.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ selected.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderLabel(selected.gender) }}</el-descriptions-item>
        <el-descriptions-item label="身份">{{ authTypeLabel(selected.authType) }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ selected.grade || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ selected.major || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ selected.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ selected.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ selected.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="政治面貌">{{ selected.politicalStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="生源地">{{ selected.hometown || '-' }}</el-descriptions-item>
        <el-descriptions-item label="宿舍">{{ selected.dormitory || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style scoped>
.students-page {
  max-width: 1280px;
}

.summary-row {
  margin-bottom: 12px;
}

.summary-card {
  border-radius: 8px;
}

.summary-card :deep(.el-card__body) {
  padding: 16px 18px;
}

.summary-value {
  font-size: 24px;
  line-height: 1.2;
  font-weight: 700;
  color: #1f2d3d;
}

.summary-label {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.card-header,
.card-title {
  display: flex;
  align-items: center;
}

.card-header {
  justify-content: space-between;
}

.card-title {
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.filters {
  padding: 4px 0 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
