const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  if (typeof value === 'string') {
    return value.replace('T', ' ').slice(0, 16)
  }
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      return ''
    }
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  } catch (e) {
    return ''
  }
}

function priorityLabel(priority) {
  const map = {
    0: '普通',
    1: '重要',
    2: '紧急',
  }
  return map[priority] || '普通'
}

function normalizeDetail(data) {
  const detail = data || {}
  return {
    ...detail,
    noticeType: detail.noticeType || '通知',
    tag: detail.tag || '',
    priorityText: priorityLabel(detail.priority),
    publishText: formatDateTime(detail.publishTime || detail.createdAt || detail.date),
    readText: formatDateTime(detail.readTime),
  }
}

function parseFallback(value) {
  if (!value) {
    return null
  }

  try {
    return JSON.parse(decodeURIComponent(String(value)))
  } catch (error) {
    console.warn('Parse notice fallback failed:', error)
    return null
  }
}

Page({
  data: {
    messageId: null,
    detail: null,
    loading: false,
  },

  onLoad(options) {
    const messageId = options.id ? decodeURIComponent(String(options.id)) : ''
    if (!messageId) {
      wx.showToast({ title: '通知不存在', icon: 'none' })
      return
    }

    console.info('Notice detail page loaded:', messageId)
    const fallback = parseFallback(options.fallback)
    this.setData({
      messageId,
      detail: fallback ? normalizeDetail(fallback) : null,
    })
    this.loadDetail()
  },

  async loadDetail() {
    const { messageId } = this.data
    this.setData({ loading: true })
    try {
      await ensureLogin()
      console.info('Load notice detail request:', messageId)
      const data = await request({ url: `/api/messages/${messageId}` })
      console.info('Load notice detail success:', messageId)
      this.setData({ detail: normalizeDetail(data) })
    } catch (error) {
      console.error('Load message detail failed:', error)
      wx.showToast({
        title: error.message || '获取通知详情失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },
})
