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
        noticeId: item.noticeId,
        title: item.title || '未命名通知',
        summary: item.summary || '',
        readStatus: typeof item.readStatus === 'number' ? item.readStatus : 0,
        pinnedStatus: typeof item.pinnedStatus === 'number' ? item.pinnedStatus : 0,
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
    const fallback = encodeURIComponent(JSON.stringify(target))
    wx.navigateTo({
      url: `/pages/notice-detail/notice-detail?id=${encodeURIComponent(messageId)}&fallback=${fallback}`,
      fail: (error) => {
        console.error('Navigate to message detail failed:', error)
        wx.showToast({ title: '无法打开通知详情', icon: 'none' })
      },
    })
  },

  async handlePinTap(event) {
    const { index } = event.currentTarget.dataset
    const target = this.data.messages[Number(index)]
    const messageId = target && target.id ? String(target.id) : ''
    if (!messageId) {
      wx.showToast({ title: '通知不存在', icon: 'none' })
      return
    }

    const isPinned = target.pinnedStatus === 1
    try {
      await ensureLogin()
      await request({
        url: `/api/messages/${encodeURIComponent(messageId)}/${isPinned ? 'unpin' : 'pin'}`,
        method: 'POST',
      })
      wx.showToast({
        title: isPinned ? '已取消置顶' : '已置顶',
        icon: 'none',
      })
      this.loadMessages()
    } catch (error) {
      console.error('Toggle message pin failed:', error)
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none',
      })
    }
  },
})
