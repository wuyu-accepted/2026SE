const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

Page({
  data: {
    feedbacks: [],
    loading: false,
  },

  onShow() {
    this.loadPending()
  },

  async loadPending() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const data = await request({ url: '/api/notice-feedback/cadre/pending?pageNum=1&pageSize=50' })
      const feedbacks = (data.records || []).map((item) => ({
        ...item,
        createdText: formatDateTime(item.createdAt),
      }))
      this.setData({ feedbacks })
    } catch (error) {
      console.error('Load pending feedback failed:', error)
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async replyFeedback(event) {
    const { id } = event.currentTarget.dataset
    this.promptAndSubmit(id, 'reply')
  },

  async escalateFeedback(event) {
    const { id } = event.currentTarget.dataset
    this.promptAndSubmit(id, 'escalate')
  },

  promptAndSubmit(id, action) {
    wx.showModal({
      title: action === 'reply' ? '回复并处理' : '上报辅导员',
      editable: true,
      placeholderText: action === 'reply' ? '请输入回复内容' : '请输入上报留言',
      success: async (res) => {
        if (!res.confirm) return
        const content = String(res.content || '').trim()
        if (!content) {
          wx.showToast({ title: '请输入内容', icon: 'none' })
          return
        }
        try {
          await request({
            url: action === 'reply' ? `/api/notice-feedback/${id}/cadre-reply` : `/api/notice-feedback/${id}/escalate`,
            method: 'POST',
            data: { content },
          })
          wx.showToast({ title: action === 'reply' ? '已处理' : '已上报', icon: 'none' })
          this.loadPending()
        } catch (error) {
          wx.showToast({ title: error.message || '操作失败', icon: 'none' })
        }
      },
    })
  },
})
