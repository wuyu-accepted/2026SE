<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createKnowledgeArticle,
  createKnowledgeCategory,
  createKnowledgeTemplate,
  deleteKnowledgeArticle,
  deleteKnowledgeTemplate,
  fetchKnowledgeArticles,
  fetchKnowledgeArticleDetail,
  fetchKnowledgeCategories,
  fetchKnowledgeStats,
  fetchKnowledgeTemplates,
  previewKnowledgeArticle,
  sourceDownloadUrl,
  updateKnowledgeArticle,
  updateKnowledgeArticleStatus,
  updateKnowledgeCategory,
  updateKnowledgeTemplate,
  updateKnowledgeTemplateStatus,
  uploadKnowledgeTemplate,
  uploadKnowledgeFile,
  uploadKnowledgeImage,
} from '../api/knowledge'

const activeTab = ref('articles')
const loading = ref(false)
const articleDialogVisible = ref(false)
const articlePreviewHtml = ref('')
const templateDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const editingArticleId = ref(null)
const editingTemplateId = ref(null)
const editingCategoryId = ref(null)
const articles = ref([])
const templates = ref([])
const categories = ref([])
const stats = ref({})
const articleTotal = ref(0)

const articleQuery = reactive({ keyword: '', status: null, contentType: '', pageNum: 1, pageSize: 10 })
const templateQuery = reactive({ keyword: '', status: null, category: '' })

const emptyArticle = () => ({
  title: '',
  summary: '',
  source: '',
  fileId: null,
  contentMode: 'file',
  editorType: 'markdown',
  sourceContent: '',
  categoryId: null,
  contentType: 'guide',
  tags: '',
  targetGrades: '',
  targetMajors: '',
  targetPoliticalStatuses: '',
  targetPartyStages: '',
  scenarioCodes: '',
  priority: 0,
  status: 0,
})
const emptyTemplate = () => ({
  name: '',
  description: '',
  category: '',
  fileId: null,
  format: '',
  tags: '',
  targetGrades: '',
  targetMajors: '',
  targetPoliticalStatuses: '',
  targetPartyStages: '',
  scenarioCodes: '',
  priority: 0,
  status: 1,
})
const emptyCategory = () => ({ name: '', code: '', sortOrder: 0, status: 1 })

const articleForm = reactive(emptyArticle())
const templateForm = reactive(emptyTemplate())
const categoryForm = reactive(emptyCategory())

const categoryOptions = computed(() => categories.value.filter(item => item.status !== 0))

function resetReactive(target, source) {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source)
}

async function loadArticles() {
  loading.value = true
  try {
    const data = await fetchKnowledgeArticles(articleQuery)
    articles.value = data.records || []
    articleTotal.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  loading.value = true
  try {
    templates.value = await fetchKnowledgeTemplates(templateQuery)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categories.value = await fetchKnowledgeCategories()
}

async function loadStats() {
  stats.value = await fetchKnowledgeStats()
}

function openCreateArticle() {
  editingArticleId.value = null
  resetReactive(articleForm, emptyArticle())
  articlePreviewHtml.value = ''
  articleDialogVisible.value = true
}

async function openEditArticle(row) {
  editingArticleId.value = row.id
  articleDialogVisible.value = true
  articlePreviewHtml.value = ''
  const detail = await fetchKnowledgeArticleDetail(row.id)
  resetReactive(articleForm, { ...emptyArticle(), ...detail })
  articlePreviewHtml.value = detail.renderedContent || ''
}

async function saveArticle() {
  if (!articleForm.title) {
    ElMessage.warning('请填写标题')
    return
  }
  if (articleForm.contentMode === 'file' && !articleForm.fileId) {
    ElMessage.warning('请上传资料文件')
    return
  }
  if (articleForm.contentMode === 'editor' && !articleForm.sourceContent) {
    ElMessage.warning('请填写在线编排内容')
    return
  }
  if (editingArticleId.value) {
    await updateKnowledgeArticle(editingArticleId.value, articleForm)
  } else {
    await createKnowledgeArticle(articleForm)
  }
  ElMessage.success('资料已保存')
  articleDialogVisible.value = false
  loadArticles()
}

async function changeArticleStatus(row, status) {
  await updateKnowledgeArticleStatus(row.id, status)
  ElMessage.success(status === 1 ? '已发布' : '状态已更新')
  loadArticles()
}

async function removeArticle(row) {
  await ElMessageBox.confirm(`确认删除资料“${row.title}”？`, '删除确认', { type: 'warning' })
  await deleteKnowledgeArticle(row.id)
  ElMessage.success('已删除')
  loadArticles()
}

function downloadSource(row) {
  window.open(sourceDownloadUrl(row.id), '_blank')
}

function openCreateTemplate() {
  editingTemplateId.value = null
  resetReactive(templateForm, emptyTemplate())
  templateDialogVisible.value = true
}

function openEditTemplate(row) {
  editingTemplateId.value = row.id
  resetReactive(templateForm, { ...emptyTemplate(), ...row })
  templateDialogVisible.value = true
}

async function saveTemplate() {
  if (!templateForm.name) {
    ElMessage.warning('请填写模板名称')
    return
  }
  if (editingTemplateId.value) {
    await updateKnowledgeTemplate(editingTemplateId.value, templateForm)
  } else {
    await createKnowledgeTemplate(templateForm)
  }
  ElMessage.success('模板已保存')
  templateDialogVisible.value = false
  loadTemplates()
}

async function handleTemplateUpload(options) {
  const result = await uploadKnowledgeTemplate(options.file)
  templateForm.fileId = result.id
  templateForm.format = (options.file.name.split('.').pop() || '').toUpperCase()
  ElMessage.success('文件已上传')
}

async function handleKnowledgeFileUpload(options) {
  const result = await uploadKnowledgeFile(options.file)
  articleForm.fileId = result.id
  ElMessage.success('资料文件已上传')
}

async function handleKnowledgeImageUpload(options) {
  const result = await uploadKnowledgeImage(options.file)
  const marker = articleForm.editorType === 'latex'
    ? `\\includegraphics{file:${result.id}}`
    : `![图片](file:${result.id})`
  articleForm.sourceContent = `${articleForm.sourceContent || ''}\n${marker}`
  ElMessage.success('图片已插入编辑内容')
}

async function previewArticle() {
  const result = await previewKnowledgeArticle(articleForm)
  articlePreviewHtml.value = result.renderedContent || ''
}

async function changeTemplateStatus(row, status) {
  await updateKnowledgeTemplateStatus(row.id, status)
  ElMessage.success('状态已更新')
  loadTemplates()
}

async function removeTemplate(row) {
  await ElMessageBox.confirm(`确认删除模板“${row.name}”？`, '删除确认', { type: 'warning' })
  await deleteKnowledgeTemplate(row.id)
  ElMessage.success('已删除')
  loadTemplates()
}

function openCreateCategory() {
  editingCategoryId.value = null
  resetReactive(categoryForm, emptyCategory())
  categoryDialogVisible.value = true
}

function openEditCategory(row) {
  editingCategoryId.value = row.id
  resetReactive(categoryForm, { ...emptyCategory(), ...row })
  categoryDialogVisible.value = true
}

async function saveCategory() {
  if (!categoryForm.name || !categoryForm.code) {
    ElMessage.warning('请填写分类名称和编码')
    return
  }
  if (editingCategoryId.value) {
    await updateKnowledgeCategory(editingCategoryId.value, categoryForm)
  } else {
    await createKnowledgeCategory(categoryForm)
  }
  ElMessage.success('分类已保存')
  categoryDialogVisible.value = false
  loadCategories()
}

function extractStatusText(status) {
  const labels = { success: '已索引', empty: '无可索引文本', failed: '解析失败', unsupported: '暂不支持', editor: '在线内容' }
  return labels[status] || '待索引'
}

function statusText(status) {
  if (status === 1) return '已发布/启用'
  if (status === 2) return '已下架'
  return '草稿/禁用'
}

onMounted(async () => {
  await loadCategories()
  loadArticles()
  loadTemplates()
  loadStats()
})
</script>

<template>
  <div class="knowledge-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <div class="title">知识库管理</div>
            <div class="subtitle">维护政策、流程、FAQ 与模板，并配置智能推荐适用对象。</div>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="资料管理" name="articles">
          <div class="toolbar">
            <el-input v-model="articleQuery.keyword" placeholder="搜索标题/摘要/标签/文件全文" clearable style="width: 240px" />
            <el-select v-model="articleQuery.contentType" placeholder="内容类型" clearable style="width: 150px">
              <el-option label="政策" value="policy" />
              <el-option label="流程" value="process" />
              <el-option label="问答" value="faq" />
              <el-option label="指南" value="guide" />
            </el-select>
            <el-button type="primary" @click="loadArticles">查询</el-button>
            <el-button type="success" @click="openCreateArticle">新增资料</el-button>
          </div>
          <el-table v-loading="loading" :data="articles" border>
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="contentType" label="类型" width="100" />
            <el-table-column prop="tags" label="标签" min-width="150" show-overflow-tooltip />
            <el-table-column prop="targetGrades" label="年级" width="120" />
            <el-table-column prop="targetPartyStages" label="党团阶段" width="140" />
            <el-table-column prop="priority" label="优先级" width="90" />
            <el-table-column label="全文索引" width="110">
              <template #default="{ row }">{{ extractStatusText(row.extractStatus) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">{{ statusText(row.status) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditArticle(row)">编辑</el-button>
                <el-button size="small" type="success" @click="changeArticleStatus(row, 1)">发布</el-button>
                <el-button size="small" type="warning" @click="changeArticleStatus(row, 2)">下架</el-button>
                <el-button v-if="row.contentMode === 'editor'" size="small" @click="downloadSource(row)">源文件</el-button>
                <el-button size="small" type="danger" @click="removeArticle(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            layout="prev, pager, next, total"
            :total="articleTotal"
            :page-size="articleQuery.pageSize"
            v-model:current-page="articleQuery.pageNum"
            @current-change="loadArticles"
          />
        </el-tab-pane>

        <el-tab-pane label="模板管理" name="templates">
          <div class="toolbar">
            <el-input v-model="templateQuery.keyword" placeholder="搜索模板名称" clearable style="width: 240px" />
            <el-button type="primary" @click="loadTemplates">查询</el-button>
            <el-button type="success" @click="openCreateTemplate">新增模板</el-button>
          </div>
          <el-table :data="templates" border>
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="format" label="格式" width="90" />
            <el-table-column prop="fileId" label="文件ID" width="100" />
            <el-table-column prop="tags" label="标签" min-width="140" show-overflow-tooltip />
            <el-table-column prop="downloadCount" label="下载" width="80" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditTemplate(row)">编辑</el-button>
                <el-button size="small" type="success" @click="changeTemplateStatus(row, 1)">启用</el-button>
                <el-button size="small" type="warning" @click="changeTemplateStatus(row, 0)">禁用</el-button>
                <el-button size="small" type="danger" @click="removeTemplate(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="分类管理" name="categories">
          <div class="toolbar">
            <el-button type="success" @click="openCreateCategory">新增分类</el-button>
          </div>
          <el-table :data="categories" border>
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="code" label="编码" />
            <el-table-column prop="sortOrder" label="排序" width="90" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="openEditCategory(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="统计概览" name="stats">
          <div class="stats-grid">
            <el-statistic title="资料数" :value="stats.articleCount || 0" />
            <el-statistic title="模板数" :value="stats.templateCount || 0" />
            <el-statistic title="分类数" :value="stats.categoryCount || 0" />
            <el-statistic title="行为事件" :value="stats.behaviorEventCount || 0" />
            <el-statistic title="推荐日志" :value="stats.recommendationLogCount || 0" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="articleDialogVisible" title="知识资料" width="760px">
      <el-form :model="articleForm" label-width="110px">
        <el-form-item label="标题"><el-input v-model="articleForm.title" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="articleForm.summary" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="articleForm.categoryId" clearable>
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容类型">
          <el-select v-model="articleForm.contentType">
            <el-option label="政策" value="policy" />
            <el-option label="流程" value="process" />
            <el-option label="问答" value="faq" />
            <el-option label="指南" value="guide" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布方式">
          <el-radio-group v-model="articleForm.contentMode">
            <el-radio-button label="file">上传文件</el-radio-button>
            <el-radio-button label="editor">在线编排</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="articleForm.tags" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="适用年级"><el-input v-model="articleForm.targetGrades" placeholder="如 2023,2024" /></el-form-item>
        <el-form-item label="适用专业"><el-input v-model="articleForm.targetMajors" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="政治面貌"><el-input v-model="articleForm.targetPoliticalStatuses" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="党团阶段"><el-input v-model="articleForm.targetPartyStages" placeholder="如 activist,applicant" /></el-form-item>
        <el-form-item label="场景编码"><el-input v-model="articleForm.scenarioCodes" placeholder="如 leave,aid,report" /></el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="articleForm.priority" :min="0" /></el-form-item>
        <el-form-item v-if="articleForm.contentMode === 'file'" label="资料文件">
          <el-upload :http-request="handleKnowledgeFileUpload" :show-file-list="false">
            <el-button>上传资料文件</el-button>
          </el-upload>
          <span class="file-id">文件ID：{{ articleForm.fileId || '-' }}</span>
          <div v-if="articleForm.extractStatus" class="extract-tip">全文索引：{{ extractStatusText(articleForm.extractStatus) }}{{ articleForm.extractError ? `（${articleForm.extractError}）` : '' }}</div>
        </el-form-item>
        <template v-else>
          <el-form-item label="编排格式">
            <el-radio-group v-model="articleForm.editorType">
              <el-radio-button label="markdown">Markdown</el-radio-button>
              <el-radio-button label="latex">LaTeX</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="插入图片">
            <el-upload :http-request="handleKnowledgeImageUpload" :show-file-list="false" accept="image/*">
              <el-button>上传并插入图片</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="源文案">
            <el-input v-model="articleForm.sourceContent" type="textarea" :rows="10" placeholder="输入 Markdown 或 LaTeX 源文案" />
          </el-form-item>
          <el-form-item label="在线预览">
            <div class="preview-actions">
              <el-button @click="previewArticle">编译/预览</el-button>
              <el-button v-if="editingArticleId" @click="downloadSource({ id: editingArticleId })">下载可编辑源文件</el-button>
            </div>
            <div class="article-preview-shell">
              <div class="article-preview" v-html="articlePreviewHtml || '暂无预览'" />
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="articleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveArticle">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialogVisible" title="知识模板" width="680px">
      <el-form :model="templateForm" label-width="110px">
        <el-form-item label="名称"><el-input v-model="templateForm.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="templateForm.description" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="templateForm.category" /></el-form-item>
        <el-form-item label="上传文件">
          <el-upload :http-request="handleTemplateUpload" :show-file-list="false">
            <el-button>上传模板</el-button>
          </el-upload>
          <span class="file-id">文件ID：{{ templateForm.fileId || '-' }}</span>
        </el-form-item>
        <el-form-item label="格式"><el-input v-model="templateForm.format" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="templateForm.tags" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="适用年级"><el-input v-model="templateForm.targetGrades" /></el-form-item>
        <el-form-item label="适用专业"><el-input v-model="templateForm.targetMajors" /></el-form-item>
        <el-form-item label="政治面貌"><el-input v-model="templateForm.targetPoliticalStatuses" /></el-form-item>
        <el-form-item label="党团阶段"><el-input v-model="templateForm.targetPartyStages" /></el-form-item>
        <el-form-item label="场景编码"><el-input v-model="templateForm.scenarioCodes" /></el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="templateForm.priority" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" title="知识分类" width="480px">
      <el-form :model="categoryForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="categoryForm.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="categoryForm.code" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.knowledge-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 18px; font-weight: 700; color: #1f2937; }
.subtitle { margin-top: 6px; color: #64748b; font-size: 13px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.pager { margin-top: 14px; justify-content: flex-end; }
.stats-grid { display: grid; grid-template-columns: repeat(3, minmax(160px, 1fr)); gap: 16px; }
.file-id { margin-left: 12px; color: #64748b; font-size: 13px; }
.extract-tip { margin-top: 8px; color: #64748b; font-size: 13px; }
.preview-actions { display: flex; gap: 10px; margin-bottom: 10px; }
.article-preview-shell {
  width: 100%;
  min-height: 220px;
  padding: 18px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #f3f4f6;
}
.article-preview {
  max-width: 760px;
  min-height: 180px;
  margin: 0 auto;
  padding: 28px 34px;
  border-radius: 4px;
  background: #ffffff;
  color: #1f2937;
  line-height: 1.75;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}
.article-preview :deep(h1) { margin: 0 0 16px; font-size: 24px; text-align: center; }
.article-preview :deep(h2) { margin: 18px 0 10px; font-size: 20px; }
.article-preview :deep(p) { margin: 8px 0; }
.article-preview :deep(img) { display: block; max-width: 100%; margin: 12px auto; }
</style>
