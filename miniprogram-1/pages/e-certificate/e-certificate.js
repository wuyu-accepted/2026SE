const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    applications: [],
    loading: false,
    formVisible: false,
    form: {
      title: '',
      reason: '',
      templateType: '在校证明',
    },
    submitting: false,
    templateTypes: ['在校证明', '学籍证明', '成绩证明', '毕业证明', '其他'],
  },

  onShow() {
    this.loadList()
  },

  async loadList() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const data = await request({ url: '/api/certificate/me/applications' })
      this.setData({ applications: (data || []).map((item) => ({
        ...item,
        statusText: ['待审批', '审批中', '已通过', '已驳回'][item.status] || '未知',
        submitTime: (item.submitTime || '').replace('T', ' ').slice(0, 16),
      })) })
    } catch (error) {
      console.error('Load certificate list failed:', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  showForm() {
    this.setData({ formVisible: true, 'form.title': '', 'form.reason': '', 'form.templateType': '在校证明' })
  },

  hideForm() {
    this.setData({ formVisible: false })
  },

  onTemplateChange(event) {
    this.setData({ 'form.templateType': this.data.templateTypes[event.detail.value] })
  },

  onFieldInput(event) {
    const { field } = event.currentTarget.dataset
    this.setData({ [`form.${field}`]: event.detail.value })
  },

  async onSubmit() {
    const { title, reason, templateType } = this.data.form
    if (!title.trim()) {
      wx.showToast({ title: '请输入证明标题', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await request({
        url: '/api/certificate/me/applications',
        method: 'POST',
        data: { title: title.trim(), reason: reason.trim(), templateType },
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      this.hideForm()
      this.loadList()
    } catch (error) {
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
