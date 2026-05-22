<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAuditLogs, fetchCounselors } from '../api/admin'

const activeTab = ref('counselors')

// 辅导员账户管理
const counselors = ref([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref(null)
const form = ref({ realName: '', studentNo: '', phone: '', password: '' })
const pendingCounselorCount = computed(() => counselors.value.filter((item) => item.status !== 1).length)

async function loadCounselors() {
  loading.value = true
  try {
    const res = await fetchCounselors()
    counselors.value = res || []
  } catch (e) {
    ElMessage.error('加载辅导员列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { realName: '', studentNo: '', phone: '', password: '' }
  dialog.value = true
}

function openEdit(c) {
  editing.value = c
  form.value = { realName: c.realName, studentNo: c.studentNo, phone: c.phone || '', password: '' }
  dialog.value = true
}

function normalizeJobNo(value) {
  form.value.studentNo = String(value || '').replace(/\D/g, '')
}

async function save() {
  if (!form.value.realName || !form.value.studentNo) {
    ElMessage.warning('请填写姓名和工号')
    return
  }
  if (!/^\d+$/.test(form.value.studentNo)) {
    ElMessage.warning('辅导员工号只能填写数字')
    return
  }
  try {
    const http = (await import('../api/http')).default
    if (editing.value) {
      await http.put(`/api/admin/counselors/${editing.value.id}`, {
        realName: form.value.realName,
        phone: form.value.phone,
        password: form.value.password || undefined,
      })
      ElMessage.success('更新成功')
    } else {
      if (!form.value.password) {
        ElMessage.warning('请设置密码')
        return
      }
      await http.post('/api/admin/counselors', {
        realName: form.value.realName,
        studentNo: form.value.studentNo,
        phone: form.value.phone,
        password: form.value.password,
      })
      ElMessage.success('新增成功')
    }
    dialog.value = false
    await loadCounselors()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该辅导员账号？', '确认删除', { type: 'warning' })
    const http = (await import('../api/http')).default
    await http.delete(`/api/admin/counselors/${id}`)
    ElMessage.success('已删除')
    await loadCounselors()
  } catch (_) {}
}

async function handleApprove(id) {
  try {
    await ElMessageBox.confirm('确认通过该辅导员注册申请？', '审核通过', { type: 'warning' })
    const http = (await import('../api/http')).default
    await http.post(`/api/admin/counselors/${id}/approve`)
    ElMessage.success('已审核通过')
    await loadCounselors()
  } catch (_) {}
}

// 审计日志
const auditLogs = ref([])
const auditTotal = ref(0)
const auditLoading = ref(false)
const auditQuery = ref({ module: '', action: '', pageNum: 1, pageSize: 20 })
const modules = ['user', 'knowledge', 'party', 'notice', 'admin']
const actions = ['create', 'update', 'delete', 'login', 'logout']

async function loadAuditLogs() {
  auditLoading.value = true
  try {
    const res = await fetchAuditLogs({
      module: auditQuery.value.module || undefined,
      action: auditQuery.value.action || undefined,
      pageNum: auditQuery.value.pageNum,
      pageSize: auditQuery.value.pageSize,
    })
    auditLogs.value = res.records
    auditTotal.value = res.total
  } catch (e) {
    ElMessage.error('加载审计日志失败')
  } finally {
    auditLoading.value = false
  }
}

function onPageChange(page) {
  auditQuery.value.pageNum = page
  loadAuditLogs()
}

onMounted(() => {
  loadCounselors()
  loadAuditLogs()
})
</script>

<template>
  <div class="settings-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <el-icon size="18" color="#1677ff"><Setting /></el-icon>
          <span>系统设置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane name="counselors">
          <template #label>
            <span>辅导员账户管理</span>
            <el-badge v-if="pendingCounselorCount" :value="pendingCounselorCount" class="tab-badge" />
          </template>
          <div class="toolbar">
            <el-button type="primary" size="small" @click="openCreate">
              <el-icon><Plus /></el-icon> 新增辅导员
            </el-button>
            <el-tag v-if="pendingCounselorCount" type="warning" effect="plain">
              {{ pendingCounselorCount }} 个注册申请待审核
            </el-tag>
          </div>
          <el-table :data="counselors" v-loading="loading" stripe empty-text="暂无辅导员账户">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="studentNo" label="工号" width="120" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                  {{ row.status === 1 ? '正常' : '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status !== 1" type="success" link @click="handleApprove(row.id)">通过</el-button>
                <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="审计日志" name="audit">
          <div class="toolbar">
            <el-select v-model="auditQuery.module" placeholder="全部模块" size="small" clearable style="width: 140px" @change="loadAuditLogs">
              <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
            </el-select>
            <el-select v-model="auditQuery.action" placeholder="全部操作" size="small" clearable style="width: 140px" @change="loadAuditLogs">
              <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
            </el-select>
            <el-button size="small" @click="loadAuditLogs">刷新</el-button>
          </div>
          <el-table :data="auditLogs" v-loading="auditLoading" stripe empty-text="暂无审计日志">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userName" label="操作人" width="100" />
            <el-table-column prop="module" label="模块" width="90" />
            <el-table-column prop="action" label="操作" width="80" />
            <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
            <el-table-column prop="ipAddress" label="IP" width="140" />
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
          <div class="pagination" v-if="auditTotal > auditQuery.pageSize">
            <el-pagination
              v-model:current-page="auditQuery.pageNum"
              :page-size="auditQuery.pageSize"
              :total="auditTotal"
              layout="prev, pager, next"
              @current-change="onPageChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="dialog"
      :title="editing ? '编辑辅导员' : '新增辅导员'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" placeholder="辅导员姓名" />
        </el-form-item>
        <el-form-item label="工号" required :disabled="!!editing">
          <el-input v-model="form.studentNo" placeholder="数字辅导员工号" @input="normalizeJobNo" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item :label="editing ? '新密码' : '密码'" :required="!editing">
          <el-input v-model="form.password" type="password" :placeholder="editing ? '留空则不修改' : '设置密码'" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 1200px;
}

.header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.tab-badge {
  margin-left: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
