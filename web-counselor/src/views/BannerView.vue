<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/http'

const banners = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = ref({
  title: '',
  subtitle: '',
  targetType: 'none',
  targetId: null,
  targetPath: '',
  sortOrder: 0,
})

const targetTypeOptions = [
  { value: 'none', label: '仅展示' },
  { value: 'knowledge', label: '知识文章' },
  { value: 'notice', label: '通知公告' },
]

async function fetchBanners() {
  loading.value = true
  try {
    const res = await request.get('/api/admin/banners')
    banners.value = res || []
  } catch (e) {
    ElMessage.error('加载轮播图列表失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.value = { title: '', subtitle: '', targetType: 'none', targetId: null, targetPath: '', sortOrder: 0 }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = {
    title: row.title || '',
    subtitle: row.subtitle || '',
    targetType: row.targetType || 'none',
    targetId: row.targetId || null,
    targetPath: row.targetPath || '',
    sortOrder: row.sortOrder || 0,
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  try {
    if (editingId.value) {
      await request.put(`/api/admin/banners/${editingId.value}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/api/admin/banners', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchBanners()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除轮播图「${row.title}」？`, '提示')
    await request.delete(`/api/admin/banners/${row.id}`)
    ElMessage.success('删除成功')
    await fetchBanners()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(fetchBanners)
</script>

<template>
  <div class="banner-page">
    <div class="page-header">
      <h2>首页轮播图管理</h2>
      <el-button type="primary" @click="openCreate">添加轮播图</el-button>
    </div>

    <el-table :data="banners" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="subtitle" label="副标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="targetType" label="跳转类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ targetTypeOptions.find(o => o.value === row.targetType)?.label || row.targetType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetId" label="目标ID" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑轮播图' : '添加轮播图'" width="560px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="显示在首页轮播图上" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="500" show-word-limit placeholder="简要描述" />
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.targetType" style="width:100%">
            <el-option v-for="o in targetTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID" v-if="form.targetType !== 'none'">
          <el-input-number v-model="form.targetId" :min="1" style="width:100%" placeholder="输入知识文章或通知的ID" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.banner-page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
}
</style>
