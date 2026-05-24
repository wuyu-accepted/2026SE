<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/http'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const form = ref({ title: '', studentName: '', studentNo: '', awardLevel: '', awardDate: '', description: '', category: '' })

const levelOptions = ['国家级', '省级', '校级', '院级', '其他']
const categoryOptions = ['奖学金', '竞赛', '学术', '社会实践', '志愿服务', '文体活动', '其他']

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/api/admin/honors')
    list.value = res || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.value = { title: '', studentName: '', studentNo: '', awardLevel: '', awardDate: '', description: '', category: '' }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = {
    title: row.title || '',
    studentName: row.studentName || '',
    studentNo: row.studentNo || '',
    awardLevel: row.awardLevel || '',
    awardDate: row.awardDate || '',
    description: row.description || '',
    category: row.category || '',
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.title || !form.value.studentName) {
    ElMessage.warning('请填写荣誉名称和学生姓名')
    return
  }
  try {
    if (editingId.value) {
      await request.put(`/api/admin/honors/${editingId.value}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/api/admin/honors', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」？`, '提示')
    await request.delete(`/api/admin/honors/${row.id}`)
    ElMessage.success('已删除')
    await fetchList()
  } catch (_) {}
}

onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2>奖励荣誉管理</h2>
      <el-button type="primary" @click="openCreate">添加荣誉</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe empty-text="暂无荣誉数据">
      <el-table-column prop="title" label="荣誉名称" min-width="200" />
      <el-table-column prop="studentName" label="获得者" width="120" />
      <el-table-column prop="awardLevel" label="级别" width="100" />
      <el-table-column prop="category" label="类别" width="100" />
      <el-table-column prop="awardDate" label="获奖时间" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '展示' : '隐藏' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑荣誉' : '添加荣誉'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="荣誉名称" required>
          <el-input v-model="form.title" placeholder="如：国家奖学金" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="获得者" required>
              <el-input v-model="form.studentName" placeholder="姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学号">
              <el-input v-model="form.studentNo" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="获奖级别">
              <el-select v-model="form.awardLevel" style="width:100%">
                <el-option v-for="o in levelOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别">
              <el-select v-model="form.category" style="width:100%">
                <el-option v-for="o in categoryOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="获奖时间">
          <el-input v-model="form.awardDate" placeholder="如：2026-05" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="荣誉详情" />
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
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
</style>
