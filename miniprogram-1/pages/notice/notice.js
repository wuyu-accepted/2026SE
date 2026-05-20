const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

function formatDate(value) {
  if (!value) {
    return ''
  }

  if (typeof value === 'string') {
    const normalized = value.replace('T', ' ')
    return normalized.length >= 10 ? normalized.slice(0, 10) : normalized
  }

  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      return ''
    }
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  } catch (e) {
    return ''
  }
}

Page({
  data: {
    messages: [],
    loading: false,
  },

  onShow() {
    this.loadMessages()
  },

  async loadMessages() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const data = await request({ url: '/api/messages/recent?limit=20' })
      const messages = (data || []).map((item) => ({
        id: item.id,
        title: item.title || '未命名通知',
        summary: item.summary || '',
        readStatus: typeof item.readStatus === 'number' ? item.readStatus : 0,
        date: formatDate(item.createdAt || item.readTime),
      }))
      this.setData({ messages })
    } catch (error) {
      console.error('Load messages failed:', error)
      wx.showToast({
        title: '获取通知失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  handleMessageTap(event) {
    const { index } = event.currentTarget.dataset
    const target = this.data.messages[Number(index)]
    const messageId = target && target.id ? String(target.id) : ''
    if (!messageId) {
      wx.showToast({ title: '通知不存在', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/notice-detail/notice-detail?id=${encodeURIComponent(messageId)}`,
      fail: (error) => {
        console.error('Navigate to message detail failed:', error)
        wx.showToast({ title: '无法打开通知详情', icon: 'none' })
      },
    })
  },
})
