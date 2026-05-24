const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    messages: [
      {
        role: 'assistant',
        content: '你好，我是 AI 助手。你可以问我请假、证明、党团流程、通知、知识库和功能入口。',
        citations: [],
        actions: [],
      },
    ],
    inputValue: '',
    loading: false,
    conversationId: null,
  },

  async onLoad() {
    try {
      await ensureLogin()
    } catch (_) {}
  },

  onInput(event) {
    this.setData({ inputValue: event.detail.value || '' })
  },

  async sendMessage() {
    const content = (this.data.inputValue || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入问题', icon: 'none' })
      return
    }
    const nextMessages = this.data.messages.concat({ role: 'user', content })
    this.setData({ messages: nextMessages, inputValue: '', loading: true })
    try {
      const result = await request({
        url: '/api/ai/chat',
        method: 'POST',
        timeout: 60000,
        data: {
          message: content,
          conversationId: this.data.conversationId,
        },
      })
      this.setData({
        conversationId: result.conversationId || this.data.conversationId,
        messages: nextMessages.concat({
          role: 'assistant',
          content: result.answer || '暂时没有返回内容',
          citations: result.citations || [],
          actions: result.actions || [],
          fallback: !!result.fallback,
        }),
      })
    } catch (error) {
      const timeout = error && /timeout/i.test(error.errMsg || error.message || '')
      this.setData({
        messages: nextMessages.concat({
          role: 'assistant',
          content: timeout ? 'AI 正在检索和生成，当前网络请求超时了。请稍后重试，或换一个更具体的问题。' : (error.message || 'AI 暂不可用'),
          citations: [],
          actions: [],
          fallback: true,
        }),
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  handleActionTap(event) {
    const { path, tabpage } = event.currentTarget.dataset
    if (!path) return
    if (tabpage) {
      wx.switchTab({ url: path })
    } else {
      wx.navigateTo({ url: path })
    }
  },

  handleCitationTap(event) {
    const { id, path } = event.currentTarget.dataset
    if (path) {
      wx.navigateTo({ url: path })
      return
    }
    if (id) {
      wx.navigateTo({ url: `/pages/knowledge-detail/knowledge-detail?id=${id}` })
    }
  },
})
