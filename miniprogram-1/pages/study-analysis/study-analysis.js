const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

const CATEGORY_OPTIONS = [
  '通识模块',
  '专业模块',
  '创新训练与科学研究',
  '素质拓展与发展指导',
]

Page({
  data: {
    loading: false,
    saving: false,
    courseName: '',
    creditsText: '',
    categoryIndex: 1,
    categoryOptions: CATEGORY_OPTIONS,
    records: [],
    summary: null,
    importText: '',
    importCategoryIndex: 1,
    missing: null,
    activeModule: '',
    moduleDetail: null,
    activeElectiveKey: '',
    electiveDetail: null,
  },

  onShow() {
    this.refreshAll()
  },

  async refreshAll() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const [records, summary] = await Promise.all([
        request({ url: '/api/study-analysis/me/records' }).catch(() => []),
        request({ url: '/api/study-analysis/me/summary' }).catch(() => null),
      ])
      this.setData({ records, summary })
      this.loadMissingCourses()
    } finally {
      this.setData({ loading: false })
    }
  },

  onCourseNameInput(e) {
    this.setData({ courseName: e.detail.value || '' })
  },

  onCreditsInput(e) {
    this.setData({ creditsText: e.detail.value || '' })
  },

  onCategoryChange(e) {
    this.setData({ categoryIndex: Number(e.detail.value || 0) })
  },

  async onAddCourse() {
    const courseName = String(this.data.courseName || '').trim()
    const category = CATEGORY_OPTIONS[this.data.categoryIndex] || '专业模块'
    const creditsText = String(this.data.creditsText || '').trim()
    if (!courseName) {
      wx.showToast({ title: '请输入课程名称', icon: 'none' })
      return
    }
    if (this.data.saving) return

    const credits = this.parseCreditsOrNull(creditsText)
    if (creditsText && credits == null) {
      wx.showToast({ title: '学分格式不正确', icon: 'none' })
      return
    }

    this.setData({ saving: true })
    try {
      await ensureLogin()
      await request({
        url: '/api/study-analysis/me/records',
        method: 'POST',
        data: [{ courseName, category, credits }],
      })
      this.setData({ courseName: '', creditsText: '' })
      wx.showToast({ title: '已添加', icon: 'success' })
      this.refreshAll()
    } catch (err) {
      wx.showToast({ title: err.message || '添加失败', icon: 'none' })
    } finally {
      this.setData({ saving: false })
    }
  },

  onImportTextInput(e) {
    this.setData({ importText: e.detail.value || '' })
  },

  onImportCategoryChange(e) {
    this.setData({ importCategoryIndex: Number(e.detail.value || 0) })
  },

  async onBatchImport() {
    const text = String(this.data.importText || '').trim()
    if (!text) {
      wx.showToast({ title: '请输入要导入的内容', icon: 'none' })
      return
    }
    if (this.data.saving) return

    this.setData({ saving: true })
    try {
      await ensureLogin()
      const defaultCategory = CATEGORY_OPTIONS[this.data.importCategoryIndex] || '专业模块'
      const result = await request({
        url: '/api/study-analysis/me/records/import',
        method: 'POST',
        data: { text, defaultCategory },
      })
      wx.showToast({ title: `导入 ${result.importedCount || 0} 条`, icon: 'success' })
      this.setData({ importText: '' })
      this.refreshAll()
    } catch (err) {
      wx.showToast({ title: err.message || '导入失败', icon: 'none' })
    } finally {
      this.setData({ saving: false })
    }
  },

  async loadMissingCourses() {
    try {
      await ensureLogin()
      const missing = await request({ url: '/api/study-analysis/me/missing-courses', data: { limit: 30 } })
      this.setData({ missing })
    } catch (err) {
      this.setData({ missing: null })
    }
  },

  async onTapModule(e) {
    const module = e.currentTarget.dataset.module
    if (!module) return
    const next = this.data.activeModule === module ? '' : module
    this.setData({ activeModule: next, moduleDetail: null, activeElectiveKey: '', electiveDetail: null })
    if (!next) return
    try {
      await ensureLogin()
      const moduleDetail = await request({ url: '/api/study-analysis/me/module-detail', data: { module: next, limit: 200 } })
      this.setData({ moduleDetail })
    } catch (err) {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' })
    }
  },

  async onTapElective(e) {
    const key = e.currentTarget.dataset.key
    if (!key) return
    const next = this.data.activeElectiveKey === key ? '' : key
    this.setData({ activeElectiveKey: next, electiveDetail: null })
    if (!next) return
    try {
      await ensureLogin()
      const electiveDetail = await request({ url: '/api/study-analysis/me/module-detail', data: { module: '专业模块', electiveKey: next, limit: 500 } })
      this.setData({ electiveDetail })
    } catch (err) {
      wx.showToast({ title: err.message || '加载失败', icon: 'none' })
    }
  },

  onClearAll() {
    wx.showModal({
      title: '提示',
      content: '确认清空已录入课程吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await ensureLogin()
          await request({ url: '/api/study-analysis/me/records', method: 'DELETE' })
          wx.showToast({ title: '已清空', icon: 'success' })
          this.refreshAll()
        } catch (err) {
          wx.showToast({ title: err.message || '清空失败', icon: 'none' })
        }
      },
    })
  },

  parseCreditsOrNull(text) {
    const s0 = String(text || '').trim()
    if (!s0) return null
    const s = s0.replace('学分', '').trim()
    if (!s) return null
    const n = Number(s)
    if (!Number.isFinite(n) || n <= 0 || n > 50) return null
    return n
  },
})
