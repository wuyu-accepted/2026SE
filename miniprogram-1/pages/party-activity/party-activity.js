const { request } = require('../../utils/request')

Page({
  data: {
    form: {
      title: '',
      reason: '',
      eventDate: '',
    },
    submitting: false,
    loadingList: false,
    items: [],
  },

  onShow() {
    this.loadList()
  },

  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  onReasonInput(e) {
    this.setData({ 'form.reason': e.detail.value })
  },

  onDateChange(e) {
    this.setData({ 'form.eventDate': e.detail.value })
  },

  statusText(status) {
    if (status === 0) return '待审核'
    if (status === 1) return '已通过'
    if (status === 2) return '已驳回'
    return '未知'
  },

  async onSubmit() {
    const title = String(this.data.form.title || '').trim()
    const reason = String(this.data.form.reason || '').trim()
    const eventDate = String(this.data.form.eventDate || '').trim()
    if (!title) {
      wx.showToast({ title: '标题不能为空', icon: 'none' })
      return
    }
    if (!reason) {
      wx.showToast({ title: '事由不能为空', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await request({
        url: '/api/party/me/activities',
        method: 'POST',
        data: {
          title,
          reason,
          eventDate: eventDate || null,
          reviewerId: null,
        },
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      this.setData({ form: { title: '', reason: '', eventDate: '' } })
      this.loadList()
    } catch (error) {
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadList() {
    this.setData({ loadingList: true })
    try {
      const list = await request({ url: '/api/party/me/activities' })
      const mapped = (Array.isArray(list) ? list : []).map((item) => ({
        ...item,
        statusText: this.statusText(item.status),
      }))
      this.setData({ items: mapped })
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loadingList: false })
    }
  },
})

